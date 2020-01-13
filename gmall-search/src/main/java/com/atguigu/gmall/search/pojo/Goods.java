package com.atguigu.gmall.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

@Document(indexName = "goods",type = "info",shards = 3,replicas = 2)
@Data
public class Goods {
    @Id
    private Long skuId; //sku的id
    @Field(type = FieldType.Text,analyzer = "ik_max_word")
    private String skuTitle; //sku的标题信息
    @Field(type = FieldType.Keyword,index = false)
    private String skuSubTitle; //sku的副标题
    @Field(type = FieldType.Double)
    private Double price; //价格
    @Field(type = FieldType.Keyword,index = false)
    private String defaultImages; //默认图片

    @Field(type = FieldType.Long)
    private Long sale; //销量
    @Field(type = FieldType.Date)
    private Date creatTime; //创建时间
    @Field(type = FieldType.Boolean)
    private boolean store; //库存

    @Field(type = FieldType.Long)
    private Long brandId; //品牌的id
    @Field(type = FieldType.Keyword)
    private String brandName; //品牌的名称
    @Field(type = FieldType.Long)
    private Long categoryId; //分类的id
    @Field(type = FieldType.Keyword)
    private String categoryName; //分类的名称

    @Field(type = FieldType.Nested)//nested--嵌套
    private List<SearchAttrValue> attrs; //这里封装的是分类中的子分类的信息

}
