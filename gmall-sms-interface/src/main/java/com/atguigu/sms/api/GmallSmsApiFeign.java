package com.atguigu.sms.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.sms.vo.SaleVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface GmallSmsApiFeign {
    @PostMapping("sms/skubounds/sales")
    public Resp<Object> saveSales(@RequestBody SaleVO saleVO);
}
