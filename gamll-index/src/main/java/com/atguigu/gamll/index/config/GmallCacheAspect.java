package com.atguigu.gamll.index.config;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.security.Key;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

//由spring容器发现
@Component
//是一个切面
@Aspect
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 环绕通知的四个条件
     *      1.返回值是 Object
     *      2.参数是 ProceedingJoinPoint
     *      3.抛出 Throwable
     *      4.
     */
    @Around("@annotation(com.atguigu.gamll.index.config.GmallCache)")//里面写表达式
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取方法
        Method method = signature.getMethod();
        //获取方法上的注解
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        //获取返回值的类型
        Class returnType = signature.getReturnType();
        //joinPoint.getArgs();这个方法返回值是数组，转换为集合
        List<Object> args = Arrays.asList(joinPoint.getArgs());
        //获取缓存中的数据
        String prefix = gmallCache.value();
        String key = prefix + args;

        Object cache = cache(key, returnType);
        if (cache != null) {
            return cache;
        }

        //为空，加分布式锁
        String lockName = gmallCache.lockName();
        RLock fairLock = redissonClient.getFairLock(lockName + args);
        fairLock.lock();
        //判断缓存是否为空

        Object cache1 = cache(key, returnType);
        if (cache1 != null) {

            fairLock.unlock();
            return cache1;
        }
        Object result = joinPoint.proceed(joinPoint.getArgs());//获取数据

        //将查询数据库的数据存放到缓存中
        stringRedisTemplate.opsForValue().set(key,JSON.toJSONString(result),gmallCache.timeout()+new Random().nextInt(gmallCache.brand()), TimeUnit.MINUTES);

        //释放锁
        fairLock.unlock();

        return result;
    }

    private Object cache(String key,Class returnType){
        String jsonString = stringRedisTemplate.opsForValue().get(key);

        //判断是否为空
        if (StringUtils.isNotBlank(jsonString)){
            //将字符串转换成对象，需要返回值类型
            return JSON.parseObject(jsonString,returnType);
        }
        return null;
    }
}
