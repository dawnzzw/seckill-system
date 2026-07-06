package com.example.seckill.service;

import com.example.seckill.entity.User;
import com.example.seckill.mapper.UserMapper;
import com.example.seckill.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    /**
     * 用户注册
     */
    public String register(String username,String password,String phone) {
        //1.检查用户名是否已存在
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }
        //2.密码长度校验
        if(password == null || password.length() < 6 || password.length() > 20){
            throw new RuntimeException("密码长度必须在6到20位之间");
        }

        //3.密码加密
        String encodedPassword = passwordEncoder.encode(password);

        //4.创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setPhone(phone);

        userMapper.insert(user);
        return "用户注册成功";
    }

    /**
     * 用户登录
     */
    public String login(String username, String password) {

        //1.查询用户
        User user=userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        //2.验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        //3.生成Token
        return jwtUtil.generateToken(user.getId(), user.getUsername(), "USER");
    }

    /**
     * 根据用户名查询用户
     */
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    /**
     * 根据用户ID查询用户
     */
    public User findById(Long id) {
        return userMapper.findById(id);
    }
}
