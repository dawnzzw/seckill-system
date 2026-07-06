package com.example.seckill.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {
    private Long id;  //订单ID
    private String orderNo; //订单编号
    private Long userId;  //用户ID
    private Long productId;  //商品ID
    private Integer quantity;  //购买数量
    private BigDecimal totalPrice;  //订单总价
    private Integer status;  //0-待支付,1-已支付,2-已取消
    private LocalDateTime createTime;  //创建时间
    private LocalDateTime  payTime; //支付时间
}
