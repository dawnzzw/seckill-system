package com.example.seckill.aspect;


import com.example.seckill.annotation.RateLimiter;
import com.example.seckill.util.RateLimiterUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class RateLimitAspect {
    @Autowired
    private RateLimiterUtil rateLimiterUtil;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate:";

    @Around("@annotation(com.example.seckill.annotation.RateLimiter)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        //1.获取方法上的注解
        //获取方法“签名”，包括方法名字，参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //从签名中获取真正的Method对象
        Method method = signature.getMethod();
        //查看这个方法中是否有@RateLimiter标签，找到了就存下来，没有就返回null
        RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);

        //从注解中读取type值
        RateLimiter.Config[] configs = rateLimiter.value();
        // 如果配置为空，直接放行
        if (configs == null || configs.length == 0) {
            return joinPoint.proceed();
        }

        // 逐个检查每个限流配置
        for (RateLimiter.Config config : configs) {
            RateLimiter.Type type = config.type();
            int maxRequests = config.maxRequests();
            int windowSeconds = config.windowSeconds();

            // 如果是 NONE，跳过
            if (type == RateLimiter.Type.NONE) {
                continue;
            }

            // 构建限流 Key
            String key = buildKey(type, joinPoint);
            if (key == null) {
                continue;
            }

            // 执行限流检查
            if (!rateLimiterUtil.allowRequest(key, maxRequests, windowSeconds)) {
                String message = getLimitMessage(type, windowSeconds);
                throw new RuntimeException(message);
            }
        }

        // 全部检查通过，放行
        return joinPoint.proceed();
    }

    /**
     * 根据限流类型构建 Redis Key
     */
    private String buildKey(RateLimiter.Type type, ProceedingJoinPoint joinPoint) {

        HttpServletRequest request = getRequest();

        switch (type) {
            case IP:
                String ip = getClientIp(request);
                return RATE_LIMIT_KEY_PREFIX + "ip:" + ip;

            case USER:
                Long userId = getUserId(joinPoint);
                if (userId == null) {
                    return null;  // 没有userId，不进行限流
                }
                return RATE_LIMIT_KEY_PREFIX + "user:" + userId;

            case PRODUCT:
                Long productId = getProductId(joinPoint);
                if (productId == null) {
                    return null;
                }
                return RATE_LIMIT_KEY_PREFIX + "product:" + productId;

            case NONE:
            default:
                return null;
        }
    }

    /**
     * 获取限流提示信息
     */
    private String getLimitMessage(RateLimiter.Type type, int windowSeconds) {
        switch (type) {
            case IP:
                return "IP访问过于频繁，请 " + windowSeconds + " 秒后再试";
            case USER:
                return "操作过于频繁，请 " + windowSeconds + " 秒后再试";
            case PRODUCT:
                return "当前商品下单人数过多，请稍后再试";
            default:
                return "请求过于频繁，请稍后再试";
        }
    }

    /**
     * 获取 HttpServletRequest
     */
    private HttpServletRequest getRequest() {

        //从Spring的“工具箱”里获取请求的上下文
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        //从上下文中取出真正的请求对象
        return attributes.getRequest();
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        //Nginx代理
        String ip = request.getHeader("X-Forwarded-For");

        //Apache代理
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        //WebLogic服务器
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        //获取直接连接服务器的客户端IP
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多级代理，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 从方法参数中提取 userId
     */
    private Long getUserId(ProceedingJoinPoint joinPoint) {

        //获取所有参数，参数存储在数组里
        Object[] args = joinPoint.getArgs();
        // 按顺序：userId, productId, quantity
        //第一个参数是Long类型的
        if (args.length > 0 && args[0] instanceof Long) {

            //将Object
            return (Long) args[0];
        }
        return null;
    }

    /**
     * 从方法参数中提取 productId
     */
    private Long getProductId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        // 按顺序：userId, productId, quantity
        //第二个参数是Long类型的
        if (args.length > 1 && args[1] instanceof Long) {
            return (Long) args[1];
        }
        return null;
    }
}
