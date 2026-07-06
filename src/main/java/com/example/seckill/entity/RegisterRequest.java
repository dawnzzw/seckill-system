package com.example.seckill.entity;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String phone;
    private String role;
    private String shopName;
}
