package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopProxy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;


    static {
        SECKILL_SCRIPT=new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks=new ArrayBlockingQueue<>(1024*1024);

    private static final ExecutorService SECKILL_ORDER_EXECUTOR= Executors.newSingleThreadExecutor();


    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{

        @Override
        public void run() {
            try {
                //获取队列中订单信�?
                VoucherOrder order = orderTasks.take();
                //创建订单
                handleVoucherOrder(order);
            } catch (Exception e) {
                log.error("处理订单异常:",e);
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean islock = lock.tryLock();
        if (!islock) {
           log.error("不允许重复下单");
           return;
        }

        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }

    private IVoucherOrderService proxy;

    //新业务逻辑
    @Override
    public Result secKillVoucher(Long voucherId) {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        //执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        int r = result.intValue();
        //判断result的类�?
        if(r !=0){
            return Result.fail(r==1?"库存不足":"无法重复购买");
        }

        //TODO:保存到阻塞队�?
        //先封装订单信�?
        VoucherOrder voucherOrder=new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);

        //放入阻塞队列:若队列里没有元素，则尝试获取元素的线程会休眠，直到有元素之后被唤�?
        orderTasks.add(voucherOrder);
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        return Result.ok(orderId);
    }

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        //实现一人一�?
        Long userId1 = voucherOrder.getUserId();
        Long count = query().eq("user_id", userId1).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if(count>0){
            log.error("用户已经购买过一单");
            return;
        }


        boolean success = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                //TODO: 乐观锁解决超卖问�?
                .gt("stock",0)
                .update();
        if(!success){
            log.error("库存不足");
            return;
        }
        save(voucherOrder);
    }



    //旧业务逻辑

//    @Override
//    public Result secKillVoucher(Long voucherId) {
//        //查询
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//
//        //判断是否开�?
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())&&
//                voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀未开�?已结�?);
//        }
//        if(voucher.getStock()<1){
//            return Result.fail("库存不足");
//        }
//        Long userId1 = UserHolder.getUser().getId();
//        //获取�?
//        //自己实现的分布式�?
////        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId1, stringRedisTemplate);
//        //调用Redisson的分布式�?
//        RLock lock = redissonClient.getLock("lock:order:" + userId1);
//        boolean islock = lock.tryLock();
//        if (!islock) {
//            return Result.fail("不允许重复下�?);
//        }
//
//        try {
//            //获取spring的代理对象，不然会事务失�?
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return createVoucherOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }
//
//    }

//    @Transactional
//    public synchronized Result createVoucherOrder(Long voucherId) {
//        //实现一人一�?
//        Long userId1 = UserHolder.getUser().getId();
//        int count = query().eq("user_id", userId1).eq("voucher_id", voucherId).count();
//        if(count>0){
//            return Result.fail("用户已经购买过一�?);
//        }
//
//
//        boolean success = seckillVoucherService.update()
//                .setSql("stock=stock-1")
//                .eq("voucher_id", voucherId)
//                //TODO: 乐观锁解决超卖问�?
//                .gt("stock",0)
//                .update();
//        if(!success){
//            return Result.fail("库存不足");
//        }
//        VoucherOrder voucherOrder=new VoucherOrder();
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        Long userId = UserHolder.getUser().getId();
//        voucherOrder.setUserId(userId);
//        voucherOrder.setVoucherId(voucherId);
//
//        save(voucherOrder);
//        return Result.ok(orderId);
//    }
}
