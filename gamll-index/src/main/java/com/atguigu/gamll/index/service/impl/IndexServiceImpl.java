package com.atguigu.gamll.index.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.gamll.index.feign.GmallPmsClient;
import com.atguigu.gamll.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexServiceImpl implements IndexService {
    @Autowired
    private GmallPmsClient pmsClient;

    @Override
    public List<CategoryEntity> queryLvl1Categories() {
        Resp<List<CategoryEntity>> category = pmsClient.queryCategory(1, null);
        List<CategoryEntity> categoryEntities = category.getData();
        return categoryEntities;
    }

    @Override
    public List<CategoryVo> queryCategoriesWithSub(Long pid) {
        Resp<List<CategoryVo>> categoriesWithSub = pmsClient.queryCategoriesWithSub(pid);
        List<CategoryVo> categoryVos = categoriesWithSub.getData();
        return categoryVos;
    }
}
