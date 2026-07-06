package com.example.seckill.util;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    //存入数据
    public void set(String key,Object value){
        redisTemplate.opsForValue().set(key,value);
    }

    //存入数据和过期时间，单位：秒
    public void set(String key,Object value,long timeout){
        redisTemplate.opsForValue().set(key,value,timeout, TimeUnit.SECONDS);
    }

    //获取数据
    public Object get(String key){
        return redisTemplate.opsForValue().get(key);
    }

    //删除数据
    public void delete(String key){
        redisTemplate.delete(key);
    }

    //判断数据是否存在
    public boolean exists(String key){
        return redisTemplate.hasKey(key);
    }

    //新增：获取所有匹配的键（生产环境慎用，数据量大时阻塞）
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    //执行Lua脚本
    public Long executeScript(DefaultRedisScript<Long> script, List<String> keys, List<String> args) {
        return redisTemplate.execute(script, keys, args.toArray());
    }

    //新增：执行Lua脚本（带返回值类型）
    public <T> T executeScript(DefaultRedisScript<T> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }
}
