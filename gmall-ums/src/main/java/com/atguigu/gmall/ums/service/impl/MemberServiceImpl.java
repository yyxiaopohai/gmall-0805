package com.atguigu.gmall.ums.service.impl;

import com.atguigu.core.exception.UmsException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.ums.dao.MemberDao;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;

import javax.annotation.Resource;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Resource
    private MemberDao memberDao;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        switch (type){
            case 1: wrapper.eq("username",data); break;
            case 2: wrapper.eq("mobile",data); break;
            case 3: wrapper.eq("email",data); break;
            default: return null;
        }
        return memberDao.selectCount(wrapper) ==0;

    }

    @Override
    public void register(MemberEntity memberEntity, String code) {
        //查询验证码

        String redisCode = stringRedisTemplate.opsForValue().get(memberEntity.getMobile());

        if (!StringUtils.equals(redisCode,code)){
            throw new UmsException("验证码错误");
        }
        //生成盐
        String salt = UUID.randomUUID().toString().substring(0, 6);
        memberEntity.setSalt(salt);

        //加盐加密
        String password = DigestUtils.md5Hex(memberEntity.getPassword() + salt);
        memberEntity.setPassword(password);

        //保存用户信息
        memberEntity.setLevelId(1l);
        memberEntity.setSourceType(1);
        memberEntity.setIntegration(1000);
        memberEntity.setGrowth(100);
        memberEntity.setStatus(1);
        memberEntity.setCreateTime(new Date());
        this.save(memberEntity);

        //删除验证码
        stringRedisTemplate.delete(memberEntity.getMobile());
    }

    @Override
    public MemberEntity queryUser(String username, String password) {
        //先通过用户名进行查询
        MemberEntity memberEntity = this.getOne(new QueryWrapper<MemberEntity>().eq("username", username));
        //判断是否有
        //没有：直接返回
        if (memberEntity == null){
            throw new UmsException("用户名错误");
        }

        //有：将盐查出来
        String salt = memberEntity.getSalt();

        //对密码进行加盐加密
        password = DigestUtils.md5Hex(password + salt);

        //与数据库中的密码（从数据库中查询）进行比较
        if (!StringUtils.equals(password,memberEntity.getPassword())){
            throw new UmsException("用户名密码错误");
        }

        return memberEntity;
    }

}