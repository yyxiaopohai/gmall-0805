package com.atguigu.gmall.cart.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import jdk.nashorn.internal.ir.ReturnNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.RecursiveTask;

@RestController
@RequestMapping("cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping
    public Resp<Object> addCart(@RequestBody Cart cart){

        cartService.addCart(cart);

        return Resp.ok(null);
    }

    @GetMapping
    public Resp<List<Cart>> queryCarts(){
        List<Cart> carts = cartService.queryCarts();
        return Resp.ok(carts);
    }

    @PostMapping("update")
    public Resp<Object> updateNum(@RequestBody Cart cart){

        cartService.updateNum(cart);

        return Resp.ok(null);
    }

    @PostMapping("check")
    public Resp<Object> check(@RequestBody Cart cart){
        cartService.check(cart);
        return Resp.ok(null);
    }

    @PostMapping("delete")
    public Resp<Object> delete(@RequestParam("skuId") Long skuId){
        cartService.delete(skuId);
        return Resp.ok(null);
    }

    @GetMapping("{userId}")
    public List<Cart> queryCheckdCarts(@PathVariable("userId")Long userId){
        return cartService.queryCheckdCarts(userId);
    }
}
