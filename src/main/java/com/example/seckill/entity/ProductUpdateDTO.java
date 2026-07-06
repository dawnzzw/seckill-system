package com.example.seckill.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductUpdateDTO {
    private Long productId;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String description;
}
