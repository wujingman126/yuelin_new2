package co.yixiang.modules.order.web.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName ProductAttrDTO
 * @Author hupeng <610796224@qq.com>
 * @Date 2019/11/3
 **/
@Data
public class ProductAttrDTO  implements Serializable {
    private Integer productId;
    private String suk;
    private Double price;
    private String image;
}
