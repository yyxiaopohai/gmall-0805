package com.atguigu.gmall.wms.vo;

import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Data;

@Data
public class SkuLockVO {

    private String orderToken;

    private Long skuId;

    private Integer count;

    private Boolean lock = false;//锁定状态，

    private Long wareSkuId;
}
