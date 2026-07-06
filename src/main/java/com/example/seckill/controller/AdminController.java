package com.example.seckill.controller;


import com.example.seckill.constants.RedisConstants;
import com.example.seckill.entity.Order;
import com.example.seckill.entity.Product;
import com.example.seckill.exception.Result;
import com.example.seckill.mapper.OrderMapper;
import com.example.seckill.service.ProductService;
import com.example.seckill.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.seckill.constants.RedisConstants.CACHE_EXPIRE_TIME;
import static com.example.seckill.constants.RedisConstants.SECKILL_STATUS;

@RestController
@RequestMapping("/api/admin")
public class AdminController {


    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 1. 单商品预热
     * GET /api/admin/preheat?productId=100
     */
    @GetMapping("/preheat")
    public String preheat(Long productId) {
        // 1. 获取商品信息
        Product product = productService.getProductById(productId);
        if (product == null) {
            return "商品不存在";
        }

        // 2. 预热商品信息
        String key = RedisConstants.PRODUCT_KEY_PREFIX + productId;
        redisUtil.set(key, product, RedisConstants.CACHE_EXPIRE_TIME);

        // 3. 预热库存
        String stockKey = RedisConstants.STOCK_KEY_PREFIX + productId;
        redisUtil.set(stockKey, product.getStock(), RedisConstants.CACHE_EXPIRE_TIME);

        //3.预热状态
        String statusKey= SECKILL_STATUS+productId;
        redisUtil.set(statusKey,1,RedisConstants.CACHE_EXPIRE_TIME);

        return "✅ 预热成功！商品ID：" + productId +
                "，库存：" + product.getStock() +
                "，秒杀状态：进行中";

    }

    /**
     * 2. 批量预热
     * GET /api/admin/preheatBatch?productIds=100,101,102
     */
    @GetMapping("/preheatBatch")
    public String preheatBatch(String productIds) {
        if(productIds == null){
            return "请传入商品ID列表，用逗号分隔";
        }

        String[] ids = productIds.split(",");
        int successCount = 0;
        int failCount = 0;
        StringBuilder msg = new StringBuilder();

        for(String id : ids){
            Long productId = Long.parseLong(id.trim());
            try{
                Product product = productService.getProductById(productId);
                if (product == null) {
                    failCount++;
                    msg.append("商品ID").append(productId).append("不存在");
                    continue;
                }
                String key = RedisConstants.PRODUCT_KEY_PREFIX + productId;
                redisUtil.set(key, product, RedisConstants.CACHE_EXPIRE_TIME);

                String stockKey = RedisConstants.STOCK_KEY_PREFIX + productId;
                redisUtil.set(stockKey, product.getStock(), RedisConstants.CACHE_EXPIRE_TIME);

                String statusKey= SECKILL_STATUS+productId;
                redisUtil.set(statusKey,1,RedisConstants.CACHE_EXPIRE_TIME);
                successCount++;
                msg.append("商品ID").append(productId).append("预热成功！").append("\n");

            } catch (Exception e) {
                failCount++;
                msg.append("商品ID").append(productId).append("预热失败！").append(e.getMessage());

            }
        }
        return String.format("预热完成！成功%d个，失败%d个。详情：%s", successCount, failCount, msg.toString());
    }

    /**
     * 3. 结束秒杀
     * GET /api/admin/endSeckill?productId=100
     */
    @GetMapping("/endSeckill")
    public String endSeckill(Long productId) {
        String statusKey = "seckill:status:" + productId;
        if (redisUtil.get(statusKey) == null) {
            return "该商品未预热或已结束";
        }
        redisUtil.set(statusKey, 2, 3600L);
        return "秒杀已结束，商品ID：" + productId;
    }

    /**
     * 4.批量结束秒杀
     * GET /api/admin/endSeckillBatch?productIds=100,101,102
     */
    @GetMapping("/endSeckillBatch")
    public String endSeckillBatch(String productIds) {
        if(productIds == null|| productIds.trim().isEmpty()){
            return "请传入商品ID列表，用逗号分隔";
        }

        String[] ids = productIds.split(",");
        int successCount = 0;
        int failCount = 0;
        StringBuilder msg = new StringBuilder();

        for(String id : ids){
            Long productId = Long.parseLong(id.trim());
            String statusKey = SECKILL_STATUS+ productId;
            try{
                if (redisUtil.get(statusKey) == null) {
                    failCount++;
                    msg.append("商品ID").append(productId).append("未预热或已结束").append("\n");
                    continue;
                }
                redisUtil.set(statusKey, 2, 3600L);
                successCount++;
                msg.append("商品ID").append(productId).append("已结束").append("\n");
            } catch (Exception e) {
                failCount++;
                msg.append("商品ID").append(productId).append("操作失败（").append(e.getMessage()).append(");");
            }
        }
        return String.format("操作完成！成功%d个，失败%d个。详情：%s", successCount, failCount, msg.toString());
    }


    /**
     * 5. 查看所有秒杀活动状态
     * GET /api/admin/seckillStatus
     */
    @GetMapping("/seckillStatus")
    public Map<String, Object> seckillStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> keys = redisUtil.keys("seckill:status:*");

        if (keys == null || keys.isEmpty()) {
            result.put("提示", "暂无秒杀活动，请先预热");
            return result;
        }

