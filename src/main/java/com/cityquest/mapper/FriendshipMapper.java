package com.cityquest.mapper;

import com.cityquest.entity.FriendshipInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 好友关系Mapper接口
 */
@Mapper
public interface FriendshipMapper {
    /**
     * 添加关注关系
     */
    int insert(FriendshipInfo friendshipInfo);

    /**
     * 取消关注
     */
    int delete(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    /**
     * 检查是否已关注
     */
    int checkFollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    /**
     * 查询用户的关注列表
     */
    List<FriendshipInfo> selectFollowingList(@Param("followerId") Long followerId, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * 查询用户的粉丝列表
     */
    List<FriendshipInfo> selectFollowerList(@Param("followeeId") Long followeeId, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * 查询关注数
     */
    int selectFollowingCount(@Param("followerId") Long followerId);

    /**
     * 查询粉丝数
     */
    int selectFollowerCount(@Param("followeeId") Long followeeId);
}

