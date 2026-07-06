package com.example.seckill.controller;

import com.example.seckill.entity.Product;
import com.example.seckill.service.ProductService;
import com.example.seckill.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ProductController {

    @Autowired
    private  ProductService productService;

    @Autowired
    private StockService stockService;

    // 获取所有商品
    @GetMapping("/api/product/list")
    public List<Product> listProducts() {
        return productService.getAllProducts();
    }

    //根据ID获取商品信息
    @GetMapping("/api/product/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // 重置库存
    @GetMapping("/api/product/resetStock/{id}/{stock}")
    public String resetStock(@PathVariable Long id, @PathVariable Integer stock) {
        stockService.resetStock(id,stock);
        return "库存已重置为："+stock;
    }

    //根据ID获取库存信息
    @GetMapping("/api/stock/{id}")
    public Map<String, Object> getStock(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Integer stock = stockService.getCurrentStock(id);
        result.put("productId", id);
        result.put("stock", stock != null ? stock : 0);
        return result;
    }
}
