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
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Transient;

/**
 * <p>
 *
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tb_article")
@ApiModel(value="Article对象", description="")
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @ApiModelProperty(value = "标题")
    @TableField("title")
    private String title;

    @ApiModelProperty(value = "用户ID")
    @TableField("user_id")
    private String userId;

    @ApiModelProperty(value = "用户头像")
    @TableField("user_avatar")
    private String userAvatar;

    @ApiModelProperty(value = "用户昵称")
    @TableField("user_name")
    private String userName;

    @ApiModelProperty(value = "文章封面")
    @TableField("cover")
    private String cover;

    @ApiModelProperty(value = "分类ID")
    @TableField("category_id")
    private String categoryId;

    @ApiModelProperty(value = "文章内容")
    @TableField("content")
    private String content;

    @ApiModelProperty(value = "类型（0表示富文本，1表示markdown）")
    @TableField("type")
    private String type;

    @ApiModelProperty(value = "状态（1表示已发布，2表示草稿，0表示删除、3表示置顶）")
    @TableField("state")
    private String state = "1";

    @ApiModelProperty(value = "摘要")
    @TableField("summary")
    private String summary;

    @ApiModelProperty(value = "标签")
    @TableField("labels")
    private String labels;

    @ApiModelProperty(value = "阅读数量")
    @TableField("view_count")
    private Integer viewCount = 0;

    @ApiModelProperty(value = "发布时间")
    @TableField("create_time")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private List<String> label = new ArrayList<>();

    public void setLabel() {
        String labels = getLabels();
        if (labels.contains("-")) {
            String[] split = labels.split("-");
            for (String lbl : split) {
                label.add(lbl);
            }
        } else {
            label.add(labels);
        }
    }
}
