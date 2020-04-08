/**
 * Copyright (C) 2018-2019
 * All rights reserved, Designed By www.yixiang.co
 * 注意：
 * 本软件为www.yixiang.co开发研制，未经购买不得使用
 * 购买后可获得全部源代码（禁止转卖、分享、上传到码云、github等开源平台）
 * 一经发现盗用、分享等行为，将追究法律责任，后果自负
 */
package co.yixiang.modules.shop.service.impl;

import cn.hutool.core.util.ObjectUtil;
import co.yixiang.exception.ErrorRequestException;
import co.yixiang.modules.shop.entity.YxStoreCouponUser;
import co.yixiang.modules.shop.mapper.YxStoreCouponUserMapper;
import co.yixiang.modules.shop.mapping.CouponMap;
import co.yixiang.modules.shop.service.YxStoreCouponService;
import co.yixiang.modules.shop.service.YxStoreCouponUserService;
import co.yixiang.modules.shop.web.param.YxStoreCouponUserQueryParam;
import co.yixiang.modules.shop.web.vo.YxStoreCouponQueryVo;
import co.yixiang.modules.shop.web.vo.YxStoreCouponUserQueryVo;
import co.yixiang.common.service.impl.BaseServiceImpl;
import co.yixiang.common.web.vo.Paging;
import co.yixiang.utils.OrderUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * 优惠券发放记录表 服务实现类
 * </p>
 *
 * @author hupeng
 * @since 2019-10-27
 */
