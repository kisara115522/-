package com.hmdp;

import cn.hutool.core.util.IdUtil;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private RedisIdWorker redisIdWorker;


    private ExecutorService es= Executors.newFixedThreadPool(1000);

    //全局id生成测试
    @Test
    void testWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1000);
        Runnable task=()-> {
            for (int i = 0; i < 100; i++) {
                long id =redisIdWorker.nextId("order");
                System.out.println("id="+id);
            }
            latch.countDown();
        };
        long begin =System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time="+(end-begin));

    }
}
