package com.example.seckill.service;

import com.example.seckill.entity.Order;
import com.example.seckill.entity.OrderMessage;
import com.example.seckill.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    @Autowired
    private OrderMapper orderMapper;

    @KafkaListener(topics="order-topic",groupId="seckill-group")
    public void handleOrderMessage(OrderMessage orderMessage){
        try{
            System.out.println("收到订单消息："+orderMessage);

            //1.将消息体转换成Order实体
            Order order=new Order();
            order.setOrderNo(orderMessage.getOrderNo());
            order.setUserId(orderMessage.getUserId());
            order.setProductId(orderMessage.getProductId());
            order.setQuantity(orderMessage.getQuantity());
            order.setTotalPrice(orderMessage.getTotalPrice());
            order.setStatus(orderMessage.getStatus());

            //2.插入数据库
            orderMapper.insert(order);
            System.out.println("订单消息消费成功，订单ID："+order.getId());
        }catch(Exception e){
            System.err.println("订单消息消费失败，订单消息："+e.getMessage());
        }
    }
}
