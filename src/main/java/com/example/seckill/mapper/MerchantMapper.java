package com.example.seckill.mapper;

import com.example.seckill.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MerchantMapper {

    //添加商户
    int insert(Merchant merchant);

    //根据商户名查询商户
    Merchant findByUsername( @Param("username") String username);

    //根据ID查询商户
    Merchant findById( @Param("id") Long id);
}
