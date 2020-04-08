/**
 * Copyright (C) 2018-2019
 * All rights reserved, Designed By www.yixiang.co
 * 注意：
 * 本软件为www.yixiang.co开发研制，未经购买不得使用
 * 购买后可获得全部源代码（禁止转卖、分享、上传到码云、github等开源平台）
 * 一经发现盗用、分享等行为，将追究法律责任，后果自负
 */
package co.yixiang.modules.activity.web.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import co.yixiang.annotation.AnonymousAccess;
import co.yixiang.common.api.ApiResult;
import co.yixiang.common.web.controller.BaseController;
import co.yixiang.constant.ShopConstants;
import co.yixiang.modules.activity.service.YxStoreSeckillService;
import co.yixiang.modules.activity.web.dto.SeckillConfigDTO;
import co.yixiang.modules.activity.web.dto.SeckillTimeDTO;
import co.yixiang.modules.activity.web.dto.StoreSeckillDTO;
import co.yixiang.modules.activity.web.param.YxStoreSeckillQueryParam;
import co.yixiang.modules.activity.web.vo.YxStoreSeckillQueryVo;
import co.yixiang.modules.shop.entity.YxSystemGroupData;
import co.yixiang.modules.shop.service.YxStoreProductRelationService;
import co.yixiang.modules.shop.service.YxSystemGroupDataService;
import co.yixiang.utils.OrderUtil;
import co.yixiang.utils.SecurityUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * 商品秒杀产品前端控制器
 * </p>
 *
 * @author hupeng
 * @since 2019-12-14
 */
