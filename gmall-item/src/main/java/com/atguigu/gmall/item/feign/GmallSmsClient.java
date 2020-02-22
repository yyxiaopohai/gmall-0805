package com.atguigu.gmall.item.feign;

import com.atguigu.sms.api.GmallSmsApiFeign;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApiFeign {
}
