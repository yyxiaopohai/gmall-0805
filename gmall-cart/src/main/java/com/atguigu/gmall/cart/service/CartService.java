package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.pojo.Cart;

import java.util.List;

public interface CartService {
    void addCart(Cart cart);

    void updateNum(Cart cart);

    void check(Cart cart);

    void delete(Long skuId);

    List<Cart> queryCarts();

    List<Cart> queryCheckdCarts(Long userId);
}
