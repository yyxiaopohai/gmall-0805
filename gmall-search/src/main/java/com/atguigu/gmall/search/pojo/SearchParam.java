package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParam {

    private String key;//搜索的数据

    private Long[] catelog3;//三级分类的id
    private Long[] brand;//品牌id

    private Double priceFrom;//最低价格
    private Double priceTo;//最高价格

    private List<String> props;//规格参数

    private String order;//排序

    private Integer pageNum = 1;//分页
    private Integer pageSize = 64;//每页的个数

    private Boolean store;//是否有货
}
