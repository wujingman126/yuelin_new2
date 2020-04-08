package co.yixiang.modules.order.web.dto;

import lombok.Data;

/**
 * @ClassName PriceGroup
 * @Author hupeng <610796224@qq.com>
 * @Date 2019/10/27
 **/
@Data
public class PriceGroupDTO {

    private Double costPrice;
    private Double storeFreePostage;
    private Double storePostage;
    private Double totalPrice;
    private Double vipPrice;

}