        Map<String, String> statusMap = new LinkedHashMap<>();
        for (String key : keys) {
            String productId = key.replace("seckill:status:", "");
            Integer status = (Integer) redisUtil.get(key);
            String statusText = status == null ? "未开始" :
                    status == 1 ? "进行中" :
                            status == 2 ? "已结束" : "未知";
            statusMap.put("商品ID " + productId, statusText);
        }

        result.put("秒杀活动状态", statusMap);
        result.put("活动总数", statusMap.size());
        return result;
    }

    /**
     * 6. 紧急熔断：一键结束所有正在进行的秒杀活动
     * GET /api/admin/emergencyStop
     */
     @GetMapping("/emergencyStop")
     public String emergencyStop() {
         Set<String> keys = redisUtil.keys(SECKILL_STATUS + "*");
         if (keys == null || keys.isEmpty()) {
             return "没有进行中的秒杀活动";
         }

         int successCount = 0;
         int failCount = 0;
         for (String key : keys) {
             try {
                 Integer status = (Integer) redisUtil.get(key);
                 if (status != null && status == 1) {
                     String productId = key.replace(SECKILL_STATUS, "");
                     redisUtil.set(key, 2, CACHE_EXPIRE_TIME);
                     successCount++;
                     System.out.println("紧急熔断：商品" + productId);
                 }
             } catch (Exception e) {
                 failCount++;
                 System.err.println("商品" + key.replace(SECKILL_STATUS, "") + "熔断失败：" + e.getMessage());
             }
         }
         return String.format("紧急熔断完成！成功关闭 %d 个秒杀活动，未能关闭%d个秒杀活动", successCount, failCount);
     }

    /**
     * 7.商家更新商品信息（部分更新）
     * PATCH /api/admin/updateProduct
     */
    @PatchMapping("/updateProduct")
    public String updateProduct(@RequestParam Long productId,
                                @RequestParam(required = false) String name,
                                @RequestParam(required = false) BigDecimal price,
                                @RequestParam(required = false) Integer stock,
                                @RequestParam(required = false) String description) {
        try{
            int updated=productService.updateProduct(productId,name,price,stock, description);
            return "商品更新成功，影响行数："+updated;
        }catch(Exception e){
            return "商品更新失败："+e.getMessage();
        }
    }

    /**
     * 8.支付统计看板
     * GET /api/admin/statistics/payment?startTime=2026-06-01&endTime=2026-06-30
     */
    @GetMapping("/statistics/payment")
    public Result<Map<String, Object>> paymentStatistics(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime){
        //1.默认时间范围：当月
        if(startTime == null || startTime.trim().isEmpty()){
            startTime=getCurrentMonthFirstDay();
        }
        if(endTime == null || endTime.trim().isEmpty()){
            endTime=getCurrentMonthLastDay();
        }

        //2.查询统计数据
        Integer total = orderMapper.countByTime(startTime, endTime);
        Integer paid = orderMapper.countByStatusAndTime(1, startTime, endTime);
        Integer pending = orderMapper.countByStatusAndTime(0, startTime, endTime);
        Integer cancelled = orderMapper.countByStatusAndTime(2, startTime, endTime);

        Integer timeout = orderMapper.countByStatusAndTime(3, startTime, endTime);

        // 3. 计算支付率
        BigDecimal payRate = BigDecimal.ZERO;
        if (total != null && total > 0) {
            payRate = BigDecimal.valueOf(paid)
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }

        // 4. 封装返回数据
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("时间范围", startTime + " ~ " + endTime);
        result.put("总订单数", total != null ? total : 0);
        result.put("已支付", paid != null ? paid : 0);
        result.put("待支付", pending != null ? pending : 0);
        result.put("已取消", cancelled != null ? cancelled : 0);
        result.put("超时未支付", timeout != null ? timeout : 0);
        result.put("支付率", payRate.multiply(BigDecimal.valueOf(100)).intValue() + "%");

        return Result.success(result);

    }
    /**
     * 9.获取当前月份第一天
     */
    private String getCurrentMonthFirstDay() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return now.withDayOfMonth(1).toString();
    }

    /**
     * 10.获取当前月份最后一天
     */
    private String getCurrentMonthLastDay() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return now.withDayOfMonth(now.lengthOfMonth()).toString();
    }

    /**
     * 11.查询所有订单（商家专用）
     * GET /api/admin/orders
     */
    @GetMapping("/orders")
    public Result<Map<String, Object>> getAllOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        // 默认时间范围
        if (startTime == null || startTime.trim().isEmpty()) {
            startTime = "1970-01-01";
        }
        if (endTime == null || endTime.trim().isEmpty()) {
            endTime = "2099-12-31";
        }

        // 分页参数
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        int offset = (page - 1) * pageSize;

        // 查询数据
        List<Order> orders = orderMapper.findAllByTime(startTime, endTime);
        // 这里可以根据 status 进行过滤，也可以直接在 SQL 中加条件
        // 为了简洁，这里在内存中过滤（数据量小时可以）
        if (status != null) {
            orders = orders.stream()
                    .filter(o -> o.getStatus().equals(status))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 手动分页
        int total = orders.size();
        int fromIndex = Math.min(offset, total);
        int toIndex = Math.min(offset + pageSize, total);
        List<Order> pageList = orders.subList(fromIndex, toIndex);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", pageList);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (total + pageSize - 1) / pageSize);

        return Result.success(result);
    }


}
