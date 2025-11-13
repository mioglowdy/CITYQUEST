package com.cityquest.service.impl;

import com.cityquest.entity.*;
import com.cityquest.mapper.*;
import com.cityquest.service.SocialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 社交服务实现类
 */
@Service
public class SocialServiceImpl implements SocialService {

    @Autowired
    private FriendshipMapper friendshipMapper;

    @Autowired
    private FeedMapper feedMapper;

    @Autowired
    private FeedLikeMapper feedLikeMapper;

    @Autowired
    private FeedCommentMapper feedCommentMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public boolean followUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new RuntimeException("不能关注自己");
        }

        // 验证关注者是否存在
        UserInfo follower = userMapper.selectById(followerId);
        if (follower == null) {
            throw new RuntimeException("关注者用户不存在");
        }

        // 验证被关注者是否存在
        UserInfo followee = userMapper.selectById(followeeId);
        if (followee == null) {
            throw new RuntimeException("被关注者用户不存在");
        }

        // 检查是否已关注
        if (friendshipMapper.checkFollow(followerId, followeeId) > 0) {
            throw new RuntimeException("已经关注过该用户");
        }

        // 添加关注关系
        FriendshipInfo friendshipInfo = new FriendshipInfo();
        friendshipInfo.setFollowerId(followerId);
        friendshipInfo.setFolloweeId(followeeId);
        friendshipInfo.setCreateTime(new Date());

        int result = friendshipMapper.insert(friendshipInfo);

        // 创建通知：被关注者收到新粉丝通知
        if (result > 0) {
            String title = "新粉丝";
            String followerName = follower.getNickname() != null ? follower.getNickname() : follower.getUsername();
            String message = followerName + " 关注了你";
            Integer relatedUserId = followerId > Integer.MAX_VALUE ? null : followerId.intValue();
            createNotification(followeeId, "new_follower", title, message, relatedUserId);
        }

        return result > 0;
    }

    @Override
    @Transactional
    public boolean unfollowUser(Long followerId, Long followeeId) {
        return friendshipMapper.delete(followerId, followeeId) > 0;
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        return friendshipMapper.checkFollow(followerId, followeeId) > 0;
    }

    @Override
    public Map<String, Object> getFollowingList(Long userId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<FriendshipInfo> friendships = friendshipMapper.selectFollowingList(userId, offset, pageSize);
        int total = friendshipMapper.selectFollowingCount(userId);

        // 富化数据：添加用户信息
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (FriendshipInfo friendship : friendships) {
            Map<String, Object> item = new HashMap<>();
            UserInfo user = userMapper.selectById(friendship.getFolloweeId());
            if (user != null) {
                item.put("id", user.getId());
                item.put("username", user.getUsername());
                item.put("nickname", user.getNickname());
                item.put("avatar", user.getAvatar());
                item.put("points", user.getPoints());
                item.put("followTime", friendship.getCreateTime());
            }
            enriched.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", enriched);
        result.put("items", enriched);
        result.put("total", total);
        return result;
    }

    @Override
    public Map<String, Object> getFollowerList(Long userId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<FriendshipInfo> friendships = friendshipMapper.selectFollowerList(userId, offset, pageSize);
        int total = friendshipMapper.selectFollowerCount(userId);

        // 富化数据：添加用户信息
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (FriendshipInfo friendship : friendships) {
            Map<String, Object> item = new HashMap<>();
            UserInfo user = userMapper.selectById(friendship.getFollowerId());
            if (user != null) {
                item.put("id", user.getId());
                item.put("username", user.getUsername());
                item.put("nickname", user.getNickname());
                item.put("avatar", user.getAvatar());
                item.put("points", user.getPoints());
                item.put("followTime", friendship.getCreateTime());
            }
            enriched.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", enriched);
        result.put("items", enriched);
        result.put("total", total);
        return result;
    }

    @Override
    @Transactional
    public boolean publishFeed(Long userId, String content, String imageUrl, Integer taskId, Integer recordId, Boolean isPublic) {
        UserFeedInfo feedInfo = new UserFeedInfo();
        feedInfo.setUserId(userId);
        feedInfo.setContent(content);
        feedInfo.setImageUrl(imageUrl);
        feedInfo.setTaskId(taskId);
        feedInfo.setRecordId(recordId);
        feedInfo.setIsPublic(isPublic != null ? isPublic : true);
        feedInfo.setLikeCount(0);
        feedInfo.setCommentCount(0);
        feedInfo.setCreateTime(new Date());
        feedInfo.setUpdateTime(new Date());

        int result = feedMapper.insert(feedInfo);

        // 如果动态关联了任务，通知关注该用户的好友
        if (result > 0 && taskId != null) {
            notifyFollowersAboutFeed(userId, feedInfo.getId(), taskId);
        }

        return result > 0;
    }

    @Override
    public Map<String, Object> getFeedList(Long userId, Integer page, Integer pageSize, String type) {
        int offset = (page - 1) * pageSize;
        List<UserFeedInfo> feeds;
        int total;

        if ("my".equals(type) && userId != null) {
            // 我的动态
            feeds = feedMapper.selectByUserId(userId, offset, pageSize);
            total = feedMapper.selectCount(userId, null);
        } else if ("friend".equals(type) && userId != null) {
            // 好友动态
            feeds = feedMapper.selectFriendFeedList(userId, offset, pageSize);
            total = feedMapper.selectCount(null, false); // 简化处理
        } else {
            // 公开动态（时间线）
            feeds = feedMapper.selectPublicFeedList(offset, pageSize);
            total = feedMapper.selectCount(null, true);
        }

        // 富化数据：添加用户信息、点赞状态等
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (UserFeedInfo feed : feeds) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", feed.getId());
            item.put("content", feed.getContent());
            item.put("imageUrl", feed.getImageUrl());
            // 解析imageUrl为imageList（如果imageUrl包含多个图片，用逗号分隔）
            if (feed.getImageUrl() != null && !feed.getImageUrl().isEmpty()) {
                String[] imageArray = feed.getImageUrl().split(",");
                List<String> imageList = new ArrayList<>();
                for (String img : imageArray) {
                    String trimmed = img.trim();
                    if (!trimmed.isEmpty()) {
                        imageList.add(trimmed);
                    }
                }
                item.put("imageList", imageList);
            } else {
                item.put("imageList", new ArrayList<>());
            }
            item.put("taskId", feed.getTaskId());
            item.put("recordId", feed.getRecordId());
            item.put("isPublic", feed.getIsPublic());
            item.put("likeCount", feed.getLikeCount());
            item.put("commentCount", feed.getCommentCount());
            item.put("createTime", feed.getCreateTime());

            // 添加用户信息
            UserInfo user = userMapper.selectById(feed.getUserId());
            if (user != null) {
                item.put("userId", user.getId());
                item.put("username", user.getUsername());
                item.put("nickname", user.getNickname());
                item.put("avatar", user.getAvatar());
            }

            // 检查当前用户是否已点赞
            if (userId != null) {
                item.put("isLiked", feedLikeMapper.checkLike(feed.getId(), userId) > 0);
            } else {
                item.put("isLiked", false);
            }

            enriched.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", enriched);
        result.put("items", enriched);
        result.put("total", total);
        return result;
    }

    @Override
    @Transactional
    public boolean likeFeed(Integer feedId, Long userId) {
        // 检查是否已点赞
        if (feedLikeMapper.checkLike(feedId, userId) > 0) {
            return false; // 已点赞
        }

        // 添加点赞
        FeedLikeInfo likeInfo = new FeedLikeInfo();
        likeInfo.setFeedId(feedId);
        likeInfo.setUserId(userId);
        likeInfo.setCreateTime(new Date());

        int result = feedLikeMapper.insert(likeInfo);

        // 更新动态点赞数
        if (result > 0) {
            feedMapper.updateLikeCount(feedId, 1);

            // 创建通知：动态作者收到点赞通知
            UserFeedInfo feed = feedMapper.selectById(feedId);
            if (feed != null && !feed.getUserId().equals(userId)) {
                UserInfo liker = userMapper.selectById(userId);
                String title = "收到点赞";
                String message = (liker.getNickname() != null ? liker.getNickname() : liker.getUsername()) + " 赞了你的动态";
                createNotification(feed.getUserId(), "feed_like", title, message, feedId);
            }
        }

        return result > 0;
    }

    @Override
    @Transactional
    public boolean unlikeFeed(Integer feedId, Long userId) {
        int result = feedLikeMapper.delete(feedId, userId);

        // 更新动态点赞数
        if (result > 0) {
            feedMapper.updateLikeCount(feedId, -1);
        }

        return result > 0;
    }

    @Override
    @Transactional
    public boolean commentFeed(Integer feedId, Long userId, String content, Integer parentId) {
        FeedCommentInfo commentInfo = new FeedCommentInfo();
        commentInfo.setFeedId(feedId);
        commentInfo.setUserId(userId);
        commentInfo.setContent(content);
        commentInfo.setParentId(parentId);
        commentInfo.setCreateTime(new Date());

        int result = feedCommentMapper.insert(commentInfo);

        // 更新动态评论数
        if (result > 0) {
            feedMapper.updateCommentCount(feedId, 1);

            // 创建通知：动态作者收到评论通知
            UserFeedInfo feed = feedMapper.selectById(feedId);
            if (feed != null && !feed.getUserId().equals(userId)) {
                UserInfo commenter = userMapper.selectById(userId);
                String title = "收到评论";
                String message = (commenter.getNickname() != null ? commenter.getNickname() : commenter.getUsername()) + " 评论了你的动态";
                createNotification(feed.getUserId(), "feed_comment", title, message, feedId);
            }
        }

        return result > 0;
    }

    @Override
    public Map<String, Object> getCommentList(Integer feedId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<FeedCommentInfo> comments = feedCommentMapper.selectByFeedId(feedId, offset, pageSize);
        int total = feedCommentMapper.selectCount(feedId);

        // 富化数据：添加用户信息
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (FeedCommentInfo comment : comments) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", comment.getId());
            item.put("content", comment.getContent());
            item.put("parentId", comment.getParentId());
            item.put("createTime", comment.getCreateTime());

            // 添加用户信息
            UserInfo user = userMapper.selectById(comment.getUserId());
            if (user != null) {
                item.put("userId", user.getId());
                item.put("username", user.getUsername());
                item.put("nickname", user.getNickname());
                item.put("avatar", user.getAvatar());
            }

            enriched.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", enriched);
        result.put("items", enriched);
        result.put("total", total);
        return result;
    }

    @Override
    public boolean createNotification(Long userId, String type, String title, String message, Integer relatedId) {
        if (userId == null) {
            return false;
        }
        NotificationInfo notificationInfo = new NotificationInfo();
        notificationInfo.setUserId(userId);
        notificationInfo.setType(type);
        notificationInfo.setTitle(title);
        notificationInfo.setMessage(message);
        notificationInfo.setRelatedId(relatedId);
        notificationInfo.setReadStatus(false);
        notificationInfo.setCreateTime(new Date());

        return notificationMapper.insert(notificationInfo) > 0;
    }

    @Override
    public Map<String, Object> getNotificationList(Long userId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<NotificationInfo> notifications = notificationMapper.selectByUserId(userId, offset, pageSize);
        int unreadCount = notificationMapper.selectUnreadCount(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", notifications);
        result.put("items", notifications);
        result.put("total", notifications.size());
        result.put("unreadCount", unreadCount);
        return result;
    }

    @Override
    public boolean markNotificationAsRead(Integer notificationId) {
        return notificationMapper.markAsRead(notificationId) > 0;
    }

    @Override
    public boolean markAllNotificationsAsRead(Long userId) {
        return notificationMapper.markAllAsRead(userId) > 0;
    }

    @Override
    public int getUnreadNotificationCount(Long userId) {
        return notificationMapper.selectUnreadCount(userId);
    }

    @Override
    @Transactional
    public boolean deleteFeed(Integer feedId, Long userId) {
        // 检查动态是否存在
        UserFeedInfo feed = feedMapper.selectById(feedId);
        if (feed == null) {
            throw new RuntimeException("动态不存在");
        }

        // 检查是否有权限删除（只能删除自己的动态）
        if (!feed.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此动态");
        }

        // 删除动态相关的点赞记录
        feedLikeMapper.deleteByFeedId(feedId);

        // 删除动态相关的评论记录
        feedCommentMapper.deleteByFeedId(feedId);

        // 删除动态
        int result = feedMapper.delete(feedId);
        return result > 0;
    }

    /**
     * 通知关注者关于新动态
     */
    private void notifyFollowersAboutFeed(Long userId, Integer feedId, Integer taskId) {
        // 获取所有关注该用户的用户列表
        List<FriendshipInfo> followers = friendshipMapper.selectFollowerList(userId, 0, 1000); // 简化：最多通知1000个粉丝

        UserInfo feedAuthor = userMapper.selectById(userId);
        String authorName = feedAuthor.getNickname() != null ? feedAuthor.getNickname() : feedAuthor.getUsername();

        for (FriendshipInfo friendship : followers) {
            Long followerId = friendship.getFollowerId();
            if (followerId == null || followerId.equals(userId)) {
                continue;
            }
            String title = "好友动态";
            String message = authorName + " 完成了新任务并发布了动态";
            createNotification(followerId, "feed_published", title, message, feedId);
        }
    }
}

