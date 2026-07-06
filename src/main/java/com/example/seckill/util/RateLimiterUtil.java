package com.example.seckill.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RateLimiterUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 固定时间窗口限流
     * @param key 限流key（如：rate:user:1）
     * @param maxRequests 时间窗口内最大请求数
     * @param windowSeconds 时间窗口 (秒)
     * @return true：限流通过；false：被限流
     */
    public boolean allowRequest(String key, int maxRequests, int windowSeconds) {
        //Redis计数器+1.
        Long count = redisTemplate.opsForValue().increment(key, 1);
        if(count == 1){
            //第一次请求，设置过期时间
            redisTemplate.expire(key,windowSeconds, TimeUnit.SECONDS);
        }
        return count <= maxRequests;
    }
}
