package com.atguigu.sms.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.sms.vo.ItemSaleVO;
import com.atguigu.sms.vo.SaleVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface GmallSmsApiFeign {
    @PostMapping("sms/skubounds/sales")
    public Resp<Object> saveSales(@RequestBody SaleVO saleVO);

    @GetMapping("sms/skubounds/{skuId}")
    public Resp<List<ItemSaleVO>> queryItemSaleVOByskuId(@PathVariable("skuId")Long skuId);
}
