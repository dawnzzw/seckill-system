package com.example.seckill.controller;

import com.example.seckill.entity.LoginRequest;
import com.example.seckill.entity.Merchant;
import com.example.seckill.entity.RegisterRequest;
import com.example.seckill.entity.User;
import com.example.seckill.exception.Result;
import com.example.seckill.exception.ResultCode;
import com.example.seckill.service.MerchantService;
import com.example.seckill.service.UserService;
import com.example.seckill.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MerchantService merchantService;
    /**
     * 注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Result<Map<String, String>> register(@RequestBody RegisterRequest request) {
        try {
            String message;
            String role =request.getRole();
            if("MERCHANT".equals(role)){
                message = merchantService.register(
                        request.getUsername(),
                        request.getPassword(),
                        request.getShopName(),
                        request.getPhone()
                );
            }else{
                message = userService.register(
                        request.getUsername(),
                        request.getPassword(),
                        request.getPhone()
                );
            }
            Map<String, String> data = new HashMap<>();
            data.put("message", message);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, e.getMessage());
        }
    }

    /**
     * 登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            String token;
            String role =request.getRole();
            if("MERCHANT".equals(role)){
                token = merchantService.login(
                        request.getUsername(),
                        request.getPassword()
                );
                Merchant merchant = merchantService.findByUsername(request.getUsername());

                Map<String,Object> data = new HashMap<>();
                data.put("token", token);
                data.put("merchantId", merchant.getId());
                data.put("username", merchant.getUsername());
                data.put("role","MERCHANT");
                data.put("shopName", merchant.getShopName());
                return Result.success(data);
            }else {
                // 1. 验证登录，获取 Token
                token = userService.login(request.getUsername(), request.getPassword());

                // 2. 查询用户信息
                User user = userService.findByUsername(request.getUsername());

                // 3. 返回 Token 和用户信息
                Map<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("userId", user.getId());
                data.put("username", user.getUsername());
                data.put("role", "USER");
                return Result.success(data);
            }

        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, e.getMessage());
        }
    }

    /**
     * 验证 Token 是否有效
     * GET /api/auth/verify
     */
    @GetMapping("/verify")
    public Result<Map<String, Object>> verify(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader;
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (!jwtUtil.validateToken(token)) {
                return Result.error(ResultCode.UNAUTHORIZED, "Token 无效或已过期");
            }

            Long userId = jwtUtil.getUserId(token);
            String role = jwtUtil.getRole(token);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("role", role);
            data.put("valid", true);

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(ResultCode.UNAUTHORIZED, "Token 无效");
        }
    }
}