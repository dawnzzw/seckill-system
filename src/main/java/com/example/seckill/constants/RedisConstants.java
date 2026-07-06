package com.example.seckill.constants;

public class RedisConstants {
    //商品缓存 key 前缀
    public static final String PRODUCT_KEY_PREFIX = "product:";

     //商品库存缓存 key 前缀
    public static final String STOCK_KEY_PREFIX = "product:stock:";

    //缓存默认过期时间（秒）：1小时
    public static final long CACHE_EXPIRE_TIME = 3600;

    //订单缓存 key 前缀
    public static final String ORDER_KEY_PREFIX = "order:";

    //用户缓存 key 前缀
    public static final String USER_KEY_PREFIX = "user:";

    //商品锁key前缀
    public static final String LOCK_KEY_PREFIX = "lock:product:";

    //kafka主题
    public static final String ORDER_TOPIC = "order-topic";

    //秒杀状态
    public static final String SECKILL_STATUS="seckill:status:";

}
