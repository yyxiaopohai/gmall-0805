package com.atguigu.gmall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParam;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVO;
import com.atguigu.gmall.search.pojo.SearchResponseVO;
import com.atguigu.gmall.search.service.SearchService;
import com.sun.xml.internal.bind.v2.TODO;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Resource
    private RestHighLevelClient highLevelClient;

    @Override
    public SearchResponseVO search(SearchParam searchParam) throws IOException {
        SearchResponse searchResponse = highLevelClient.search(new SearchRequest(new String[]{"goods"}, buildDSL(searchParam)), RequestOptions.DEFAULT);

        SearchResponseVO searchResponseVO = parseSearchResult(searchResponse);
        searchResponseVO.setPageNum(searchParam.getPageNum());
        searchResponseVO.setPageSize(searchParam.getPageSize());
        return searchResponseVO;
    }

    //返回结果集的封装
        private SearchResponseVO parseSearchResult(SearchResponse searchResponse){

        SearchResponseVO searchResponseVO = new SearchResponseVO();
        SearchHits hits = searchResponse.getHits();
        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = new ArrayList<>();
        for (SearchHit hitsHit : hitsHits) {

            String goodsJson = hitsHit.getSourceAsString();
            Goods goods = JSON.parseObject(goodsJson, Goods.class);

            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField skuTitle = highlightFields.get("skuTitle");
            goods.setSkuTitle(skuTitle.getFragments()[0].string());

            goodsList.add(goods);
        }
        //解析查询结果集
        searchResponseVO.setProducts(goodsList);

        //解析品牌
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        ParsedLongTerms attrIdAgg = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        SearchResponseAttrVO responseAttrVO = new SearchResponseAttrVO();
        responseAttrVO.setProductAttributeId(null);
        responseAttrVO.setName("品牌");

        //获取聚合中的桶
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)){

            List<String> brandValues = buckets.stream().map(bucket -> {

                Map<String,Object> map = new HashMap<>();
                map.put("id",((Terms.Bucket) bucket).getKeyAsNumber());
                Map<String, Aggregation> stringAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                ParsedStringTerms attrNameAgg = (ParsedStringTerms)stringAggregationMap.get("brandNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                map.put("name",nameAggBuckets.get(0).getKeyAsString());

                return JSON.toJSONString(map);
            }).collect(Collectors.toList());
            responseAttrVO.setValue(brandValues);


            searchResponseVO.setBrand(responseAttrVO);
        }

        //解析分类
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
        SearchResponseAttrVO categoryVO = new SearchResponseAttrVO();
        categoryVO.setProductAttributeId(null);
        categoryVO.setName("分类");
        //获取桶
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryBuckets)){
            List<String> categoryValues = categoryBuckets.stream().map(categoryBucket -> {

                Map<String,Object> map = new HashMap<>();
                map.put("id",((Terms.Bucket) categoryBucket).getKeyAsNumber());
                Map<String, Aggregation> categoryAgg = ((Terms.Bucket) categoryBucket).getAggregations().asMap();
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms)categoryAgg.get("categoryNameAgg");
                List<? extends Terms.Bucket> aggBuckets = categoryNameAgg.getBuckets();
                map.put("name",aggBuckets.get(0).getKeyAsString());
                return JSON.toJSONString(map);
            }).collect(Collectors.toList());
            categoryVO.setValue(categoryValues);
            searchResponseVO.setCatelog(categoryVO);
        }

        //解析attr
        ParsedNested attrsAgg = (ParsedNested)aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAggs = (ParsedLongTerms)attrsAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggsBuckets = attrIdAggs.getBuckets();
        attrIdAggsBuckets.stream().map(attrIdAggsBucket -> {
            SearchResponseAttrVO attrVO = new SearchResponseAttrVO();
            attrVO.setProductAttributeId(((Terms.Bucket) attrIdAggsBucket).getKeyAsNumber().longValue());
            ParsedStringTerms attrNameAgg = (ParsedStringTerms)((Terms.Bucket) attrIdAggsBucket).getAggregations().get("attrNameAgg");
            attrVO.setName(attrNameAgg.getBuckets().get(0).getKeyAsString());
            ParsedStringTerms valueAgg = (ParsedStringTerms)((Terms.Bucket) attrIdAggsBucket).getAggregations().get("attrValueAgg");
            List<? extends Terms.Bucket> valueAggBuckets = valueAgg.getBuckets();
            //将valueAggBuckets这个集合转换成 attrVO.setValue();需要的子集合
            List<String> attrList = valueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            attrVO.setValue(attrList);
            return attrVO;
        }).collect(Collectors.toList());
        searchResponseVO.setAttrs(null);

        searchResponseVO.setTotal(searchResponse.getHits().getTotalHits());
        return searchResponseVO;
    }

    //查询条件的封装
    private SearchSourceBuilder buildDSL(SearchParam searchParam){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.fetchSource(new String[]{"skuId","skuTitle","skuSubTitle","price","defaultImage"},null);
        String key = searchParam.getKey();
        if (StringUtils.isEmpty(key)){
            return sourceBuilder;
        }
        //1. 构建查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //1.1. 构建匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle",key).operator(Operator.AND));
        //1.2. 构建过滤条件
        Long[] brandIds = searchParam.getBrand();
        if (brandIds != null && brandIds.length != 0){
            //1.2.1. 品牌过滤
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandIds));
        }
        //1.2.2. 分类过滤
        Long[] categoryIds = searchParam.getCatelog3();
        if (categoryIds != null && categoryIds.length != 0){
            boolQueryBuilder.filter(QueryBuilders.termQuery("categoryId",categoryIds));
        }
        //1.2.3. 价格区间过滤
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
        Double priceFrom = searchParam.getPriceFrom();
        if (priceFrom != null){
            rangeQueryBuilder.gte(priceFrom);
        }
        Double priceTo = searchParam.getPriceTo();
        if (priceTo != null){
            rangeQueryBuilder.lte(priceTo);
        }
        boolQueryBuilder.filter(rangeQueryBuilder);

        //1.2.4. 规格参数过滤
        List<String> props = searchParam.getProps();
        //判断是否为空
        if (!CollectionUtils.isEmpty(props)){
            props.forEach(prop ->{
                String[] attr = StringUtils.split(prop, ":");
                if (attr != null && attr.length == 2){
                    String attrId = attr[0];
                    String[] attrValues = StringUtils.split(attr[1], "-");
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                    boolQuery.must(QueryBuilders.termsQuery("attrs.attrValue",attrValues));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs",boolQuery, ScoreMode.None));
                }
            });
        }
        sourceBuilder.query(boolQueryBuilder);
        //2. 构建排序
        String order = searchParam.getOrder();
        if (StringUtils.isNotBlank(order)){
            String[] orders = StringUtils.split(order, ":");
            if (orders != null && orders.length == 2){
                String orderFiled = orders[0];
                String orderBy = orders[1];
                switch (orderFiled){
                    case "0": orderFiled = "_score"; break;
                    case "1": orderFiled = "sale"; break;
                    case "2": orderFiled = "price"; break;
                    default: orderFiled = "_score"; break;
                }

                sourceBuilder.sort(orderFiled,StringUtils.equals(orderBy,"asc")? SortOrder.ASC:SortOrder.DESC);
            }
        }
        //3. 构建分页
        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        sourceBuilder.from((pageNum-1)*pageSize);
        sourceBuilder.size(pageSize);

        //4. 构架高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("skuTitle");
        highlightBuilder.preTags("<span style='color:red;'>");
        highlightBuilder.postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);

        //5. 构建聚合
        //5.1. 构建品牌的聚合
        sourceBuilder.aggregation(
                AggregationBuilders.terms("brandIdAgg").field("brandId").subAggregation(
                        AggregationBuilders.terms("brandNameAgg").field("brandName")
                )
        );
        //5.2. 构建分类的聚合
        sourceBuilder.aggregation(
                AggregationBuilders.terms("categoryIdAgg").field("categoryId").subAggregation(
                        AggregationBuilders.terms("categoryNameAgg").field("categoryName")
                )
        );

        //5.3. 构建规格参数的聚合
        sourceBuilder.aggregation(
                AggregationBuilders.nested("attrAgg","attrs").subAggregation(
                        AggregationBuilders.terms("attrIdAgg").field("attrs.attrId").subAggregation(
                                AggregationBuilders.terms("attrNameAgg").field("attrs.attrName")
                        ).subAggregation(
                                AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")
                        )
                )
        );
        System.out.println(sourceBuilder);
        return sourceBuilder;
    }
}
