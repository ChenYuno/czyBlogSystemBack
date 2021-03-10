package net.sjw.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.util.ArrayList;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.util.List;

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
@TableName("tb_comment")
@ApiModel(value="Comment对象", description="")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @ApiModelProperty(value = "父内容id")
    @TableField("parent_content_id")
    private String parentContentId;

    @TableField(exist = false)
    private List<Comment> childrenCommends = new ArrayList<>();

    @ApiModelProperty(value = "文章ID")
    @TableField("article_id")
    private String articleId;

    @ApiModelProperty(value = "评论内容")
    @TableField("content")
    private String content;

    @ApiModelProperty(value = "评论用户的ID")
    @TableField("user_id")
    private String userId;

    @ApiModelProperty(value = "评论用户的头像")
    @TableField("user_avatar")
    private String userAvatar;

    @ApiModelProperty(value = "评论用户的名称")
    @TableField("user_name")
    private String userName;

    @ApiModelProperty(value = "状态（0表示删除，1表示正常）")
    @TableField("state")
    private String state="1";

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private Date updateTime;


}
