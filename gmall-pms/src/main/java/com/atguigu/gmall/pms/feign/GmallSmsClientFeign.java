package com.atguigu.gmall.pms.feign;

import com.atguigu.core.bean.Resp;
import com.atguigu.sms.api.GmallSmsApiFeign;
import com.atguigu.sms.vo.SaleVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("sms-service")
public interface GmallSmsClientFeign extends GmallSmsApiFeign {

}
