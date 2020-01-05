package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.dao.*;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClientFeign;
import com.atguigu.gmall.pms.service.ProductAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SkuSaleAttrValueService;
import com.atguigu.gmall.pms.vo.BaseAttrValueVO;
import com.atguigu.gmall.pms.vo.SkuInfoVO;
import com.atguigu.gmall.pms.vo.SpuInfoVO;
import com.atguigu.sms.vo.SaleVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.service.SpuInfoService;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Resource
    private GmallSmsClientFeign smsClientFeign;
    @Resource
    private SpuInfoDescDao descDao;
    @Resource
    private ProductAttrValueService attrValueService;
    @Resource
    private SkuInfoDao skuInfoDao;
    @Resource
    private SkuImagesService skuImagesService;
    @Resource
    private SkuSaleAttrValueService saleAttrValueService;
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo querySpuByCidOrKey(QueryCondition queryCondition, long catId) {

        QueryWrapper<SpuInfoEntity> queryWrapper = new QueryWrapper<>();
        if (catId != 0l){
            queryWrapper.eq("catalog_id",catId);
        }
        String key = queryCondition.getKey();
        if (StringUtils.isNotBlank(key)){
            queryWrapper.and(t->t.like("spu_name",key).or().eq("id",key));
        }
        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(queryCondition),queryWrapper);
        return new PageVo(page);
    }

    @Override
    public void bigSave(SpuInfoVO spuInfoVO) {

        // TODO: 2020/1/4 这里是保存共9张表的信息
        //1.保存spu信息
        //1.1保存spuInfo
        spuInfoVO.setCreateTime(new Date());
        spuInfoVO.setUodateTime(spuInfoVO.getCreateTime());
        this.save(spuInfoVO);
        Long spuId = spuInfoVO.getId();
        //1.2保存spudesc信息
        List<String> spuImages = spuInfoVO.getSpuImages();
        if (!CollectionUtils.isEmpty(spuImages)){
            SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
            descEntity.setSpuId(spuId);
            descEntity.setDecript(StringUtils.join(spuImages,","));
            descDao.insert(descEntity);
        }
        //1.3保存基础属性相关信息ProductAttr
        List<BaseAttrValueVO> baseAttrs = spuInfoVO.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){
            List<ProductAttrValueEntity> attrValueEntuties = baseAttrs.stream().map(baseAttr -> {
                ProductAttrValueEntity attrValueEntity = new ProductAttrValueEntity();
                BeanUtils.copyProperties(baseAttr, attrValueEntity);
                attrValueEntity.setSpuId(spuId);
                attrValueEntity.setAttrSort(0);
                attrValueEntity.setQuickShow(0);
                return attrValueEntity;
            }).collect(Collectors.toList());
            attrValueService.saveBatch(attrValueEntuties);
        }

        //2.保存skuInfo信息
        List<SkuInfoVO> skus = spuInfoVO.getSkus();
        if (CollectionUtils.isEmpty(skus)){
            return;
        }
        skus.forEach(sku->{
            SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
            BeanUtils.copyProperties(sku,skuInfoEntity);
            skuInfoEntity.setSpuId(spuId);
            List<String> images = sku.getImages();
            //这里判断图片是否为空，如果没有传图片使用默认图片，如果传了图片用传了的图片
            if (!CollectionUtils.isEmpty(images)){
                skuInfoEntity.setSkuDefaultImg(skuInfoEntity.getSkuDefaultImg()==null?images.get(0):skuInfoEntity.getSkuDefaultImg());
            }
            skuInfoEntity.setSkuCode(null);
            skuInfoEntity.setCatalogId(spuInfoVO.getCatalogId());
            skuInfoEntity.setBrandId(spuInfoVO.getBrandId());
            skuInfoDao.insert(skuInfoEntity);
            Long skuId = skuInfoEntity.getSkuId();

            //保存图片信息
            if (!CollectionUtils.isEmpty(images)){
                List<SkuImagesEntity> imageList = images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgSort(0);
                    skuImagesEntity.setImgUrl(image);
                    skuImagesEntity.setDefaultImg(StringUtils.equals(image, skuInfoEntity.getSkuDefaultImg()) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(imageList);
            }
            //保存pms_sku_sale_attr_value
            List<SkuSaleAttrValueEntity> saleAttrs = sku.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)){
                saleAttrs.forEach(skuSaleAttrValueEntity -> {
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    skuSaleAttrValueEntity.setAttrSort(0);
                });
                saleAttrValueService.saveBatch(saleAttrs);
            }

            //3.保存营销相关信息
            SaleVO saleVO = new SaleVO();
            BeanUtils.copyProperties(sku,saleVO);
            saleVO.setSkuId(skuId);
            smsClientFeign.saveSales(saleVO);
        });

    }

}