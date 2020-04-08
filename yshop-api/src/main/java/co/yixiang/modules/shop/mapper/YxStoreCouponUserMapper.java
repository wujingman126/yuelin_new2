package co.yixiang.modules.shop.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import co.yixiang.modules.shop.entity.YxStoreCouponUser;
import co.yixiang.modules.shop.web.param.YxStoreCouponUserQueryParam;
import co.yixiang.modules.shop.web.vo.YxStoreCouponUserQueryVo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

/**
 * <p>
 * 优惠券发放记录表 Mapper 接口
 * </p>
 *
 * @author hupeng
 * @since 2019-10-27
 */
@Repository
public interface YxStoreCouponUserMapper extends BaseMapper<YxStoreCouponUser> {

    /**
     * 根据ID获取查询对象
     * @param id
     * @return
     */
    YxStoreCouponUserQueryVo getYxStoreCouponUserById(Serializable id);

    /**
     * 获取分页对象
     * @param page
     * @param yxStoreCouponUserQueryParam
     * @return
     */
    IPage<YxStoreCouponUserQueryVo> getYxStoreCouponUserPageList(@Param("page") Page page, @Param("param") YxStoreCouponUserQueryParam yxStoreCouponUserQueryParam);

}
