package com.example.seckill.mapper;

import com.example.seckill.entity.Product;
import com.example.seckill.entity.ProductUpdateDTO;
import com.example.seckill.entity.StockUpdate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {

    //查询所有商品
    List<Product> findAll();

    //根据ID查询商品信息
    Product findById(Long id);

    //更新单个商品库存（用于对账修复）
    int updateStock(@Param("productId") Long productId,@Param("stock") Integer stock);

    //批量更新商品库存
    int batchUpdateStock(@Param("list") List<StockUpdate> updates);


    //更新商品信息
    int updateProductSelective(ProductUpdateDTO updateDTO);
}
