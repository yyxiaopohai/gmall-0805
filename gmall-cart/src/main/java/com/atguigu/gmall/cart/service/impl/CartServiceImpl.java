package com.atguigu.gmall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.sms.vo.ItemSaleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Resource
    private GmallSmsClient smsClient;
    @Resource
    private GmallWmsClient wmsClient;
    @Resource
    private GmallPmsClient pmsClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "cart:item:";

    private static final String PRICE_PREFIX = "cart:price:";
    @Override
    public void addCart(Cart cart) {

        String key = KEY_PREFIX;
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        if (userInfo.getUserId() != null){
            key += userInfo.getUserId();
        }else {
            key += userInfo.getUserKey();
        }

        //获取购物车信息
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(key);

        String skuId = cart.getSkuId().toString();
        Integer count = cart.getCount();
        //判断是否有该商品
        if (hashOps.hasKey(skuId)){
            //有，更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson,Cart.class);
            cart.setCount(cart.getCount()+count);

        }else {
            //没有，添加
            Resp<SkuInfoEntity> skuInfoEntity = this.pmsClient.querySkuById(cart.getSkuId());
            SkuInfoEntity skuInfoEntityData = skuInfoEntity.getData();
            if (skuInfoEntityData == null){
                return;
            }
            cart.setPrice(skuInfoEntityData.getPrice());
            cart.setImage(skuInfoEntityData.getSkuDefaultImg());
            cart.setSkuTitle(skuInfoEntityData.getSkuTitle());

            Resp<List<WareSkuEntity>> listResp = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> skuEntities = listResp.getData();
            if (!CollectionUtils.isEmpty(skuEntities)){
                cart.setStore(skuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
            }

            Resp<List<SkuSaleAttrValueEntity>> saleAttrValue = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = saleAttrValue.getData();
            cart.setSaleAttrs(skuSaleAttrValueEntities);

            cart.setCheck(true);

            Resp<List<ItemSaleVO>> saleResp = this.smsClient.queryItemSaleVOByskuId(cart.getSkuId());
            List<ItemSaleVO> itemSaleVOS = saleResp.getData();
            cart.setSales(itemSaleVOS);
            stringRedisTemplate.opsForValue().set(PRICE_PREFIX + skuId,skuInfoEntityData.getPrice().toString());
        }
        hashOps.put(skuId,JSON.toJSONString(cart));

    }

    @Override
    public void updateNum(Cart cart) {
        //获取登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //组装外层的key
        String key = KEY_PREFIX;
        if (userInfo.getUserId() != null){
            key += userInfo.getUserId();
        } else {
            key += userInfo.getUserKey();
        }

        //获取内层的key
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(key);

        if (hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            Integer count = cart.getCount();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        }
    }

    @Override
    public void check(Cart cart) {
        //获取登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //组装外层的key
        String key = KEY_PREFIX;
        if (userInfo.getUserId() != null){
            key += userInfo.getUserId();
        } else {
            key += userInfo.getUserKey();
        }

        //获取内层的key
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(key);

        if (hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            Boolean check = cart.getCheck();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCheck(check);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        }
    }

    @Override
    public void delete(Long skuId) {
        //获取登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //组装外层的key
        String key = KEY_PREFIX;
        if (userInfo.getUserId() != null){
            key += userInfo.getUserId();
        } else {
            key += userInfo.getUserKey();
        }

        //获取内层的key
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(key);

        //判断购物车有没有这个数据
        if (hashOps.hasKey(skuId.toString())){
            hashOps.delete(skuId.toString());
        }
    }

    @Override
    public List<Cart> queryCarts() {
        //获取用户登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = KEY_PREFIX + userInfo.getUserKey();
        Long userId = userInfo.getUserId();

        BoundHashOperations<String, Object, Object> userKeyHashOps = this.stringRedisTemplate.boundHashOps(userKey);
        List<Object> values = userKeyHashOps.values();

        List<Cart> userKeyCarts = null;
        if (!CollectionUtils.isEmpty(values)){
            userKeyCarts = values.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                String currentPrice = stringRedisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(currentPrice));
                return cart;
            }).collect(Collectors.toList());

        }

        //判断是否登录，未登录直接返回
        if (userId == null){
            return userKeyCarts;
        }

        //登录了合并未登录的购物车到登录的购物车
        String userIdKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> userIdHashOps = this.stringRedisTemplate.boundHashOps(userIdKey);

        if (!CollectionUtils.isEmpty(userKeyCarts)){
            userKeyCarts.forEach(cart -> {
                if (userIdHashOps.hasKey(cart.getSkuId().toString())){
                    String cartJson = userIdHashOps.get(cart.getSkuId().toString()).toString();
                    Integer count = cart.getCount();
                    cart = JSON.parseObject(cartJson, Cart.class);
                    cart.setCount(cart.getCount()+count);

                }
                userIdHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            });

            this.stringRedisTemplate.delete(userKey);

            List<Object> userIdCartJsons = userIdHashOps.values();
            if (!CollectionUtils.isEmpty(userIdCartJsons)){
                return userIdCartJsons.stream().map(cartJson -> {
                    Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);

                    String currentPrice = stringRedisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                    cart.setCurrentPrice(new BigDecimal(currentPrice));
                    return cart;
                }).collect(Collectors.toList());
            }
        }
        return null;
    }

    @Override
    public List<Cart> queryCheckdCarts(Long userId) {

        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(KEY_PREFIX + userId);
        List<Object> values = hashOps.values();
        if (!CollectionUtils.isEmpty(values)){
            return values.stream().map(cartJson -> JSON.parseObject(cartJson.toString(),Cart.class)).filter(cart -> cart.getCheck()).collect(Collectors.toList());
        }
        return null;
    }


}
