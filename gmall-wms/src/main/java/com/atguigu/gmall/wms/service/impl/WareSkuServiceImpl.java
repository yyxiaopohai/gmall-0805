package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;

    @Resource
    private WareSkuDao wareSkuDao;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private AmqpTemplate amqpTemplate;

    private static final String KEY_PREFIX = "wms:stock:";
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public List<SkuLockVO> checkAndLock(List<SkuLockVO> skuLockVOS) {

        //判断传过来的集合是否为空，
        if (CollectionUtils.isEmpty(skuLockVOS)){
            //为空直接返回
            return null;
        }
        //不为空，遍历集合，验证库存并且锁库存

        skuLockVOS.forEach(skuLockVO -> {
            checkLock(skuLockVO);
        });

        //判断锁定结果集中是否包含锁定失败的商品，（如果有锁定失败的，锁定成功的也要回滚）
        if (skuLockVOS.stream().anyMatch(skuLockVO -> skuLockVO.getLock() == false)) {
            //获取已经锁定成功的商品，解锁库存
            skuLockVOS.stream().filter(skuLockVO -> skuLockVO.getLock()).forEach(skuLockVO -> {
                //解锁库存
                wareSkuDao.unLock(skuLockVO.getWareSkuId(),skuLockVO.getCount());
            });
            return skuLockVOS;
        }

        String orderToken = skuLockVOS.get(0).getOrderToken();
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(skuLockVOS));

        amqpTemplate.convertAndSend("ORDER-EXCHANGE","wms.ttl",orderToken);
        return null;
    }

    private void checkLock(SkuLockVO skuLockVO){
        RLock fairLock = redissonClient.getFairLock("lock" + skuLockVO.getSkuId());

        fairLock.lock();
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.check(skuLockVO.getSkuId(), skuLockVO.getCount());
        if (!CollectionUtils.isEmpty(wareSkuEntities)){
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);

            int lock = wareSkuDao.lock(wareSkuEntity.getId(), skuLockVO.getCount());
            if (lock !=0) {
                skuLockVO.setLock(true);
                skuLockVO.setWareSkuId(wareSkuEntity.getId());
            }
        }


        fairLock.unlock();
    }

}