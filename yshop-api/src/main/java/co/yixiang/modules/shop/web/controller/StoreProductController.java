/**
 * Copyright (C) 2018-2019
 * All rights reserved, Designed By www.yixiang.co
 * 注意：
 * 本软件为www.yixiang.co开发研制，未经购买不得使用
 * 购买后可获得全部源代码（禁止转卖、分享、上传到码云、github等开源平台）
 * 一经发现盗用、分享等行为，将追究法律责任，后果自负
 */
package co.yixiang.modules.shop.web.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.http.HttpUtil;
import co.yixiang.annotation.AnonymousAccess;
import co.yixiang.aop.log.Log;
import co.yixiang.common.api.ApiResult;
import co.yixiang.common.web.controller.BaseController;
import co.yixiang.enums.AppFromEnum;
import co.yixiang.enums.ProductEnum;
import co.yixiang.modules.shop.service.YxStoreProductRelationService;
import co.yixiang.modules.shop.service.YxStoreProductReplyService;
import co.yixiang.modules.shop.service.YxStoreProductService;
import co.yixiang.modules.shop.service.YxSystemConfigService;
import co.yixiang.modules.shop.web.dto.ProductDTO;
import co.yixiang.modules.shop.web.param.YxStoreProductQueryParam;
import co.yixiang.modules.shop.web.param.YxStoreProductRelationQueryParam;
import co.yixiang.modules.shop.web.vo.YxStoreProductQueryVo;
import co.yixiang.modules.user.entity.YxSystemAttachment;
import co.yixiang.modules.user.service.YxSystemAttachmentService;
import co.yixiang.modules.user.service.YxUserService;
import co.yixiang.modules.user.web.vo.YxUserQueryVo;
import co.yixiang.utils.OrderUtil;
import co.yixiang.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 商品控制器
 * </p>
 *
 * @author hupeng
 * @since 2019-10-19
 */
