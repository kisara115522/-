package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;


    public Result queryById(Long id) {
//        缓存穿透的解决
        //lamda表达式可以简写为this::getById；
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,
                id2->getById(id2),CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }

        return Result.ok(shop);
    }

    //互斥锁实现
    public Shop queryWithMutex(Long id){
        //1. 根据id查redis
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2.判断是否命中
        if (StrUtil.isNotBlank(shopJson)) {
            //3.存在直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        //先判断是否是空字符串，是的话代表是缓存击穿
        //null “” \t\n返回false  所以判断不为null就是“”
        if (shopJson != null) {
            return null;
        }
        //4.实现缓存重建
        //4.1 获取互斥锁
        String lockKey= null;
        Shop shopValue = null;
        try {
            lockKey = LOCK_SHOP_KEY+id;
            Boolean isLock = tryLock(lockKey);

            //4.2判断是否获取成功
            if (!isLock) {
                //4.3 失败则休眠重试
                Thread.sleep(50);
                queryWithMutex(id);
            }

            //4.4 成功 ，就根据id查询数据库
            shopValue = getById(id);
            //TODO：模拟重建延迟
            Thread.sleep(200);
            if (shopValue == null) {
                //5.不存在返回错误
                //用存空值解决缓存击穿
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,
                        "",CACHE_NULL_TTL, TimeUnit.MINUTES);

                return null;
            }
            //6。存在，写入redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,
                    JSONUtil.toJsonPrettyStr(shopValue),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            //7.释放互斥锁
            unLock(lockKey);
        }
        //8.返回
        return shopValue;
    }

    //缓存穿透的实现
    public Shop queryWithPassThrough(Long id){
        //1. 根据id查redis
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2.判断是否命中
        if (StrUtil.isNotBlank(shopJson)) {
            //3.存在直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        //先判断是否是空字符串，是的话代表是缓存击穿
        //null “” \t\n返回false  所以判断不为null就是“”
        if (shopJson != null) {
            return null;
        }
        //4.不存在根据id查数据库
        Shop shopValue = getById(id);

        if (shopValue == null) {
            //5.不存在返回错误
            //用存空值解决缓存击穿
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,
                    "",CACHE_NULL_TTL, TimeUnit.MINUTES);

            return null;
        }
        //6。存在，写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,
                JSONUtil.toJsonPrettyStr(shopValue),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //7。返回
        return shopValue;
    }


    //用redis的String setnx命令模拟互斥锁，此命令只会在key不存在的时候执行，存在即不执行
    private Boolean tryLock(String key){
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10L, TimeUnit.SECONDS);
        //因为如果直接返回对象java会拆箱可能会出现null
        return BooleanUtil.isTrue(aBoolean);
    }

    //释放锁就是把这个key从redis里删除，这样其他线程就可以获取到这个锁
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }

        updateById(shop);
        //删除缓存

        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }
}
