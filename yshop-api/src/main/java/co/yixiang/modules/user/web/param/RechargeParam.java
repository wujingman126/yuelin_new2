package co.yixiang.modules.user.web.param;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @ClassName RechargeParam
 * @Author hupeng <610796224@qq.com>
 * @Date 2019/12/8
 **/
@Data
public class RechargeParam  implements Serializable {
    private String from;

    @NotNull(message = "金额必填")
    @Min(value = 1,message = "充值金额不能低于1")
    private Double price;

    private Double paidPrice = 0d;

    private String orderSn;
}
