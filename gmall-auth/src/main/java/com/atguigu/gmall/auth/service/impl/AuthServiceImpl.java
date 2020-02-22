package com.atguigu.gmall.auth.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.auth.service.AuthService;
import com.atguigu.gmall.ums.entity.MemberEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
@EnableConfigurationProperties({JwtProperties.class})
public class AuthServiceImpl implements AuthService {

    @Autowired
    private GmallUmsClient umsClient;
    @Resource
    private JwtProperties jwtProperties;
    @Override
    public String accredit(String username, String password) {
        //查询用户
        Resp<MemberEntity> memberEntityResp = umsClient.queryUser(username, password);
        MemberEntity memberEntity = memberEntityResp.getData();

        //判断是否存在
        if (memberEntity == null){
            return null;
        }
        try {
            //生成jwt
            Map<String,Object> map = new HashMap<>();
            map.put("id",memberEntity.getId());
            map.put("userName",memberEntity.getUsername());
            return JwtUtils.generateToken(map,jwtProperties.getPrivateKey(),jwtProperties.getExpireTime());


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
