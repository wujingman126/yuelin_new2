package co.yixiang.modules.order.web.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName ProductDTO
 * @Author hupeng <610796224@qq.com>
 * @Date 2019/11/3
 **/
@Data
public class ProductDTO  implements Serializable {
    private String image;
    private Double price;
    private String storeName;
    private ProductAttrDTO attrInfo;

}