@Slf4j
@RestController
@RequestMapping
@Api(value = "商品秒杀", tags = "营销:商品秒杀", description = "商品秒杀")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StoreSeckillController extends BaseController {

    private final YxStoreSeckillService yxStoreSeckillService;
    private final YxSystemGroupDataService yxSystemGroupDataService;
    private final YxStoreProductRelationService relationService;

    /**
     * 秒杀产品列表
     */
    @AnonymousAccess
    @GetMapping("/seckill/list/{time}")
    @ApiOperation(value = "秒杀产品列表", notes = "秒杀产品列表", response = YxStoreSeckillQueryVo.class)
    public ApiResult<Object> getYxStoreSeckillPageList(@PathVariable String time,
                                                       YxStoreSeckillQueryParam queryParam) throws Exception {
        if (StrUtil.isBlank(time)) return ApiResult.fail("参数错误");
        /** 此处代码已经废弃
        YxSystemGroupData systemGroupData = yxSystemGroupDataService
                .findData(Integer.valueOf(time));
        if (ObjectUtil.isNull(systemGroupData)) return ApiResult.fail("参数错误");
        int today = OrderUtil.dateToTimestampT(DateUtil.beginOfDay(new Date()));//今天开始的时间戳
        JSONObject jsonObject = JSONObject.parseObject(systemGroupData.getValue());
        int startTime = today + (jsonObject.getInteger("time") * 3600);
        int endTime = today + ((jsonObject.getInteger("time") + jsonObject.getInteger("continued")) * 3600);
        **/
        return ApiResult.ok(yxStoreSeckillService.getList(queryParam.getPage().intValue(),
                queryParam.getLimit().intValue(), Integer.valueOf(time)));
    }


    /**
     * 根据id获取商品秒杀产品详情
     */
    @AnonymousAccess
    @GetMapping("/seckill/detail/{id}")
    @ApiOperation(value = "秒杀产品详情", notes = "秒杀产品详情", response = YxStoreSeckillQueryVo.class)
    public ApiResult<Object> getYxStoreSeckill(@PathVariable Integer id) throws Exception {
        int uid = SecurityUtils.getUserId().intValue();
        StoreSeckillDTO storeSeckillDTO = yxStoreSeckillService.getDetail(id);
        storeSeckillDTO.setUserCollect(relationService
                .isProductRelation(storeSeckillDTO.getStoreInfo().getProductId(),"product",uid,"collect"));
        return ApiResult.ok(storeSeckillDTO);
    }


    /**
     * 秒杀产品时间区间
     */
    @AnonymousAccess
    @GetMapping("/seckill/index")
    @ApiOperation(value = "秒杀产品时间区间", notes = "秒杀产品时间区间", response = YxStoreSeckillQueryVo.class)
    public ApiResult<Object> getYxStoreSeckillIndex() throws Exception {
        //获取秒杀配置
        AtomicInteger seckillTimeIndex = new AtomicInteger();
        SeckillConfigDTO seckillConfigDTO = new SeckillConfigDTO();
        List<YxSystemGroupData> yxSystemGroupDataList = yxSystemGroupDataService.list(new QueryWrapper<YxSystemGroupData>()
                .eq("group_name", ShopConstants.YSHOP_SECKILL_TIME));
        List<SeckillTimeDTO> list = new ArrayList<>();
        int today = OrderUtil.dateToTimestampT(DateUtil.beginOfDay(new Date()));
        yxSystemGroupDataList.forEach(i -> {
            String jsonStr = i.getValue();
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            int time = Integer.valueOf(jsonObject.get("time").toString());//时间 5
            int continued = Integer.valueOf(jsonObject.get("continued").toString());//活动持续事件  3
            SimpleDateFormat sdf = new SimpleDateFormat("HH");
            String nowTime = sdf.format(new Date());
            String index = nowTime.substring(0, 1);
            int currentHour = index.equals("0") ? Integer.valueOf(nowTime.substring(1, 2)) : Integer.valueOf(nowTime);
            SeckillTimeDTO seckillseckillTimeDTO = new SeckillTimeDTO();
            seckillseckillTimeDTO.setId(i.getId());
            //活动结束时间
            int activityEndHour = Integer.valueOf(time).intValue() + Integer.valueOf(continued).intValue();
            if (activityEndHour > 24) {
                seckillseckillTimeDTO.setState("即将开始");
                seckillseckillTimeDTO.setTime(jsonObject.get("time").toString().length() > 1 ? jsonObject.get("time").toString() + ":00" : "0" + jsonObject.get("time").toString() + ":00");
                seckillseckillTimeDTO.setStatus(2);
                seckillseckillTimeDTO.setStop(today + activityEndHour * 3600);
            } else {
                if (currentHour >= time && currentHour < activityEndHour) {
                    seckillseckillTimeDTO.setState("抢购中");
                    seckillseckillTimeDTO.setTime(jsonObject.get("time").toString().length() > 1 ? jsonObject.get("time").toString() + ":00" : "0" + jsonObject.get("time").toString() + ":00");
                    seckillseckillTimeDTO.setStatus(1);
                    seckillseckillTimeDTO.setStop(today + activityEndHour * 3600);
                    seckillTimeIndex.set(yxSystemGroupDataList.indexOf(i));

                } else if (currentHour < time) {
                    seckillseckillTimeDTO.setState("即将开始");
                    seckillseckillTimeDTO.setTime(jsonObject.get("time").toString().length() > 1 ? jsonObject.get("time").toString() + ":00" : "0" + jsonObject.get("time").toString() + ":00");
                    seckillseckillTimeDTO.setStatus(2);
                    seckillseckillTimeDTO.setStop(OrderUtil.dateToTimestamp(new Date()) + activityEndHour * 3600);
                } else if (currentHour >= activityEndHour) {
                    seckillseckillTimeDTO.setState("已结束");
                    seckillseckillTimeDTO.setTime(jsonObject.get("time").toString().length() > 1 ? jsonObject.get("time").toString() + ":00" : "0" + jsonObject.get("time").toString() + ":00");
                    seckillseckillTimeDTO.setStatus(0);
                    seckillseckillTimeDTO.setStop(today + activityEndHour * 3600);
                }
            }
            list.add(seckillseckillTimeDTO);
        });
        seckillConfigDTO.setSeckillTimeIndex(seckillTimeIndex.get());
        seckillConfigDTO.setSeckillTime(list);
        return ApiResult.ok(seckillConfigDTO);
    }
}

