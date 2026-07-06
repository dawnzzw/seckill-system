package com.example.seckill.config;


import com.example.seckill.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //OPTIONS请求放行（跨域预检）
        if ("OPTIONS".equals(request.getMethod())){
            return true;
        }

        //从请求头中取出Token
        String token=request.getHeader("Authorization");
        if(token==null|| token.isEmpty()){
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\"}");
            return false;
        }

        //去除Bearer前缀（如果有）
        if(token.startsWith("Bearer")){
            token=token.substring(7);
        }

        //验证Token
        if(!jwtUtil.validateToken(token)){
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"Token已过期，请重新登录\"}");
            return false;
        }

        //将用户信息存入请求属性，供后续使用
        Long userId=jwtUtil.getUserId(token);
        String role= jwtUtil.getRole( token);
        request.setAttribute("userId",userId);
        request.setAttribute("role",role);
        return true;
    }
}
