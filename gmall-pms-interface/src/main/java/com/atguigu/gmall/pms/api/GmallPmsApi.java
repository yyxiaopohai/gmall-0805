package com.atguigu.gmall.pms.api;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {
    @PostMapping("pms/spuinfo/page")
    public Resp<List<SpuInfoEntity>> querySpuByPage(@RequestBody QueryCondition condition);

    @GetMapping("pms/skuinfo/{spuId}")
    public Resp<List<SkuInfoEntity>> querySkuBuSpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/category/info/{catId}")
    public Resp<CategoryEntity> queryCategoryById(@PathVariable("catId") Long catId);

    @GetMapping("pms/category/{pid}")
    public Resp<List<CategoryVo>> queryCategoriesWithSub(@PathVariable("pid")Long pid);

    @GetMapping("pms/brand/info/{brandId}")
    public Resp<BrandEntity> queryBrandById(@PathVariable("brandId") Long brandId);

    @GetMapping("pms/productattrvalue/{spuId}")
    public Resp<List<ProductAttrValueEntity>> querySearchAttrValue(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/spuinfo/info/{id}")
    public Resp<SpuInfoEntity> querySpuById(@PathVariable("id") Long id);

    @GetMapping("pms/category")
    public Resp<List<CategoryEntity>> queryCategory(@RequestParam(value = "level",defaultValue = "0")Integer level,
                                                    @RequestParam(value = "parentCid",required = false)Long parentCid);
}