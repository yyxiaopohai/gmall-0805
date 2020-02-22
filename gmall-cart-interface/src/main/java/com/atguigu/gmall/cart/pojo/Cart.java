package com.atguigu.gmall.cart.pojo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.sms.vo.ItemSaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Cart {

    private Long skuId;
    private String skuTitle;
    private String image;
    private List<SkuSaleAttrValueEntity> saleAttrs;
    private BigDecimal price;//本来的价格
    private BigDecimal currentPrice;//变动的价格(减价)
    private Integer count;
    private Boolean store = false;
    private Boolean check;
    private List<ItemSaleVO> sales;


}
