package com.atguigu.gamll.index.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gamll.index.config.GmallCache;
import com.atguigu.gamll.index.feign.GmallPmsClient;
import com.atguigu.gamll.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexServiceImpl implements IndexService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";
    @Override
    public List<CategoryEntity> queryLvl1Categories() {
        Resp<List<CategoryEntity>> category = pmsClient.queryCategory(1, null);
        List<CategoryEntity> categoryEntities = category.getData();
        return categoryEntities;
    }

    @Override
    @GmallCache(value = "index:cates:",timeout = 7000,brand = 100,lockName = "lock")
    public List<CategoryVo> queryCategoriesWithSub(Long pid) {
//        //查询缓存
//        String cateJson = stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
//        //判断有没有
//        //有--直接返回
//        if (StringUtils.isNotBlank(cateJson)){
//            List<CategoryVo> categoryVos = JSON.parseArray(cateJson, CategoryVo.class);
//            return categoryVos;
//        }
//
//        RLock lock = redissonClient.getLock("lock" + pid);
//        lock.lock();
//
//        String cateJson2 = stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
//        if (StringUtils.isNotBlank(cateJson2)){
//            //第二次判断，如果有值直接返回，后面的并发都不用查询数据库
//            //不要忘了将锁释放，不然会成为死锁
//            lock.unlock();
//            List<CategoryVo> categoryVos = JSON.parseArray(cateJson2, CategoryVo.class);
//            return categoryVos;
//        }
//        //没有--查询数据库
        Resp<List<CategoryVo>> categoriesWithSub = pmsClient.queryCategoriesWithSub(pid);
        List<CategoryVo> categoryVos = categoriesWithSub.getData();
//
//        //查询完成之后放入缓存
//        stringRedisTemplate.opsForValue().set(KEY_PREFIX+pid,JSON.toJSONString(categoryVos),5+new Random().nextInt(5), TimeUnit.DAYS);
//
//        lock.unlock();
        return categoryVos;
    }

    @Override
    public void testLock() {

        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        String numString = stringRedisTemplate.opsForValue().get("num");
        if (numString == null){
            return;
        }
        Integer num = new Integer(numString);
        stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));

        lock.unlock();
    }

    @Override
    public String testRead() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("lock");
        lock.readLock().lock(10l,TimeUnit.SECONDS);

        String msg = stringRedisTemplate.opsForValue().get("msg");

//        lock.readLock().unlock();
        return "读取了数据："+msg;
    }

    @Override
    public String testwrite() {

        RReadWriteLock lock = redissonClient.getReadWriteLock("lock");
        lock.writeLock().lock(10l,TimeUnit.SECONDS);

        String uuid = UUID.randomUUID().toString();

        stringRedisTemplate.opsForValue().set("msg",uuid);


//        lock.writeLock().unlock();
        return "写了数据："+uuid;
    }

    @Override
    public String door() {
        RCountDownLatch lauch = redissonClient.getCountDownLatch("lauch");
        lauch.trySetCount(6);


        try {
            lauch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "班长锁门";
    }

    @Override
    public String down() {
        RCountDownLatch lauch = redissonClient.getCountDownLatch("lauch");
        lauch.countDown();
        return "走人";
    }


    public void testLock1() {
        //使用setnx，如果返回true，说明拿到锁
        //如果数据库有，就返回false，没有就返回true
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid);
        //如果正确执行下面的业务
        if (lock){

            String numString = stringRedisTemplate.opsForValue().get("num");
            if (numString == null){
                return;
            }
            Integer num = new Integer(numString);
            stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));
            //执行完业务之后，将锁释放（把已经存在的可以删除就可以了）
            //为了防止将别人的锁误删
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            stringRedisTemplate.execute(new DefaultRedisScript<>(script,Long.class), Arrays.asList("lock"),uuid);
//            String value = stringRedisTemplate.opsForValue().get("lock");
//            //将value取出来和上面的uuid比较，如果一样就删除
//            if (StringUtils.equals(value,uuid)){
//                stringRedisTemplate.delete("lock");
//            }

        }else {
            //否则，等一秒，重新连接
            try {
                TimeUnit.SECONDS.sleep(1);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
