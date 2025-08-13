package com.hmdp.utils;

import cn.hutool.core.lang.func.Func;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.nio.channels.Pipe;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Component
@Slf4j
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }
    public void setWithLogicExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type,
                                         Function<ID,R> dbFallBack, Long time, TimeUnit unit){
        //1. 根据id查redis
        String key=keyPrefix+id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否命中
        if (StrUtil.isNotBlank(json)) {
            //3.存在直接返回
            return JSONUtil.toBean(json,type);
        }

        //先判断是否是空字符串，是的话代表是缓存穿�?
        //null “�?\t\n返回false  所以判断不为null就是“�?
        if (json != null) {
            return null;
        }
        //4.不存在根据id查数据库
        R r=dbFallBack.apply(id);

        if (r == null) {
            //5.不存在返回错�?
            //用存空值解决缓存穿�?
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,
                    "",CACHE_NULL_TTL, TimeUnit.MINUTES);

            return null;
        }
        //6。存在，写入redis
        this.set(key,r,time,unit);
        //7。返�?
        return r;
    }
}
