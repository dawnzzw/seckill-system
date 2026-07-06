package com.example.seckill.mapper;

import com.example.seckill.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OrderMapper {

    // 插入订单
    int insert(Order order);

    // 根据ID查询订单
    Order findById(@Param("id") Long id);

    // 查询今日订单数
    int countTodayOrders();

    // 修改订单状态
    int update(Order order);

    // 根据订单编号查询订单
    Order findByOrderNo(@Param("orderNo") String orderNo);

    //根据用户ID查询订单列表（支持状态筛选）
    List<Order> findOrdersByUser(@Param("userId") Long userId,
                                 @Param("status") Integer status,
                                 @Param("startTime") String startTime,
                                 @Param("endTime") String endTime,
                                 @Param("offset") Integer offset,
                                 @Param("pageSize") Integer pageSize);

    //统计用户订单数量（支持状态筛选）
    int countOrdersByUser(@Param("userId") Long userId,
                          @Param("status") Integer status,
                          @Param("startTime") String startTime,
                          @Param("endTime") String endTime);

    // 查询超时未支付的订单
    List<Order> findTimeoutOrders();


    // 批量更新订单状态
    int batchUpdateStatus(@Param("orderNos") List<String> list,
                          @Param ("status") Integer status);

    //按时间范围统计订单总数
    Integer countByTime(@Param("startTime") String startTime,
                        @Param("endTime") String endTime);

    //按状态+时间范围统计订单总数
    Integer countByStatusAndTime(@Param("status") Integer status,
                                 @Param("startTime") String startTime,
                                 @Param("endTime") String endTime);

    //按时间范围查询所有订单
    List<Order> findAllByTime(@Param("startTime") String startTime, @Param("endTime") String endTime);
}