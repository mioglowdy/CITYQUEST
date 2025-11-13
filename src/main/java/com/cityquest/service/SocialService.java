package com.cityquest.service;

import java.util.Map;

/**
 * 社交服务接口
 */
public interface SocialService {
    /**
     * 关注用户
     */
    boolean followUser(Long followerId, Long followeeId);

    /**
     * 取消关注
     */
    boolean unfollowUser(Long followerId, Long followeeId);

    /**
     * 检查是否已关注
     */
    boolean isFollowing(Long followerId, Long followeeId);

    /**
     * 获取关注列表
     */
    Map<String, Object> getFollowingList(Long userId, Integer page, Integer pageSize);

    /**
     * 获取粉丝列表
     */
    Map<String, Object> getFollowerList(Long userId, Integer page, Integer pageSize);

    /**
     * 发布动态
     */
    boolean publishFeed(Long userId, String content, String imageUrl, Integer taskId, Integer recordId, Boolean isPublic);

    /**
     * 获取动态列表
     */
    Map<String, Object> getFeedList(Long userId, Integer page, Integer pageSize, String type);

    /**
     * 点赞动态
     */
    boolean likeFeed(Integer feedId, Long userId);

    /**
     * 取消点赞
     */
    boolean unlikeFeed(Integer feedId, Long userId);

    /**
     * 评论动态
     */
    boolean commentFeed(Integer feedId, Long userId, String content, Integer parentId);

    /**
     * 获取评论列表
     */
    Map<String, Object> getCommentList(Integer feedId, Integer page, Integer pageSize);

    /**
     * 创建通知
     */
    boolean createNotification(Long userId, String type, String title, String message, Integer relatedId);

    /**
     * 获取通知列表
     */
    Map<String, Object> getNotificationList(Long userId, Integer page, Integer pageSize);

    /**
     * 标记通知为已读
     */
    boolean markNotificationAsRead(Integer notificationId);

    /**
     * 标记所有通知为已读
     */
    boolean markAllNotificationsAsRead(Long userId);

    /**
     * 获取未读通知数
     */
    int getUnreadNotificationCount(Long userId);

    /**
     * 删除动态
     */
    boolean deleteFeed(Integer feedId, Long userId);
}

