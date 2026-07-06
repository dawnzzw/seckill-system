package com.example.seckill.service;

import com.example.seckill.entity.Merchant;
import com.example.seckill.mapper.MerchantMapper;
import com.example.seckill.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MerchantService {

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 商户注册
     */
    public String register(String username,String password,String shopName,String phone) {
        //1.检查商户名是否已存在
        Merchant existing = merchantMapper.findByUsername(username);
        if (existing != null) {
            throw new RuntimeException("商户名已存在");
        }

        //2.密码长度校验
        if (password == null || password.length() < 6 || password.length() > 20) {
            throw new RuntimeException("密码长度必须在6-20位之间");
        }

        //3.密码加密
        String encodedPassword = passwordEncoder.encode(password);

        //4.创建商户
        Merchant merchant = new Merchant();
        merchant.setUsername(username);
        merchant.setPassword(encodedPassword);
        merchant.setShopName(shopName);
        merchant.setPhone(phone);
        merchant.setStatus(1);

        merchantMapper.insert(merchant);
        return "商户注册成功";
    }

    /**
     * 商户登录
     */
    public String login(String username, String password) {
        //1.查询商户
        Merchant merchant=merchantMapper.findByUsername(username);
        if (merchant == null) {
            throw new RuntimeException("商户不存在");
        }

        //2.查询商户状态
        if (merchant.getStatus() == 0) {
            throw new RuntimeException("该商户账号已禁用");
        }
        //3.验证密码
        if (!passwordEncoder.matches(password, merchant.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        return jwtUtil.generateToken(merchant.getId(), merchant.getUsername(), "MERCHANT");
    }

    /**
     * 根据商户名查询商户
     */
    public Merchant findByUsername(String username) {
        return merchantMapper.findByUsername(username);
    }

    /**
     * 根据商户ID查询商户
     */
    public Merchant findById(Long id) {
        return merchantMapper.findById(id);
    }
}
