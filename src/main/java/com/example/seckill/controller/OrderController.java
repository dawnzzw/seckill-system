package com.example.seckill.controller;

import com.example.seckill.annotation.RateLimiter;
import com.example.seckill.entity.Order;
import com.example.seckill.exception.Result;
import com.example.seckill.exception.ResultCode;
import com.example.seckill.mapper.OrderMapper;
import com.example.seckill.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 创建订单(三级限流）
     */
    @RateLimiter({
            @RateLimiter.Config(type = RateLimiter.Type.IP, maxRequests = 10, windowSeconds = 1),
            @RateLimiter.Config(type = RateLimiter.Type.USER, maxRequests = 1, windowSeconds = 5),
            @RateLimiter.Config(type = RateLimiter.Type.PRODUCT, maxRequests = 100, windowSeconds = 1)
    })
    @PostMapping("/create")
    public Result<String> createOrder(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            String result = orderService.createOrder(userId, productId, quantity);

            // 判断是否成功
            if (result.contains("抢购成功") || result.contains("🎉")) {
                return Result.success(result);
            }

            // 根据不同错误返回不同状态码
            if (result.contains("库存不足")) {
                return Result.error(ResultCode.STOCK_INSUFFICIENT);
            }
            if (result.contains("秒杀已结束")) {
                return Result.error(ResultCode.SECKILL_ENDED);
            }
            if (result.contains("秒杀尚未开始")) {
                return Result.error(ResultCode.SECKILL_NOT_START);
            }
            if (result.contains("操作过于频繁")) {
                return Result.error(ResultCode.RATE_LIMIT);
            }
            if (result.contains("商品不存在")) {
                return Result.error(ResultCode.PRODUCT_NOT_EXIST);
            }

            // 兜底
            return Result.error(ResultCode.ERROR, result);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "系统繁忙，请稍后再试");
        }
    }

    /**
     * 支付订单
     */
    @PostMapping("/pay")
    public Result<String> pay(@RequestParam String orderNo) {
        try {
            orderService.payOrder(orderNo);
            return Result.success("支付成功，订单号：" + orderNo);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("订单不存在")) {
                return Result.error(ResultCode.ORDER_NOT_EXIST);
            }
            if (msg != null && msg.contains("状态不允许支付")) {
                return Result.error(ResultCode.ORDER_STATUS_ERROR);
            }
            return Result.error(ResultCode.ERROR, "支付失败：" + msg);
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam String orderNo) {
        try {
            orderService.cancelOrder(orderNo);
            return Result.success("取消成功，订单号：" + orderNo);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("订单不存在")) {
                return Result.error(ResultCode.ORDER_NOT_EXIST);
            }
            if (msg != null && msg.contains("状态不允许取消")) {
                return Result.error(ResultCode.ORDER_STATUS_ERROR);
            }
            return Result.error(ResultCode.ERROR, "取消失败：" + msg);
        }
    }

    /**
     * 根据订单编号查询订单
     */
    @GetMapping("/detail")
    public Result<Order> getOrderByOrderNo(@RequestParam String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            return Result.error(ResultCode.ORDER_NOT_EXIST);
        }
        return Result.success(order);
    }

    /**
     * 查询用户订单列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> listOrders(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Map<String, Object> data = orderService.getUserOrders(userId, status, startTime, endTime, page, pageSize);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "查询订单列表失败：" + e.getMessage());
        }
    }

    /**
     * 批量更新订单状态
     */
    @PostMapping("/batchUpdateStatus")
    public Result<String> batchUpdateStatus(
            @RequestParam(required = false) List<String> orderNos,
            @RequestParam Integer status) {
        try {
            String result = orderService.batchUpdateStatus(orderNos, status);
            if (result.contains("成功")) {
                return Result.success(result);
            }
            return Result.error(result);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "批量更新失败：" + e.getMessage());
        }
    }

    /**
     * 查询今日订单数
     */
    @GetMapping("/today/count")
    public Result<Map<String, Object>> getTodayOrderCount() {
        Map<String, Object> result = new HashMap<>();
        int count = orderMapper.countTodayOrders();
        result.put("count", count);
        return Result.success(result);
    }

    /**
     * 查询订单处理结果（轮询接口）
     * GET /api/order/result?orderNo=xxx
     */
    @GetMapping("/result")
    public Result<Map<String, Object>> getOrderResult(@RequestParam String orderNo) {
        Map<String, Object> result = new HashMap<>();
        Order order = orderMapper.findByOrderNo(orderNo);

        if (order == null) {
            // 订单还没生成（Kafka还没消费完）
            result.put("status", "PROCESSING");
            result.put("message", "订单处理中，请稍候...");
            return Result.success(result);
        }

        // 订单已生成
        result.put("status", "SUCCESS");
        result.put("orderNo", order.getOrderNo());
        result.put("orderStatus", order.getStatus());
        result.put("message", "🎉 抢购成功！");
        return Result.success(result);
    }
}