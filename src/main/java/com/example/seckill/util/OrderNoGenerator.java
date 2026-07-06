package  com.example.seckill.util;

import com.example.seckill.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Component
public class OrderNoGenerator {

    private static final String BIZ_TYPE_NORMAL = "01";
    private static final String BIZ_TYPE_SECKILL = "02";

    @Autowired
    private RedisUtil redisUtil;  // 你项目里已有的Redis工具类

    /**
     * 生成订单号
     * @param bizType 业务类型：normal / seckill
     * @return 20位订单号
     */
    public String generateOrderNo(String bizType) {
        // 1. 时间部分：yyyyMMddHH（10位）
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));

        // 2. 业务码
        String bizCode = "normal".equals(bizType) ? BIZ_TYPE_NORMAL : BIZ_TYPE_SECKILL;

        // 3. Redis key：order:seq:202606251201
        String redisKey = "order:seq:" + timePart + bizCode;

        // 4. 获取序列号（原子自增 + 自动过期）
        Long seq = getSequenceFromRedis(redisKey, 3600L);  // 1小时过期

        // 5. 组装订单号（序列号补零到8位）
        return timePart + bizCode + String.format("%08d", seq);
    }

    /**
     * 从Redis获取自增序列号
     * @param key Redis key
     * @param expireSeconds 过期时间（秒）
     * @return 自增后的序列号（从1开始）
     */
    private Long getSequenceFromRedis(String key, Long expireSeconds) {
        // 用Lua脚本保证"判断是否存在 + 自增 + 设置过期"的原子性
        String luaScript =
                "if redis.call('EXISTS', KEYS[1]) == 0 then " +
                        "    redis.call('SET', KEYS[1], 0) " +
                        "    redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
                        "end " +
                        "return redis.call('INCR', KEYS[1])";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);

        return redisUtil.executeScript(script, Arrays.asList(key), Arrays.asList(expireSeconds.toString()));
    }

    /**
     * 每天凌晨清理过期的序列号缓存
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanCache() {
        // Redis中的key会自动过期，不需要手动清理
        System.out.println("✅ 订单号序列号缓存自动清理完成");
    }
}