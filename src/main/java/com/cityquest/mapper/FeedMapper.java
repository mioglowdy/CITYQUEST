package com.cityquest.mapper;

import com.cityquest.entity.UserFeedInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户动态Mapper接口
 */
@Mapper
public interface FeedMapper {
    /**
     * 发布动态
     */
    int insert(UserFeedInfo feedInfo);

    /**
     * 根据ID查询动态
     */
    UserFeedInfo selectById(@Param("id") Integer id);

    /**
     * 查询用户的动态列表
     */
    List<UserFeedInfo> selectByUserId(@Param("userId") Long userId, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * 查询公开动态列表（时间线）
     */
    List<UserFeedInfo> selectPublicFeedList(@Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * 查询好友动态列表
     */
    List<UserFeedInfo> selectFriendFeedList(@Param("userId") Long userId, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * 更新点赞数
     */
    int updateLikeCount(@Param("id") Integer id, @Param("increment") Integer increment);

    /**
     * 更新评论数
     */
    int updateCommentCount(@Param("id") Integer id, @Param("increment") Integer increment);

    /**
     * 删除动态
     */
    int delete(@Param("id") Integer id);

    /**
     * 查询动态总数
     */
    int selectCount(@Param("userId") Long userId, @Param("isPublic") Boolean isPublic);
}