@Slf4j
@Service
@AllArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class YxStoreCouponUserServiceImpl extends BaseServiceImpl<YxStoreCouponUserMapper, YxStoreCouponUser> implements YxStoreCouponUserService {

    private final YxStoreCouponUserMapper yxStoreCouponUserMapper;

    private final YxStoreCouponService storeCouponService;

    private final CouponMap couponMap;

    @Override
    public int getUserValidCouponCount(int uid) {
        checkInvalidCoupon(uid);
        QueryWrapper<YxStoreCouponUser> wrapper= new QueryWrapper<>();
        wrapper.eq("status",0).eq("uid",uid);
        return yxStoreCouponUserMapper.selectCount(wrapper);
    }

    @Override
    public List<YxStoreCouponUser> beUsableCouponList(int uid, double price) {
        QueryWrapper<YxStoreCouponUser> wrapper= new QueryWrapper<>();
        wrapper.eq("is_fail",0).eq("status",0).le("use_min_price",price).eq("uid",uid);
        return yxStoreCouponUserMapper.selectList(wrapper);
    }

    /**
     * 获取可用优惠券
     * @param uid
     * @param price
     * @return
     */
    @Override
    public YxStoreCouponUser beUsableCoupon(int uid, double price) {
        QueryWrapper<YxStoreCouponUser> wrapper= new QueryWrapper<>();
        wrapper.eq("is_fail",0).eq("status",0).eq("uid",uid)
                .le("use_min_price",price).last("limit 1") ;
        return getOne(wrapper);
    }

    @Override
    public YxStoreCouponUser getCoupon(int id, int uid) {
        QueryWrapper<YxStoreCouponUser> wrapper= new QueryWrapper<>();
        wrapper.eq("is_fail",0).eq("status",0).eq("uid",uid)
                .eq("id",id) ;
        return getOne(wrapper);
    }

    @Override
    public void useCoupon(int id) {
        YxStoreCouponUser couponUser = new YxStoreCouponUser();
        couponUser.setId(id);
        couponUser.setStatus(1);
        couponUser.setUseTime(OrderUtil.getSecondTimestampTwo());
        yxStoreCouponUserMapper.updateById(couponUser);
    }

    /**
     * 检查优惠券状态
     * @param uid
     */
    @Override
    public void checkInvalidCoupon(int uid) {
        int nowTime = OrderUtil.getSecondTimestampTwo();
        QueryWrapper<YxStoreCouponUser> wrapper= new QueryWrapper<>();
        wrapper.lt("end_time",nowTime).eq("status",0);
        YxStoreCouponUser couponUser = new YxStoreCouponUser();
        couponUser.setStatus(2);
        yxStoreCouponUserMapper.update(couponUser,wrapper);

    }

    /**
     * 获取用户优惠券
     * @param uid uid
     * @param type type
     * @return
     */
    @Override
    public List<YxStoreCouponUserQueryVo> getUserCoupon(int uid, int type) {

        checkInvalidCoupon(uid);
        QueryWrapper<YxStoreCouponUser> wrapper= new QueryWrapper<>();
        wrapper.eq("uid",uid);//默认获取所有
        if(type == 1){//获取用户优惠券（未使用）
            wrapper.eq("status",0);
        }else if(type == 2){//获取用户优惠券（已使用）
            wrapper.eq("status",1);
        }else if(type > 2){//获取用户优惠券（已过期）
            wrapper.eq("status",2);
        }
        List<YxStoreCouponUser> storeCouponUsers = yxStoreCouponUserMapper.selectList(wrapper);

        List<YxStoreCouponUserQueryVo> storeCouponUserQueryVoList = new ArrayList<>();
        int nowTime = OrderUtil.getSecondTimestampTwo();
        for (YxStoreCouponUser couponUser : storeCouponUsers) {
            YxStoreCouponUserQueryVo queryVo = couponMap.toDto(couponUser);
            if(couponUser.getIsFail() == 1){
                queryVo.set_type(0);
                queryVo.set_msg("已失效");
            }else if (couponUser.getStatus() == 1){
                queryVo.set_type(0);
                queryVo.set_msg("已使用");
            }else if (couponUser.getStatus() == 2){
                queryVo.set_type(0);
                queryVo.set_msg("已过期");
            }else if(couponUser.getAddTime() > nowTime || couponUser.getEndTime() < nowTime){
                queryVo.set_type(0);
                queryVo.set_msg("已过期");
            }else{
                if(couponUser.getAddTime()+ 3600*24 > nowTime){
                    queryVo.set_type(2);
                    queryVo.set_msg("可使用");
                }else{
                    queryVo.set_type(1);
                    queryVo.set_msg("可使用");
                }
            }

            storeCouponUserQueryVoList.add(queryVo);

        }
        return storeCouponUserQueryVoList;
    }

    @Override
    public void addUserCoupon(int uid, int cid) {
        YxStoreCouponQueryVo storeCouponQueryVo = storeCouponService.
                getYxStoreCouponById(cid);
        if(ObjectUtil.isNull(storeCouponQueryVo)) throw new ErrorRequestException("优惠劵不存在");

        YxStoreCouponUser couponUser = new YxStoreCouponUser();
        couponUser.setCid(cid);
        couponUser.setUid(uid);
        couponUser.setCouponTitle(storeCouponQueryVo.getTitle());
        couponUser.setCouponPrice(storeCouponQueryVo.getCouponPrice());
        couponUser.setUseMinPrice(storeCouponQueryVo.getUseMinPrice());
        int addTime = OrderUtil.getSecondTimestampTwo();
        couponUser.setAddTime(addTime);
        int endTime = addTime + storeCouponQueryVo.getCouponTime() * 86400;
        couponUser.setEndTime(endTime);
        couponUser.setType("get");

        save(couponUser);

    }

    @Override
    public YxStoreCouponUserQueryVo getYxStoreCouponUserById(Serializable id) throws Exception{
        return yxStoreCouponUserMapper.getYxStoreCouponUserById(id);
    }

    @Override
    public Paging<YxStoreCouponUserQueryVo> getYxStoreCouponUserPageList(YxStoreCouponUserQueryParam yxStoreCouponUserQueryParam) throws Exception{
        Page page = setPageParam(yxStoreCouponUserQueryParam,OrderItem.desc("create_time"));
        IPage<YxStoreCouponUserQueryVo> iPage = yxStoreCouponUserMapper.getYxStoreCouponUserPageList(page,yxStoreCouponUserQueryParam);
        return new Paging(iPage);
    }

}
