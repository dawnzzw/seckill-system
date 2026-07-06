package com.example.seckill.service;

import com.example.seckill.entity.Order;
import com.example.seckill.mapper.OrderMapper;
import com.example.seckill.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.seckill.constants.RedisConstants.STOCK_KEY_PREFIX;

@Service
public class OrderTimeoutService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 定时检查订单超时,2分钟1次
     */
    @Scheduled(cron = "0 0/2 * * * ?")
    public void cancelTimeoutOrders() {
        List<Order> timeoutOrders = orderMapper.findTimeoutOrders();

        if(timeoutOrders.isEmpty()){
            return;
        }

        System.out.println("开始取消超时订单：" + timeoutOrders.size());

        for (Order order : timeoutOrders) {
            String orderNo = order.getOrderNo();
            try{
                //先回滚库存，再取消订单
                boolean rollbackSuccess = rollbackStock(order.getProductId(), order.getQuantity());
                if(!rollbackSuccess){
                    //回滚库存失败,记录告警，跳过该订单（不取消，等待下次重试）
                    System.out.println("回滚库存失败，暂不取消订单：" + orderNo);
                    continue;
                }
                orderService.cancelOrder(orderNo);
                System.out.println("超时订单已取消，库存已回滚：" + orderNo);
            }catch(Exception e){
                System.out.println("取消超时订单异常：" + orderNo + "，"+ e.getMessage());
            }
        }
    }

    /**
     * 回滚Redis库存
     * @return true=回滚成功，false=回滚失败
     */
    private boolean rollbackStock(Long productId, Integer quantity) {
        try {
            String stockKey = STOCK_KEY_PREFIX+ productId;
            Integer currentStock = (Integer) redisUtil.get(stockKey);

            if (currentStock == null) {
                // Redis中没有库存缓存，记录告警，但不阻塞
                System.err.println("Redis库存缓存不存在，跳过回滚。商品ID：" + productId);
                return true;  // 返回true，让订单继续取消（因为库存数据本身有问题，留着订单也没意义）
            }

            // 回滚库存
            redisUtil.set(stockKey, currentStock + quantity);
            System.out.println("库存回滚成功，商品ID：" + productId + "，回滚数量：" + quantity + "，当前库存：" + (currentStock + quantity));
            return true;

        } catch (Exception e) {
            System.err.println("库存回滚异常：" + e.getMessage());
            return false;
        }
    }
}
