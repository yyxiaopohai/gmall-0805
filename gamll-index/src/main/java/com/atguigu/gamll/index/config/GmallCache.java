package com.atguigu.gamll.index.config;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

//可以放在方法上
@Target({ElementType.METHOD})
//是一个运行时注解
@Retention(RetentionPolicy.RUNTIME)
//可以在文档中出现
@Documented
public @interface GmallCache {

    /**
     * 自定义默认缓存的key值
     * @return
     */
    String value() default  "";

    /**
     * 自定义过期时间
     * 单位是分钟
     * @return
     */
    int timeout() default 30;

    /**
     * 随机数的范围
     * @return
     */
    int brand() default 5;

    /**
     * 自定义锁的名称
     * @return
     */
    String lockName() default "lock";
}
