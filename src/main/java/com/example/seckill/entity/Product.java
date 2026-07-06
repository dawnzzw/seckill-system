package com.example.seckill.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
