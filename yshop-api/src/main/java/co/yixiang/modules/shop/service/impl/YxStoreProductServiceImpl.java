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
import cn.hutool.core.util.StrUtil;
import co.yixiang.common.service.impl.BaseServiceImpl;
import co.yixiang.common.web.vo.Paging;
import co.yixiang.enums.CommonEnum;
import co.yixiang.exception.ErrorRequestException;
import co.yixiang.modules.shop.entity.YxStoreProduct;
import co.yixiang.modules.shop.entity.YxStoreProductAttrValue;
import co.yixiang.modules.shop.mapper.YxStoreProductAttrValueMapper;
import co.yixiang.modules.shop.mapper.YxStoreProductMapper;
import co.yixiang.modules.shop.mapping.YxStoreProductMap;
import co.yixiang.modules.shop.service.*;
import co.yixiang.modules.shop.web.dto.ProductDTO;
import co.yixiang.modules.shop.web.param.YxStoreProductQueryParam;
import co.yixiang.modules.shop.web.vo.YxStoreProductAttrQueryVo;
import co.yixiang.modules.shop.web.vo.YxStoreProductQueryVo;
import co.yixiang.modules.user.service.YxUserService;
import co.yixiang.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author hupeng
 * @since 2019-10-19
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@SuppressWarnings("unchecked")
public class YxStoreProductServiceImpl extends BaseServiceImpl<YxStoreProductMapper, YxStoreProduct> implements YxStoreProductService {

    @Autowired
    private YxStoreProductMapper yxStoreProductMapper;
    @Autowired
    private YxStoreProductAttrValueMapper storeProductAttrValueMapper;

    @Autowired
    private YxStoreProductAttrService storeProductAttrService;
    @Autowired
    private YxStoreProductRelationService relationService;
    @Autowired
    private YxStoreProductReplyService replyService;
    @Autowired
    private YxUserService userService;
    @Autowired
    private YxSystemStoreService systemStoreService;

    @Autowired
    private YxStoreProductMap storeProductMap;



    /**
     * 增加库存 减少销量
     * @param num
     * @param productId
     * @param unique
     */
    @Override
    public void incProductStock(int num, int productId, String unique) {
        if(StrUtil.isNotEmpty(unique)){
            storeProductAttrService.incProductAttrStock(num,productId,unique);
            yxStoreProductMapper.decSales(num,productId);
        }else{
            yxStoreProductMapper.incStockDecSales(num,productId);
        }
    }

    /**
     * 库存与销量
     * @param num
     * @param productId
     * @param unique
     */
    @Override
    public void decProductStock(int num, int productId, String unique) {
        if(StrUtil.isNotEmpty(unique)){
            storeProductAttrService.decProductAttrStock(num,productId,unique);
            yxStoreProductMapper.incSales(num,productId);
        }else{
            yxStoreProductMapper.decStockIncSales(num,productId);
        }
    }

    /**
     * 返回商品库存
     * @param productId
     * @param unique
     * @return
     */
    @Override
    public int getProductStock(int productId, String unique) {
        if(StrUtil.isEmpty(unique)){
            return getYxStoreProductById(productId).getStock();
        }else{
            return storeProductAttrService.uniqueByStock(unique);
        }

    }

    @Override
    public ProductDTO goodsDetail(int id, int type,int uid,String latitude,String longitude) {
        QueryWrapper<YxStoreProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("is_del",0).eq("is_show",1).eq("id",id);
        YxStoreProduct storeProduct = yxStoreProductMapper.selectOne(wrapper);
        if(ObjectUtil.isNull(storeProduct)){
            throw new ErrorRequestException("商品不存在或已下架");
        }
        Map<String, Object> returnMap = storeProductAttrService.getProductAttrDetail(id,0,0);
        ProductDTO productDTO = new ProductDTO();
        YxStoreProductQueryVo storeProductQueryVo  = storeProductMap.toDto(storeProduct);

        //处理库存
        Integer newStock = storeProductAttrValueMapper.sumStock(id);
        if(newStock != null)  storeProductQueryVo.setStock(newStock);

        //设置VIP价格
        double vipPrice = userService.setLevelPrice(
                storeProductQueryVo.getPrice().doubleValue(),uid);
        storeProductQueryVo.setVipPrice(BigDecimal.valueOf(vipPrice));
        storeProductQueryVo.setUserCollect(relationService
                .isProductRelation(id,"product",uid,"collect"));
        productDTO.setStoreInfo(storeProductQueryVo);
        productDTO.setProductAttr((List<YxStoreProductAttrQueryVo>)returnMap.get("productAttr"));
        productDTO.setProductValue((Map<String, YxStoreProductAttrValue>)returnMap.get("productValue"));

        productDTO.setReply(replyService.getReply(id));
        int replyCount = replyService.productReplyCount(id);
        productDTO.setReplyCount(replyCount);
        productDTO.setReplyChance(replyService.doReply(id,replyCount));//百分比

        //门店
        productDTO.setSystemStore(systemStoreService.getStoreInfo(latitude,longitude));
        productDTO.setMapKey(RedisUtil.get("tengxun_map_key"));

        return productDTO;
    }

