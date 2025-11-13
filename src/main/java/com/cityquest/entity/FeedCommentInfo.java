package com.cityquest.entity;

import lombok.Data;
import java.util.Date;

/**
 * 动态评论实体类
 */
@Data
public class FeedCommentInfo {
    private Integer id;
    private Integer feedId;
    private Long userId;
    private String content;
    private Integer parentId;    // 父评论ID（用于回复）
    private Date createTime;
}

