package com.cityquest.entity;

import lombok.Data;
import java.util.Date;

/**
 * 好友关系实体类
 */
@Data
public class FriendshipInfo {
    private Integer id;
    private Long followerId;  // 关注者ID
    private Long followeeId;  // 被关注者ID
    private Date createTime;
}

