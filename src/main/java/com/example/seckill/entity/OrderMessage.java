package com.example.seckill.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMessage implements Serializable {

    private String orderNo;
    private Long userId; //用户ID
    private Long productId; //商品ID
    private Integer quantity; //商品数量
    private BigDecimal totalPrice; //商品总价
    private Integer status; //订单状态


    /**
     * 从 Order 对象转换成OrderMessage
     */
    public static OrderMessage fromOrder(Order order) {
        return new OrderMessage(
                order.getOrderNo(),
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus()
        );
    }
}
