package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.oms.dao.OrderItemDao;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.vo.OrderItemVO;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.oms.dao.OrderDao;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private OrderItemDao itemDao;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageVo(page);
    }

    @Transactional
    @Override
    public OrderEntity saveOrder(OrderSubmitVO orderSubmitVO, Long userId) {

        // 新增主表（订单表oms_order）
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSubmitVO.getOrderToken()); // 订单编号
        orderEntity.setTotalAmount(orderSubmitVO.getTotalPrice());
        orderEntity.setPayType(orderSubmitVO.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setDeliveryCompany(orderSubmitVO.getDeliveryCompany());
        orderEntity.setCreateTime(new Date());
        orderEntity.setModifyTime(orderEntity.getCreateTime());
        orderEntity.setStatus(0);
        orderEntity.setDeleteStatus(0);
//        orderEntity.setGrowth(); // 通过购买商品的积分优惠信息设置
        orderEntity.setMemberId(userId);
        // 收货地址
        MemberReceiveAddressEntity address = orderSubmitVO.getAddress();
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverRegion(address.getRegion());
//        orderEntity.setMemberUsername(); // 根据查询用户名
        // TODO
        boolean flag = this.save(orderEntity);

        // 新增子表（订单详情表oms_order_item）
        if (flag) {
            List<OrderItemVO> items = orderSubmitVO.getItems();
            if (!CollectionUtils.isEmpty(items)) {
                items.forEach(orderItemVO -> {
                    OrderItemEntity itemEntity = new OrderItemEntity();
                    itemEntity.setOrderSn(orderSubmitVO.getOrderToken());
                    itemEntity.setOrderId(orderEntity.getId());
                    // 先查询sku信息
                    Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(orderItemVO.getSkuId());
                    SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                    if (skuInfoEntity != null) {
                        itemEntity.setSkuId(orderItemVO.getSkuId());
                        itemEntity.setSkuQuantity(orderItemVO.getCount());
                        itemEntity.setSkuPic(orderItemVO.getImage());
                        itemEntity.setSkuName(orderItemVO.getSkuTitle());
                        itemEntity.setSkuAttrsVals(JSON.toJSONString(orderItemVO.getSaleAttrs()));
                        itemEntity.setSkuPrice(skuInfoEntity.getPrice());

                        // 根据spuId查询 spu设置spu信息
                        Long spuId = skuInfoEntity.getSpuId();
                        Resp<SpuInfoEntity> spuInfoEntityResp = this.pmsClient.querySpuById(spuId);
                        SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
                        if (spuInfoEntity != null) {
                            itemEntity.setSpuId(spuId);
                            itemEntity.setSpuName(spuInfoEntity.getSpuName());
                            itemEntity.setSpuBrand(spuInfoEntity.getBrandId().toString());
                            itemEntity.setCategoryId(spuInfoEntity.getCatalogId());
                        }
                        // 查询优惠信息，设置优惠信息 TODO
                    }
                    itemDao.insert(itemEntity);
                });
            }
        }

//        int i = 1/0;
        // 订单创建完成之后，定时关单
        this.amqpTemplate.convertAndSend("ORDER-EXCHANGE", "order.ttl", orderSubmitVO.getOrderToken());

        return orderEntity;
       /* //新增两张表
        //新增订单表（主表）
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSubmitVO.getOrderToken());//把order token设置成订单编号
        orderEntity.setTotalAmount(orderSubmitVO.getTotalPrice());
        orderEntity.setPayType(orderSubmitVO.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setDeliveryCompany(orderSubmitVO.getDeliveryCompany());
        orderEntity.setCreateTime(new Date());
        orderEntity.setModifyTime(orderEntity.getCreateTime());
        orderEntity.setStatus(0);
        orderEntity.setDeleteStatus(0);
        orderEntity.setMemberId(userId);
        //获取地址信息
        MemberReceiveAddressEntity adress = orderSubmitVO.getAddress();
        orderEntity.setReceiverCity(adress.getCity());
        orderEntity.setReceiverDetailAddress(adress.getDetailAddress());
        orderEntity.setReceiverName(adress.getName());
        orderEntity.setReceiverPhone(adress.getPhone());
        orderEntity.setReceiverPostCode(adress.getPostCode());
        orderEntity.setReceiverProvince(adress.getProvince());
        orderEntity.setReceiverRegion(adress.getRegion());

        boolean flag = save(orderEntity);

        //新增订单详情表
        if (flag) {
            List<OrderItemVO> items = orderSubmitVO.getItems();
            if (!CollectionUtils.isEmpty(items)) {
                items.forEach(orderItemVO -> {

                    OrderItemEntity orderItemEntity = new OrderItemEntity();
                    orderItemEntity.setOrderSn(orderSubmitVO.getOrderToken());
                    orderItemEntity.setOrderId(orderEntity.getId());

                    //先查询sku信息
                    Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(orderItemVO.getSkuId());
                    SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                    if (skuInfoEntity != null){
                        orderItemEntity.setSkuId(skuInfoEntity.getSkuId());
                        orderItemEntity.setSkuQuantity(orderItemVO.getCount());
                        orderItemEntity.setSkuPic(orderItemVO.getImage());
                        orderItemEntity.setSkuName(orderItemVO.getSkuTitle());
                        orderItemEntity.setSkuAttrsVals(JSON.toJSONString(orderItemVO.getSaleAttrs()));
                        orderItemEntity.setSkuPrice(skuInfoEntity.getPrice());

                        //根据spuId查询  spu设置spu信息
                        Long spuId = skuInfoEntity.getSpuId();
                        Resp<SpuInfoEntity> spuInfoEntityResp = pmsClient.querySpuById(spuId);
                        SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
                        if (spuInfoEntity != null) {
                            orderItemEntity.setSpuId(spuId);
                            orderItemEntity.setSpuName(spuInfoEntity.getSpuName());
                            orderItemEntity.setSpuBrand(spuInfoEntity.getBrandId().toString());
                            orderItemEntity.setCategoryId(spuInfoEntity.getCatalogId());
                        }
                    }
                    //再查询spu信息
                    orderItemDao.insert(orderItemEntity);
                });
            }
        }

//        int i = 1/0;
        return orderEntity;*/
    }

}