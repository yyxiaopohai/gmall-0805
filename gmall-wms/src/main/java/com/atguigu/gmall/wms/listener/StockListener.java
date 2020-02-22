package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class StockListener {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private WareSkuDao wareSkuDao;

    private static final String KEY_PREFIX = "wms:stock:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK-UNLOCK-QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.unlock","wms.dead"}
    ))
    public void unlock(String orderToken){

        String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + orderToken);

        if (StringUtils.isEmpty(json)){
            return;
        }
        List<SkuLockVO> skuLockVOS = JSON.parseArray(json, SkuLockVO.class);

        skuLockVOS.forEach(skuLockVO -> {
            wareSkuDao.unLock(skuLockVO.getWareSkuId(),skuLockVO.getCount());
            stringRedisTemplate.delete(KEY_PREFIX + orderToken);
        });
    }

    @RabbitListener(queues = {"ORDER-DEAD-QUEUE"})
    public void testListener(String msg){
        System.out.println("消费者拿到死信消息" + msg);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK-MINUS-QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void minus(String orderToken){

        String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + orderToken);

        if (StringUtils.isEmpty(json)){
            return;
        }
        List<SkuLockVO> skuLockVOS = JSON.parseArray(json, SkuLockVO.class);

        skuLockVOS.forEach(skuLockVO -> {
            wareSkuDao.minus(skuLockVO.getWareSkuId(),skuLockVO.getCount());
            stringRedisTemplate.delete(KEY_PREFIX + orderToken);
        });
    }
}
