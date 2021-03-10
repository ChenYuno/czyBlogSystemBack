package net.sjw.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 *
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tb_looper")
@ApiModel(value="Looper对象", description="")
public class Looper implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @ApiModelProperty(value = "轮播图标题")
    @TableField("title")
    private String title;

    @ApiModelProperty(value = "顺序")
    @TableField("`order`")
    private Integer order =1;

    @ApiModelProperty(value = "状态：0表示不可用，1表示正常")
    @TableField("`state`")
    private String state ="1";

    @ApiModelProperty(value = "目标URL")
    @TableField("target_url")
    private String targetUrl;

    @ApiModelProperty(value = "图片地址")
    @TableField("image_url")
    private String imageUrl;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private Date updateTime;


}
