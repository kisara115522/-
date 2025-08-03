package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class RedisIdWorker {
    public static final long BEGIN_TIMESTAMP=1672531200;
    //序列号的位数
    public static final int COUNGT_BITS=32;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix){

        //1.时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;
        //2.序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr" + keyPrefix + ":" + date);
        //3.拼接
        return timeStamp<<COUNGT_BITS|count;
    }


//    public static void main(String[] args) {
//        LocalDateTime localDateTime = LocalDateTime.of(2023, 1, 1, 0, 0);
//        long epochSecond = localDateTime.toEpochSecond(ZoneOffset.UTC);
//        System.out.println(epochSecond);
//    }
}
