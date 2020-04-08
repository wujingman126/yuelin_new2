package co.yixiang.modules.activity.web.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName PinkDTO
 * @Author hupeng <610796224@qq.com>
 * @Date 2019/11/19
 **/
@Data
public class PinkDTO implements Serializable {
    private Integer id;
    private Integer uid;
    private Integer people;
    private Double price;
    private String stopTime;
    private String nickname;
    private String avatar;


    private String count;
    private String h;
    private String i;
    private String s;


}
