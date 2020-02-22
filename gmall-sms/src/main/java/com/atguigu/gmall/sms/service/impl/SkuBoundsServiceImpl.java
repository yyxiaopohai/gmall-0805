package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.dao.SkuFullReductionDao;
import com.atguigu.gmall.sms.dao.SkuLadderDao;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.sms.vo.ItemSaleVO;
import com.atguigu.sms.vo.SaleVO;
import io.seata.core.protocol.MergedWarpMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.sms.dao.SkuBoundsDao;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsDao, SkuBoundsEntity> implements SkuBoundsService {

    @Resource
    private SkuLadderDao ladderDao;
    @Resource
    private SkuFullReductionDao fullReductionDao;
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SkuBoundsEntity> page = this.page(
                new Query<SkuBoundsEntity>().getPage(params),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageVo(page);
    }

    @Transactional
    @Override
    public void saveSales(SaleVO saleVO) {
        //积分
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(saleVO,skuBoundsEntity);
        List<String> works = saleVO.getWork();
        skuBoundsEntity.setWork(new Integer(works.get(0))+new Integer(works.get(1))*2+new Integer(works.get(2))*4+new Integer(works.get(3))*8);
        this.save(skuBoundsEntity);
        //打折
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(saleVO,skuLadderEntity);
        skuLadderEntity.setAddOther(saleVO.getLadderAddOther());
        ladderDao.insert(skuLadderEntity);
        //满减
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(saleVO,skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(saleVO.getFullAddOther());
        fullReductionDao.insert(skuFullReductionEntity);
    }

    @Override
    public List<ItemSaleVO> queryItemSaleVOByskuId(Long skuId) {
        List<ItemSaleVO> itemSaleVOS = new ArrayList<>();
        //查询积分信息
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));

        if (skuBoundsEntity != null){
            ItemSaleVO itemSaleVO = new ItemSaleVO();
            itemSaleVO.setType("积分");
            itemSaleVO.setDesc("赠送成长积分："+ skuBoundsEntity.getGrowBounds() +",购物积分："+skuBoundsEntity.getBuyBounds());

            itemSaleVOS.add(itemSaleVO);
        }
        //查询满减信息
        SkuLadderEntity ladderEntity = this.ladderDao.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if (ladderEntity != null){
            ItemSaleVO itemSaleVO = new ItemSaleVO();
            itemSaleVO.setType("打折");
            itemSaleVO.setDesc("满"+ ladderEntity.getFullCount() +"件，打"+ ladderEntity.getDiscount().divide(new BigDecimal(10)) +"折");
            itemSaleVOS.add(itemSaleVO);
        }

        //查询打折信息
        SkuFullReductionEntity skuFullReductionEntity = this.fullReductionDao.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if (skuFullReductionEntity != null){
            ItemSaleVO itemSaleVO = new ItemSaleVO();

            itemSaleVO.setType("满减");
            itemSaleVO.setDesc("满"+skuFullReductionEntity.getFullPrice()+"件，减"+skuFullReductionEntity.getReducePrice());
            itemSaleVOS.add(itemSaleVO);
        }
        return itemSaleVOS;
    }

}