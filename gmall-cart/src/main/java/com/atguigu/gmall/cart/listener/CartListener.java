package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

@Component
public class CartListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public static final String PRICE_PREFIX = "cart:price:";

    public static final String KEY_PREFIX = "cart:item:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART-UPDATE-QUEUE",durable = "true"),
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = "item.update"
    ))
    public void listener(Long spuId){
        Resp<List<SkuInfoEntity>> querySkuBuSpuId = pmsClient.querySkuBuSpuId(spuId);
        List<SkuInfoEntity> skuInfoEntities = querySkuBuSpuId.getData();

        if (!CollectionUtils.isEmpty(skuInfoEntities)){
            skuInfoEntities.forEach(skuInfoEntity -> {
                stringRedisTemplate.opsForValue().set(PRICE_PREFIX + skuInfoEntity.getSkuId(),skuInfoEntity.getPrice().toString());
            });
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART-DELETE-QUEUE",durable = "ture"),
            exchange = @Exchange(value = "ORDER-EXCHENGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"cart.delete"}
    ))
    public void deleteListener(Map<String,Object> map){

        try {
            Long userId = (Long) map.get("userId");
            Object skuIds = map.get("skuIds");
            if (skuIds == null){
                return;
            }
            BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(KEY_PREFIX + userId);

            List<String> ids = JSON.parseArray(skuIds.toString(), String.class);
            hashOps.delete(ids.toArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
