package com.cityquest.entity;

import lombok.Data;
import java.util.Date;

/**
 * 动态点赞实体类
 */
@Data
public class FeedLikeInfo {
    private Integer id;
    private Integer feedId;
    private Long userId;
    private Date createTime;
}

