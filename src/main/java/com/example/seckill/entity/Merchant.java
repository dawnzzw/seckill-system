package com.example.seckill.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Merchant {
    private Long id;
    private String username;
    private String password;
    private String shopName;
    private String phone;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
