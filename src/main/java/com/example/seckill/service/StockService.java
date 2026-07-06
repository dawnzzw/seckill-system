package com.example.seckill.service;

import com.example.seckill.constants.RedisConstants;
import com.example.seckill.entity.Product;
import com.example.seckill.entity.StockUpdate;
import com.example.seckill.mapper.ProductMapper;
import com.example.seckill.util.RedisUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.example.seckill.constants.RedisConstants.*;

@Service
public class StockService {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductMapper productMapper;
    /**
     * 扣减库存（带分布式锁）
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 扣减后的库存数量，如果库存不足返回-1
     */
    public Integer decreaseStock(Long productId, Integer quantity) {
        String lockKey = LOCK_KEY_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            //尝试加锁（等待3秒，自动释放10秒）
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
            // 1. 查询当前库存
            String stockKey = STOCK_KEY_PREFIX + productId;
            Integer stock = (Integer) redisUtil.get(stockKey);

            // 2. 如果 Redis 中没有库存数据，从 MySQL 加载
            if (stock == null) {
                Product product = productService.getProductById(productId);
                if (product == null) {
                    throw new RuntimeException("商品不存在");
                }
                stock = product.getStock();
                redisUtil.set(stockKey, stock, RedisConstants.CACHE_EXPIRE_TIME);
            }

            // 3. 判断库存是否充足
            if (stock < quantity) {
                return -1;  // 库存不足
            }
            // 4. 扣减库存
            int newStock = stock - quantity;
            redisUtil.set(stockKey, newStock, RedisConstants.CACHE_EXPIRE_TIME);
            System.out.println("扣减库存成功，剩余：" + newStock);

            return newStock;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁失败", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询当前库存（不加锁）
     */
    public Integer getCurrentStock(Long productId) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        Integer stock = (Integer) redisUtil.get(stockKey);
        if (stock == null) {
            Product product = productService.getProductById(productId);
            if (product != null) {
                stock=product.getStock();
                redisUtil.set(stockKey, stock, RedisConstants.CACHE_EXPIRE_TIME);
            }
        }
        return stock;
    }

    /**
     * 重置库存（用于测试）
     */
    public void resetStock(Long productId, Integer stock) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        redisUtil.set(stockKey, stock, RedisConstants.CACHE_EXPIRE_TIME);
        System.out.println("库存已重置为：" + stock);
    }

    /**
     * 增加库存
     * @param productId
     * @param quantity
     */
    public void increaseStock(Long productId, Integer quantity) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        Integer currentStock = (Integer) redisUtil.get(stockKey);
        if (currentStock != null) {
            redisUtil.set(stockKey, currentStock + quantity);
        }
    }

    /**
     * 定时将 Redis 库存同步到 MySQL（每5分钟执行一次）
     */
    @Scheduled(cron=" 0 */5 * * * ?")
    @Transactional
    public void updateStockToMySQL() {
        //1.获取所有库存key
        Set<String> keys = redisUtil.keys(STOCK_KEY_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        List<StockUpdate> updates=new ArrayList<>();

        for(String key: keys){
            try{
                //从key中提取productId（格式：product:stock:1→1）
                Long productId=Long.parseLong(key.replace(STOCK_KEY_PREFIX,""));
                Integer redisStock = (Integer) redisUtil.get(key);

                if(redisStock!=null){
                    updates.add(new StockUpdate(productId,redisStock));
                }
            }catch(Exception e){
                System.err.println("解析库存Key失败："+key+"，"+e.getMessage());
            }
        }
        if(updates.isEmpty()){
            return;
        }

        //2.批量更新MySQL
        int updated=productMapper.batchUpdateStock(updates);
        System.out.println("库存同步完成：更新"+updated+"条记录，共"+updates.size()+"个商品");

    }

    /**
     * 每天凌晨2点执行：全量对账，修复不一致的数据（兜底）
     */
    @Scheduled(cron=" 0 0 2 * * ?")
    @Transactional
    public void reconcileStock(){
        //1.从MySQL中查询所有商品
        List<Product> products=productMapper.findAll();

        int fixedCount=0;

        for(Product product :products){
            String stockKey =STOCK_KEY_PREFIX+product.getId();
            Integer redisStock =(Integer) redisUtil.get(stockKey);

            if(redisStock == null){
                //Redis中没有库存数据的话，就从MySQL中获取数据存入Redis
                redisUtil.set(stockKey,product.getStock(),CACHE_EXPIRE_TIME);
                System.out.println("修复Redis缺失库存：商品ID="+product.getId()+"，库存："+product.getStock());
                continue;
            }

            //比较差异
            if(!redisStock.equals(product.getStock())){
                System.out.println("库存不一致：商品ID="+product.getId()
                        +"，MySQL="+product.getStock()
                        +"，Redis="+redisStock);

                //以Redis为准，修复MySQL
                productMapper.updateStock(product.getId(),redisStock);
                fixedCount++;
            }
        }
        if(fixedCount>0){
            System.out.println("库存对账完成：修复"+fixedCount+"个不一致的商品");
        }
        else{
            System.out.println("库存对账完成，所有商品库存一致");
        }
    }
}
