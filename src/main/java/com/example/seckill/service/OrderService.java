package com.example.seckill.service;

import com.example.seckill.entity.Order;
import com.example.seckill.entity.OrderMessage;
import com.example.seckill.entity.Product;
import com.example.seckill.mapper.OrderMapper;
import com.example.seckill.util.OrderNoGenerator;
import com.example.seckill.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.seckill.constants.RedisConstants.*;

@Service
public class OrderService {

    @Autowired
    private ProductService productService;

    @Autowired
    private StockService stockService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OrderNoGenerator orderNoGenerator;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 创建订单（扣库存 + 生成订单）
     * 流程：检查秒杀状态→扣库存→生成订单→发送Kafka消息
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantity 购买数量
     * @return 订单号
     */
    public String createOrder(Long userId, Long productId,Integer quantity) {

        System.out.println("收到下单请求：userId=" + userId + ", productId=" + productId + ", quantity=" + quantity);
        //检查秒杀状态
        String statusKey = SECKILL_STATUS + productId;
        Integer status = (Integer) redisUtil.get(statusKey);
        if (status == null || status == 0) {
            System.out.println("秒杀尚未开始：userId=" + userId);
            return "秒杀尚未开始，请稍后再来~";
        }
        if (status == 2) {
            System.out.println("秒杀已结束：userId=" + userId);
            return "秒杀已结束，下次早点来哦~";
        }

        //1.查询商品信息
        Product product = productService.getProductById(productId);
        if(product == null){
            System.out.println("商品不存在：productId=" + productId);
            return "商品不存在，请刷新重试";
        }

        //2.扣减库存（委托给StockService）
        Integer newStock = stockService.decreaseStock(productId, quantity);
        if (newStock <= -1){
            System.out.println("库存不足：productId=" + productId);
            return "库存不足，手慢了一步~";
        }

        System.out.println("库存扣减成功：productId=" + productId + ", 剩余库存=" + newStock);

        // 生成订单号
        String orderNo = orderNoGenerator.generateOrderNo("normal");
        System.out.println("订单号生成成功：orderNo=" + orderNo);
        //3.生成订单（先不插入数据库）
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(0);

        //4.发送消息到kafka（异步）
        OrderMessage orderMessage = OrderMessage.fromOrder(order);
        kafkaTemplate.send(ORDER_TOPIC,orderMessage);
        System.out.println("订单消息已发送到Kafka，用户ID："+userId+"，商品ID："+productId);

        //5.立即返回（不等待插入数据库）
        System.out.println("抢购成功：userId=" + userId + ", orderNo=" + orderNo);
        return "抢购成功！订单号：" + orderNo + "，请在30分钟内完成支付~";

    }

    /**
     * 支付订单
     */
    public void payOrder(String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if(order == null){
            throw new RuntimeException("订单不存在");
        }
        //订单状态检查 0-待支付 1-已支付 2-已取消
        if(order.getStatus() != 0){
            throw new RuntimeException("该订单状态不允许支付（当前状态："+order.getStatus()+")");
        }
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.update(order);
        System.out.println("订单已支付，订单ID："+order.getId());
    }

    /**
     * 取消订单
     */
    public void cancelOrder(String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("该订单状态不允许取消（当前状态：" + order.getStatus() + "）");
        }

        order.setStatus(2);
        orderMapper.update(order);
        System.out.println("订单已取消：" + orderNo);
    }

    /**
     * 查询用户订单列表（分页）
     * @param userId 用户ID
     * @param status 订单状态(0-待支付 1-已支付 2-已取消)
     * @param page 页码(从1开始)
     * @param pageSize 每页数量
     */
    public Map<String, Object> getUserOrders(Long userId, Integer status, String startTime, String endTime,Integer page, Integer pageSize){
        //默认值
        if(page == null || page < 1) page=1;
        if(pageSize == null || pageSize < 1) pageSize=10;

        int offset = (page-1)*pageSize;

        //查询数据
        List<Order> list = orderMapper.findOrdersByUser(userId,status,startTime, endTime, offset,pageSize);
        int total = orderMapper.countOrdersByUser(userId,status,startTime, endTime);

        //封装返回结果
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("list",list);
        result.put("total",total);
        result.put("page",page);
        result.put("pageSize",pageSize);
        result.put("totalPage",(total+pageSize-1)/pageSize);

        return result;
    }

    /**
     * 批量修改订单状态
     * @param OrderNos 订单号列表
     * @param status 目标状态
     * @return 更新结果
     */
    public String batchUpdateStatus(List<String> OrderNos, Integer status) {
        if (OrderNos == null || OrderNos.isEmpty()) {
            return "请选择要修改的订单";
        }
        if (status == null || status < 0 || status > 2) {
            return "无效的订单状态";
        }

        //批量修改订单状态
        int count = orderMapper.batchUpdateStatus(OrderNos, status);
        return "成功更新"+count+"条订单状态，订单状态修改为："+getStatusName(status);
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer status) {
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已取消";
            default: return "未知";
        }
    }

}
