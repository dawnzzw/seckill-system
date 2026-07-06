package com.example.seckill.mapper;

import com.example.seckill.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    //添加用户
    int insert(User user);

    //根据用户名查询用户
    User findByUsername(@Param("username") String username);

    //根据用户id查询用户
    User findById(@Param("id") Long id);


}
