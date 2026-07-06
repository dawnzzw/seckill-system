package com.example.seckill.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")                    // 拦截所有 /api 请求
                .excludePathPatterns(
                        "/api/auth/**",                       // 登录注册放行
                        "/api/admin/preheat",                 // 预热临时放行（后续可加权限）
                        "/api/admin/preheatBatch"
                );
    }
}