    /**
     * 商品列表
     * @return
     */
    @Override
    public List<YxStoreProductQueryVo> getGoodsList(YxStoreProductQueryParam productQueryParam) {

        QueryWrapper<YxStoreProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("is_del", CommonEnum.DEL_STATUS_0.getValue()).eq("is_show",CommonEnum.SHOW_STATUS_1.getValue());

        //分类搜索
        if(StrUtil.isNotBlank(productQueryParam.getSid()) && !productQueryParam.getSid().equals("0")){
            wrapper.eq("cate_id",productQueryParam.getSid());
        }
        //关键字搜索
        if(StrUtil.isNotEmpty(productQueryParam.getKeyword())){
            wrapper.like("store_name",productQueryParam.getKeyword());
        }

        //新品搜索
        if(StrUtil.isNotBlank(productQueryParam.getNews()) && productQueryParam.getNews().equals("1")){
            wrapper.eq("is_new",1);
        }
        //销量排序
        if(productQueryParam.getSalesOrder().equals("desc")){
            wrapper.orderByDesc("sales");
        }else if(productQueryParam.getSalesOrder().equals("asc")) {
            wrapper.orderByAsc("sales");
        }
        //价格排序
        if(productQueryParam.getPriceOrder().equals("desc")){
            wrapper.orderByDesc("price");
        }else if(productQueryParam.getPriceOrder().equals("asc")){
            wrapper.orderByAsc("price");
        }
        wrapper.orderByDesc("sort");


        Page<YxStoreProduct> pageModel = new Page<>(productQueryParam.getPage(),
                productQueryParam.getLimit());

        IPage<YxStoreProduct> pageList = yxStoreProductMapper.selectPage(pageModel,wrapper);

        List<YxStoreProductQueryVo> list = storeProductMap.toDto(pageList.getRecords());

//        for (GoodsDTO goodsDTO : list) {
//            goodsDTO.setIsCollect(isCollect(goodsDTO.getGoodsId(),userId));
//        }

        return list;
    }

    /**
     * 商品列表
     * @param page
     * @param limit
     * @param order
     * @return
     */
    @Override
    public List<YxStoreProductQueryVo> getList(int page, int limit, int order) {

        QueryWrapper<YxStoreProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("is_del",0).eq("is_show",1).orderByDesc("sort");


        //todo order = 1 精品推荐  order=2  新品 3-优惠产品 4-热卖
        switch (order){
            case 1:
                wrapper.eq("is_best",1);
                break;
            case 2:
                wrapper.eq("is_new",1);
                break;
            case 3:
                wrapper.eq("is_benefit",1);
                break;
            case 4:
                wrapper.eq("is_hot",1);
                break;
        }
        Page<YxStoreProduct> pageModel = new Page<>(page, limit);

        IPage<YxStoreProduct> pageList = yxStoreProductMapper.selectPage(pageModel,wrapper);

        List<YxStoreProductQueryVo> list = storeProductMap.toDto(pageList.getRecords());


        return list;
    }

    @Override
    public YxStoreProductQueryVo getYxStoreProductById(Serializable id){
        return yxStoreProductMapper.getYxStoreProductById(id);
    }

    @Override
    public Paging<YxStoreProductQueryVo> getYxStoreProductPageList(YxStoreProductQueryParam yxStoreProductQueryParam) throws Exception{
        Page page = setPageParam(yxStoreProductQueryParam,OrderItem.desc("create_time"));
        IPage<YxStoreProductQueryVo> iPage = yxStoreProductMapper.getYxStoreProductPageList(page,yxStoreProductQueryParam);
        return new Paging(iPage);
    }

}