@Slf4j
@RestController
@Api(value = "产品模块", tags = "商城:产品模块", description = "产品模块")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StoreProductController extends BaseController {

    private final YxStoreProductService storeProductService;
    private final YxStoreProductRelationService productRelationService;
    private final YxStoreProductReplyService replyService;
    private final YxSystemConfigService systemConfigService;
    private final YxSystemAttachmentService systemAttachmentService;
    private final YxUserService yxUserService;

    @Value("${file.path}")
    private String path;


    /**
     * 获取首页更多产品
     */
    @AnonymousAccess
    @GetMapping("/groom/list/{type}")
    @ApiOperation(value = "获取首页更多产品",notes = "获取首页更多产品")
    public ApiResult<Map<String,Object>> moreGoodsList(@PathVariable Integer type){
        Map<String,Object> map = new LinkedHashMap<>();
        if(type.equals(ProductEnum.TYPE_1.getValue())){//TODO 精品推荐
            map.put("list",storeProductService.getList(1,20,1));
        }else if(type.equals(ProductEnum.TYPE_2.getValue())){//TODO  热门榜单
            map.put("list",storeProductService.getList(1,20,4));
        }else if(type.equals(ProductEnum.TYPE_3.getValue())){//TODO 首发新品
            map.put("list",storeProductService.getList(1,20,2));
        }else if(type.equals(ProductEnum.TYPE_4.getValue())){//TODO 促销单品
            map.put("list",storeProductService.getList(1,20,3));
        }

        return ApiResult.ok(map);
    }

    /**
     * 获取首页更多产品
     */
    @AnonymousAccess
    @GetMapping("/products")
    @ApiOperation(value = "商品列表",notes = "商品列表")
    public ApiResult<List<YxStoreProductQueryVo>> goodsList(YxStoreProductQueryParam productQueryParam){
        return ApiResult.ok(storeProductService.getGoodsList(productQueryParam));
    }

    /**
     * 为你推荐
     */
    @AnonymousAccess
    @GetMapping("/product/hot")
    @ApiOperation(value = "为你推荐",notes = "为你推荐")
    public ApiResult<List<YxStoreProductQueryVo>> productRecommend(YxStoreProductQueryParam queryParam){
        return ApiResult.ok(storeProductService.getList(queryParam.getPage().intValue(),
                queryParam.getLimit().intValue(),1));
    }

    /**
     * 普通商品详情
     */
    @Log(value = "查看商品详情",type = 1)
    @GetMapping("/product/detail/{id}")
    @ApiOperation(value = "普通商品详情",notes = "普通商品详情")
    public ApiResult<ProductDTO> detail(@PathVariable Integer id,
                                        @RequestParam(value = "",required=false) String latitude,
                                        @RequestParam(value = "",required=false) String longitude,
                                        @RequestParam(value = "",required=false) String from){
        int uid = SecurityUtils.getUserId().intValue();

        ProductDTO productDTO = storeProductService.goodsDetail(id,0,uid,latitude,longitude);

        // 海报
        String siteUrl = systemConfigService.getData("site_url");
        if(StrUtil.isEmpty(siteUrl)){
            return ApiResult.fail("未配置h5地址");
        }
        String apiUrl = systemConfigService.getData("api_url");
        if(StrUtil.isEmpty(apiUrl)){
            return ApiResult.fail("未配置api地址");
        }

        YxUserQueryVo userInfo = yxUserService.getYxUserById(uid);
        String userType = userInfo.getUserType();
        if(!userType.equals(AppFromEnum.ROUNTINE.getValue())) {
            userType = AppFromEnum.H5.getValue();
        }
        //app类型
        if(StrUtil.isNotBlank(from) && AppFromEnum.APP.getValue().equals(from)){
            String name = id+"_"+uid + "_"+from+"_product_detail_wap.jpg";
            YxSystemAttachment attachment = systemAttachmentService.getInfo(name);
            String inviteCode =  OrderUtil.createShareCode();
            if(ObjectUtil.isNull(attachment)){
                systemAttachmentService.newAttachmentAdd(name, "", "","",uid,inviteCode);
            }else{
                inviteCode = attachment.getInviteCode();
            }

            productDTO.getStoreInfo().setCodeBase(inviteCode);
        }else {
            String name = id+"_"+uid + "_"+userType+"_product_detail_wap.jpg";
            YxSystemAttachment attachment = systemAttachmentService.getInfo(name);
            String fileDir = path+"qrcode"+ File.separator;
            String qrcodeUrl = "";
            String routineQrcodeUrl = "";
            if(ObjectUtil.isNull(attachment)){
                //生成二维码
                File file = FileUtil.mkdir(new File(fileDir));
                if(userType.equals(AppFromEnum.ROUNTINE.getValue())){
                    //下载图片
                    siteUrl = siteUrl+"/product/";
                    QrCodeUtil.generate(siteUrl+"?productId="+id+"&spread="+uid, 180, 180,
                            FileUtil.file(fileDir+name));
                }else{
                    QrCodeUtil.generate(siteUrl+"/detail/"+id+"?spread="+uid, 180, 180,
                            FileUtil.file(fileDir+name));
                }

                systemAttachmentService.attachmentAdd(name,String.valueOf(FileUtil.size(file)),
                        fileDir+name,"qrcode/"+name);

                qrcodeUrl = fileDir+name;
                routineQrcodeUrl = apiUrl + "/api/file/qrcode/"+name;
            }else{
                qrcodeUrl = attachment.getAttDir();
                routineQrcodeUrl = apiUrl + "/api/file/" + attachment.getSattDir();
            }

            if(userType.equals(AppFromEnum.ROUNTINE.getValue())){
                productDTO.getStoreInfo().setCodeBase(routineQrcodeUrl);
            }else{
                try {
                    String base64CodeImg = co.yixiang.utils.FileUtil.fileToBase64(new File(qrcodeUrl));
                    productDTO.getStoreInfo().setCodeBase("data:image/jpeg;base64," + base64CodeImg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        return ApiResult.ok(productDTO);
    }

    /**
     * 添加收藏
     */
    @Log(value = "添加收藏",type = 1)
    @PostMapping("/collect/add")
    @ApiOperation(value = "添加收藏",notes = "添加收藏")
    public ApiResult<Object> collectAdd(@Validated @RequestBody YxStoreProductRelationQueryParam param){
        int uid = SecurityUtils.getUserId().intValue();
        productRelationService.addRroductRelation(param,uid,"collect");
        return ApiResult.ok("success");
    }

    /**
     * 取消收藏
     */
    @Log(value = "取消收藏",type = 1)
    @PostMapping("/collect/del")
    @ApiOperation(value = "取消收藏",notes = "取消收藏")
    public ApiResult<Object> collectDel(@Validated @RequestBody YxStoreProductRelationQueryParam param){
        int uid = SecurityUtils.getUserId().intValue();
        productRelationService.delRroductRelation(param,uid,"collect");
        return ApiResult.ok("success");
    }

    /**
     * 获取产品评论
     */
    @GetMapping("/reply/list/{id}")
    @ApiOperation(value = "获取产品评论",notes = "获取产品评论")
    public ApiResult<Object> replyList(@PathVariable Integer id,
                                       YxStoreProductQueryParam queryParam){
        return ApiResult.ok(replyService.getReplyList(id,Integer.valueOf(queryParam.getType()),
                queryParam.getPage().intValue(),queryParam.getLimit().intValue()));
    }

    /**
     * 获取产品评论数据
     */
    @GetMapping("/reply/config/{id}")
    @ApiOperation(value = "获取产品评论数据",notes = "获取产品评论数据")
    public ApiResult<Object> replyCount(@PathVariable Integer id){
        return ApiResult.ok(replyService.getReplyCount(id));
    }














}

