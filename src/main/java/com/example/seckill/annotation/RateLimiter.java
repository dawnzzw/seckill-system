package com.example.seckill.annotation;


import java.lang.annotation.*;
import java.lang.reflect.Type;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    /**
     * 限流配置列表
     */
    Config[] value() default {};

    @interface Config {
        /** 限流类型 */
        Type type() default Type.USER;

        /** 最大请求数 */
        int maxRequests() default 1;

        /** 时间窗口（秒） */
        int windowSeconds() default 5;
    }

    //限流类型枚举
    enum Type {
        USER, // 用户限流
        IP,  // IP限流
        PRODUCT, // 商品限流
        NONE // 不限流（只记录不拦截）
    }
}
