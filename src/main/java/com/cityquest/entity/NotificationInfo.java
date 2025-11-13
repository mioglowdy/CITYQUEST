package com.cityquest.entity;

import lombok.Data;
import java.util.Date;

/**
 * 通知信息实体类
 */
@Data
public class NotificationInfo {
    private Integer id;
    private Long userId;      // 接收通知的用户ID
    private String type;          // 通知类型（task_completed, task_audited, new_follower, feed_like, feed_comment等）
    private String title;        // 通知标题
    private String message;       // 通知内容
    private Integer relatedId;    // 关联ID（任务ID、动态ID等）
    private Boolean readStatus;   // 是否已读
    private Date createTime;
}

