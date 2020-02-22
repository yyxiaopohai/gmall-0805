package com.example.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVO;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import com.atguigu.sms.vo.ItemSaleVO;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.gmall.order.feign.*;
import com.example.gmall.order.interceptor.LoginInterceptor;
import com.example.gmall.order.service.OrderService;
import com.example.gmall.order.vo.OrderConfirmVO;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallOmsClient omsClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private AmqpTemplate amqpTemplate;

    private static final String TOKEN_PREFIX = "order:token:";

    @Override
    public OrderConfirmVO confirm() {

        OrderConfirmVO orderConfirmVO = new OrderConfirmVO();

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        //地址信息（远程调用）
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            Resp<List<MemberReceiveAddressEntity>> addressesResp = umsClient.queryAddressesByUserId(userInfo.getUserId());
            List<MemberReceiveAddressEntity> addresses = addressesResp.getData();

            orderConfirmVO.setAddresses(addresses);
        }, threadPoolExecutor);

        //订单信息（远程调用）
        CompletableFuture<Void> itemsFuture = CompletableFuture.supplyAsync(() -> {
            return cartClient.queryCheckdCarts(userInfo.getUserId());
        }).thenAcceptAsync(carts -> {

            List<OrderItemVO> orderItems = carts.stream().map(cart -> {
                Long skuId = cart.getSkuId();
                Integer count = cart.getCount();
                OrderItemVO orderItemVO = new OrderItemVO();

                orderItemVO.setCount(count);
                orderItemVO.setSkuId(skuId);

                CompletableFuture<Void> skuFuture = CompletableFuture.runAsync(() -> {

                    Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(skuId);
                    SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                    if (skuInfoEntity != null) {
                        orderItemVO.setPrice(skuInfoEntity.getPrice());
                        orderItemVO.setImage(skuInfoEntity.getSkuDefaultImg());
                        orderItemVO.setWeight(skuInfoEntity.getWeight());
                        orderItemVO.setSkuTitle(skuInfoEntity.getSkuTitle());
                    }
                }, threadPoolExecutor);

                CompletableFuture<Void> storeFuture = CompletableFuture.runAsync(() -> {

                    Resp<List<WareSkuEntity>> wareSkuResp = wmsClient.queryWareSkuBySkuId(skuId);
                    List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        orderItemVO.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                    }
                }, threadPoolExecutor);

                CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {

                    Resp<List<SkuSaleAttrValueEntity>> listResp = pmsClient.querySaleAttrValueBySkuId(skuId);
                    List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = listResp.getData();
                    orderItemVO.setSaleAttrs(skuSaleAttrValueEntities);
                }, threadPoolExecutor);

                CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {

                    Resp<List<ItemSaleVO>> saleResp = smsClient.queryItemSaleVOByskuId(skuId);
                    List<ItemSaleVO> itemSaleVOS = saleResp.getData();
                    orderItemVO.setSales(itemSaleVOS);
                }, threadPoolExecutor);
                CompletableFuture.allOf(skuFuture, storeFuture, saleAttrFuture, salesFuture).join();

                return orderItemVO;

            }).collect(Collectors.toList());
            orderConfirmVO.setOrderItems(orderItems);
        }, threadPoolExecutor);

        //积分信息（远程调用）
        CompletableFuture<Void> boundsFuture = CompletableFuture.runAsync(() -> {

            Resp<MemberEntity> memberEntityResp = umsClient.queryMemberById(userInfo.getUserId());
            MemberEntity memberEntity = memberEntityResp.getData();
            if (memberEntity != null) {
                orderConfirmVO.setBounds(memberEntity.getIntegration());
            }
        }, threadPoolExecutor);

        //防重复提交唯一标志
        CompletableFuture<Void> tokenFuture = CompletableFuture.runAsync(() -> {

            String orderToken = IdWorker.getTimeId();
            orderConfirmVO.setOrderToken(orderToken);

            stringRedisTemplate.opsForValue().set(TOKEN_PREFIX + orderToken, orderToken, 3, TimeUnit.HOURS);
        }, threadPoolExecutor);

        CompletableFuture.allOf(addressFuture, itemsFuture, boundsFuture, tokenFuture).join();
        return orderConfirmVO;
    }

    @Override
    public OrderEntity submit(OrderSubmitVO orderSubmitVO) {

        //1. 校验是否重复提交
        //判断redis中有没有，（有-就放行提交，再删除redis中的ordertoken信息）---要具备原子性
        String orderToken = orderSubmitVO.getOrderToken();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long flag = (Long) stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(TOKEN_PREFIX + orderSubmitVO.getOrderToken()), orderToken);

        if (flag == 0) {
            throw new OrderException("请不要重复提交订单");
        }

        //2. 验价
        BigDecimal totalPrice = orderSubmitVO.getTotalPrice();
        //获取书库中的实时价格
        List<OrderItemVO> items = orderSubmitVO.getItems();
        if (CollectionUtils.isEmpty(items)) {
            throw new OrderException("请勾选需要的商品");
        }
        BigDecimal currentTotalPrice = items.stream().map(orderItemVO -> {
            //获取skuId
            Long skuId = orderItemVO.getSkuId();
            //查询数据库中的真实数据
            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity != null) {
                return skuInfoEntity.getPrice().multiply(new BigDecimal(orderItemVO.getCount()));
            }
            return new BigDecimal(0);

        }).reduce((a, b) -> a.add(b)).get();

        if (totalPrice.compareTo(currentTotalPrice) != 0) {
            throw new OrderException("页面已过期，请刷新后重试");
        }
        //3. 锁定库存

        List<SkuLockVO> skuLockVOS = items.stream().map(orderItemVO -> {
            SkuLockVO skuLockVO = new SkuLockVO();
            skuLockVO.setSkuId(orderItemVO.getSkuId());
            skuLockVO.setCount(orderItemVO.getCount());
            skuLockVO.setOrderToken(orderSubmitVO.getOrderToken());

            return skuLockVO;
        }).collect(Collectors.toList());
        Resp<List<SkuLockVO>> lockResp = wmsClient.checkAndLock(skuLockVOS);
        List<SkuLockVO> lockVOS = lockResp.getData();
        if (!CollectionUtils.isEmpty(lockVOS)) {
            throw new OrderException(JSON.toJSONString(lockVOS));
        }

        //4. 创建订单信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        OrderEntity orderEntity = null;
        try {
            Resp<OrderEntity> orderEntityResp = omsClient.saveOrder(orderSubmitVO, userInfo.getUserId());
            orderEntity = orderEntityResp.getData();
        } catch (Exception e) {
            e.printStackTrace();

            amqpTemplate.convertAndSend("ORDER-EXCHANGE","stock.unlock",orderSubmitVO.getOrderToken());

            throw new OrderException("订单保存失败");
    }

        //5. 删除购物车信息

        try {
            Map<String,Object> map = new HashMap<>();
            map.put("userId",userInfo.getUserId());
            List<Long> skuIds = items.stream().map(orderItemVO -> orderItemVO.getSkuId()).collect(Collectors.toList());
            map.put("skuIds",JSON.toJSONString(skuIds));

            amqpTemplate.convertAndSend("ORDER-EXCHENGE","cart.delete",map);
        } catch (AmqpException e) {
            e.printStackTrace();
        }
        return orderEntity;
    }
}
