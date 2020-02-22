package com.example.gmall.order.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.gmall.order.config.AlipayTemplate;
import com.example.gmall.order.interceptor.LoginInterceptor;
import com.example.gmall.order.service.OrderService;
import com.example.gmall.order.vo.OrderConfirmVO;
import com.example.gmall.order.vo.PayAsyncVo;
import com.example.gmall.order.vo.PayVo;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private AlipayTemplate alipayTemplate;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("confirm")
    public Resp<OrderConfirmVO> confirm(){
        OrderConfirmVO orderConfirmVO = orderService.confirm();
        return Resp.ok(orderConfirmVO);

    }

    @PostMapping("submit")
    public Resp<Object> submit(@RequestBody OrderSubmitVO orderSubmitVO){
        OrderEntity orderEntity = orderService.submit(orderSubmitVO);
        if (orderEntity != null) {
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderEntity.getOrderSn());
            payVo.setTotal_amount(orderEntity.getTotalAmount().toString());
            payVo.setSubject("谷粒商城");
            payVo.setBody("谷粒商城支付平台");
            try {
                String pay = alipayTemplate.pay(payVo);
                System.out.println(pay);
            } catch (AlipayApiException e) {
                e.printStackTrace();
            }
        }
        return Resp.ok(null);
    }

    @PostMapping("pay/success")
    public Resp<Object> paySuccess(PayAsyncVo payAsyncVo){

        amqpTemplate.convertAndSend("ORDER-EXCHANGE","order.pay",payAsyncVo.getOut_trade_no());
        return Resp.ok(null);
    }

    @PostMapping("seckill/{skuId}")
    public Resp<Object> seckill(@PathVariable("skuId") Long skuId) {

        String num = stringRedisTemplate.opsForValue().get("seckill:num:" + skuId);
        RSemaphore semaphore = redissonClient.getSemaphore("seckill" + skuId);

        semaphore.trySetPermits(Integer.valueOf(num));
        try {
            semaphore.acquire(1);

            stringRedisTemplate.opsForValue().decrement("seckill:num:" + skuId);

            UserInfo userInfo = LoginInterceptor.getUserInfo();
            String timeId = IdWorker.getTimeId();
            SkuLockVO skuLockVO = new SkuLockVO();
            skuLockVO.setOrderToken(timeId);
            skuLockVO.setCount(1);
            amqpTemplate.convertAndSend("ORDER-EXCHANGE","order.seckill",skuLockVO);

            RCountDownLatch countDownLatch = redissonClient.getCountDownLatch("seckill:latch:" + userInfo.getUserId());
            countDownLatch.trySetCount(1);

            semaphore.release();
            return Resp.ok(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Resp.fail("秒杀失败");
        }
    }

    @GetMapping
    public Resp<OrderEntity> querySeckill() throws InterruptedException {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        RCountDownLatch countDownLatch = redissonClient.getCountDownLatch("seckill:latch:" + userInfo.getUserId());
        countDownLatch.await();

        return Resp.ok(null);
    }
}
