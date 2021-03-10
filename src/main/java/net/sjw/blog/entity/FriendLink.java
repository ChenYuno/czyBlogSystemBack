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
@TableName("tb_friends")
@ApiModel(value="Friends对象", description="")
public class FriendLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID")
    @TableId(value = "`id`", type = IdType.ASSIGN_ID)
    private String id;

    @ApiModelProperty(value = "友情链接名称")
    @TableField("`name`")
    private String name;

    @ApiModelProperty(value = "友情链接logo")
    @TableField("logo")
    private String logo;

    @ApiModelProperty(value = "友情链接")
    @TableField("`url`")
    private String url;

    @ApiModelProperty(value = "顺序")
    @TableField("`order`")
    private Integer order;
    @ApiModelProperty(value = "友链描述")
    @TableField("`description`")
    private String description;

    @ApiModelProperty(value = "友情链接状态:0表示不可用，1表示正常")
    @TableField("`state`")
    private String state;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private Date updateTime;


}
