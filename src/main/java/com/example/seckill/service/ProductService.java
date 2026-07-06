package com.example.seckill.service;

import com.example.seckill.constants.RedisConstants;
import com.example.seckill.entity.Product;
import com.example.seckill.entity.ProductUpdateDTO;
import com.example.seckill.mapper.ProductMapper;
import com.example.seckill.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.example.seckill.constants.RedisConstants.CACHE_EXPIRE_TIME;

@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ObjectMapper objectMapper;  // 注入 Jackson

    public Product getProductById(Long id) {
        String key = RedisConstants.PRODUCT_KEY_PREFIX + id;

        // 1. 从 Redis 取数据
        Object cached = redisUtil.get(key);

        // 2. 如果缓存存在
        if (cached != null) {
            System.out.println("从Redis缓存中获取商品：" + id);
            // 如果已经是 Product，直接返回
            if (cached instanceof Product) {
                return (Product) cached;
            }
            // 如果是 LinkedHashMap，转换成 Product
            return objectMapper.convertValue(cached, Product.class);
        }

        // 3. 缓存没有，查 MySQL
        System.out.println("从MySQL数据库中获取商品：" + id);
        Product product = productMapper.findById(id);

        // 4. 写入 Redis
        if (product != null) {
            redisUtil.set(key, product, CACHE_EXPIRE_TIME);
            // 同时缓存库存
            String stockKey = RedisConstants.STOCK_KEY_PREFIX + id;
            redisUtil.set(stockKey, product.getStock(), CACHE_EXPIRE_TIME);
        }

        return product;
    }

    // 获取所有商品
    public List<Product> getAllProducts() {
        return productMapper.findAll();
    }

    // 更新商品信息
    public int updateProduct(Long productId, String name, BigDecimal price, Integer stock,String  description) {
        Product product=productMapper.findById(productId);
        if(product==null){
            throw new RuntimeException("商品不存在，ID："+productId);
        }

        //至少传一个字段
        if(name==null&&price==null&&stock==null&&description==null){
            throw new RuntimeException("请至少传入一个需要更新的字段");
        }

        ProductUpdateDTO updateDTO=new ProductUpdateDTO();
        updateDTO.setProductId(productId);
        updateDTO.setName(name);
        updateDTO.setPrice(price);
        updateDTO.setStock(stock);
        updateDTO.setDescription(description);

        int updated=productMapper.updateProductSelective(updateDTO);

        //清除Redis缓存
        if(name!=null||price!=null||stock!=null||description!=null) {
            redisUtil.delete(RedisConstants.PRODUCT_KEY_PREFIX + productId);
        }
        //更新库存
        if(stock!=null){
            redisUtil.set(RedisConstants.STOCK_KEY_PREFIX + productId, stock,CACHE_EXPIRE_TIME);
        }
        return updated;

    }
}