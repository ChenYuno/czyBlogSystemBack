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
@TableName("tb_images")
@ApiModel(value="Images对象", description="")
public class Image implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @ApiModelProperty(value = "用户ID")
    @TableField("user_id")
    private String userId;

    @ApiModelProperty(value = "路径")
    @TableField("url")
    private String url;
    @ApiModelProperty(value = "图片原名称")
    @TableField("name")
    private String name;
    @ApiModelProperty(value = "存储路径")
    @TableField("path")
    private String path;
    @ApiModelProperty(value = "图片类型")
    @TableField("content_type")
    private String contentType;
    @ApiModelProperty(value = "图片来源")
    @TableField("original")
    private String original;

    @ApiModelProperty(value = "状态（0表示删除，1表正常）")
    @TableField("state")
    private String state;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private Date updateTime;


}
