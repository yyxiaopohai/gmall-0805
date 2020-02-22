package com.example.gmall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.example.gmall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2016092200568607";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC1D8aVl4/QJAf1wk9lD6qOrqodeh8gM9Vk4Dh7j77vVgSw62oLYhPWen1X5xBIBP9SD5aAjn4oOEwZwmfn2lWAFttf7lGcuFaHVH5sQUMuQlUWQeGUZKkLsIRp/QXHLIPOGxTiABHANZI2k+tr3UgL5jwgZZX4OacEvNKM13i8cczkemzq4PR2K0CrFH1e+eYWfrW1jlCjUNL6uWzTE8hnUmgmayFGjpDkbrOnfoohhlWaX/U9bFbbGSlvQ2YWHfoLcsuErf72LdKXSNGmJQzi8OV2TbijuDT3EaiLwkYBrEwXzke43YMUl7JCV5KINtDDUN4b5j7QdBT0y492bySrAgMBAAECggEBAI/eoDH/g2OlLboHEyU/hIvpl/ziK9dfuIF2dQshllsA46tc9B9laFP1cqGlVdeUOPiQsEsobnAxA684PW4KnAOypdAdQzYfd4oDBo2oceRRCuErqj5KmdRTB3Rce/nekkD0XeySl4g+SRcbBGbqJrRl1oL7lWXvEZp/G2KUWDiLcanDZnkfDU9bT6/1Bwk81UYFEp9ii9MW10yixbdFcBEM2cE9GJg8v07Mw+furE1EEgO6u5e2OQ0SAqhJkJ6a7iDN2bBCqJjjO5KC6DqWoinOUHwZgjVFNCCi3MHzM0ao8lM03SHi6KpFbsr7rHFaW1EMyXxJNoo7FdxObUrmSJkCgYEA5Vd7QBs38294334K2fmx6EuVHXHuKcyXtO01OvCuqz1nv6uCH/ZnZWZVNYLOXctWfa2ktnVLpt+jiJDZ1qBb4nKgZi1OJN/1YksJJ43DIrVumNOAUVQEgBjLHVLKWXCNU/YUFlQ+5QZm/TW21xlzWXHOD6o3Yie4KqetihCA/XcCgYEAyhufvWXJcojGkb/dZPib62SPWAyeqoKlij8UOgPjB8RXygB1Zs/UTuobvaUEZIlZw/8N9wfeiL8PtiTLHlyqDedor1sZWjlmbOq6WBqes8AvVzVMfXlDvjMYllaoA3xjoEH67MvtyWJRKsuEq2vR4aJq8k5LWnjGiIe5aFXwz20CgYAV7k6n67JnAgLiBFCPVZRQrWGeBjYLIzhiochUUY2ybFXOe10fJ1Xt2WJOkqqfR4GG1SMKG59XXs1pkshyALWoquNxweVWG2CAvMIsWR8O46Cm1prPrCQeB8Dsle0prlylacOz+uxkxD72+br5SMLCC+LzlohjR38R/STYLgnXswKBgDxo911AQG9orJnCFKXqnYhFnYb+9qoWtO5WnnAc205mBKuPaYgalOYlmbcczOxUFa283eq5wm1zVnnmE4R2xCm1wpevjitmvtPNcXeGNWubBUt21e8nQPVoBnBhZITuOEUdk6gV6HRqW8KD5JrsdmaQCpF0j5dyRD1o/CVQktMdAoGAAwojnNCauuvgA8XjEmjDlnb+IMBSWMZ5f567BXcp3JwtygQqiSMpjZmAS6SZgaRqLJ+IakP4RSwFu6LEE+rfJyTlj0oPF5aR+8UjKHyUHBo3EbOj1FxV/3TGu9560KZAXxnhh+09JU1W8ISpM2X6d0UWYyxAMQ+KbIkGL76B7MI=";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgQcJJ3zkLFmgV5VUXxe2mgtHF9j7uIXmvhrYjGXiIQKWmGgWfiPtiXirvJpwdaiPvL0i3HejOwMGUduH0h9wr+M8E9Wabc0lzze/LSVvsuN9EpSei4jbV3NhroKxkT4SOF9qPGmWWn8Ej9cZ6/LkTcwBaD3jzTJ7MRIwfSSwNuFX0oNsGn1i+tT7n9FPouo5F0A+1Do3y/wO0YMQWxo7xOhZ+OJcc7UGWxNbGagPDNHOt3REIRuhL6l1f/o6a/NjzURx25MRrt74gRb9DVdhdSl3BA6uZX7wIw/0OPQ/6K/+nj4Q99QyK/uTS0c8gHXoLbXKmaWM2CqvR7R8k1BAaQIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url;

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url;

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
