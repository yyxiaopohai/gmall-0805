package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.atguigu.sms.vo.ItemSaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ItemVO {

    private Long skuId;//suk的id
    private String skuTitle;//标题
    private String skuSubTitle;//副标题
    private BigDecimal price;//价格
    private BigDecimal wight;//重量
    private boolean store;//库存

    private Long categoryId;//分类的id
    private String categoryName;//分类的名字
    private Long brandId;//分组的id
    private String brandName;//分组的名字
    private Long spuId;//spu的id
    private String spuName;//spu的名字

    private List<SkuImagesEntity> images;//图片信息
    private List<ItemSaleVO> sales;//促销信息:积分，打折，满减

    private List<SkuSaleAttrValueEntity> saleAttrValues;//spu下的所有水库组合

    private List<String> desc;//spu的描述信息

    private List<ItemGroupVO> groupVOS;

}
