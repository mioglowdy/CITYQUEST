package com.cityquest.entity;

import lombok.Data;
import java.util.Date;

/**
 * 用户动态实体类
 */
@Data
public class UserFeedInfo {
    private Integer id;
    private Long userId;
    private String content;
    private String imageUrl;
    private Integer taskId;      // 关联的任务ID（可选）
    private Integer recordId;     // 关联的打卡记录ID（可选）
    private Boolean isPublic;    // 是否公开
    private Integer likeCount;   // 点赞数
    private Integer commentCount; // 评论数
    private Date createTime;
    private Date updateTime;
}

