package com.atguigu.sms.vo;

import lombok.Data;

@Data
public class ItemSaleVO {

    private String type;//一共三种类型：积分，满减，打折
    private String desc;//各种的描述信息
}
