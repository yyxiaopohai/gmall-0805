package com.atguigu.gamll.index.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;

import java.util.List;

public interface IndexService {

    List<CategoryEntity> queryLvl1Categories();

    List<CategoryVo> queryCategoriesWithSub(Long pid);

    void testLock();

    String testRead();

    String testwrite();

    String door();

    String down();
}
