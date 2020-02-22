package com.atguigu.gmall.auth;

import com.atguigu.core.utils.JwtUtils;
import com.atguigu.core.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class GmallAuthApplicationTests {

    @Test
    public void contextLoads() {
    }

    private static final String pubKeyPath = "E:\\javaSE0805\\rsa\\rsa.pub";

    private static final String priKeyPath = "E:\\javaSE0805\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "23swfsdfwewettg4");
    }

        @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE1NzkxMDE4NDZ9.rAWkmIUaRR9TwSbCF8pdNLZrtRxErmyc6A63E5o-TjJTdi_i55j5tRZweLX3mBvgFRKhwja_7qHJHpzw2vIlHVGHijVIgUlGRQ6R4UA7cF-Ki6Op9S-oKpz0eTcplv_h7G6qLMWjadI2q77LMkXF3C4c2B5mbupHM70WCk9zNTSlGRqiEOQZ8y4Cm51LsFWaFUGBTYbt4raRjCibx9ZB3-KKdQeZeGJEIxrixyZOPQMZZgONDrkRnWGaHhIeJEiSreFnxwXjM__DRTLBAy4pjgcjnPTSxJbATcJofBERjrjEFBLJhM5j_uo97mZZGL6NNiXF79Xbnf7Q6W5tRi0pKg";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
