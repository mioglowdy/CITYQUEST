package com.cityquest.mapper;

import com.cityquest.entity.FeedLikeInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 动态点赞Mapper接口
 */
@Mapper
public interface FeedLikeMapper {
    /**
     * 添加点赞
     */
    int insert(FeedLikeInfo likeInfo);

    /**
     * 取消点赞
     */
    int delete(@Param("feedId") Integer feedId, @Param("userId") Long userId);

    /**
     * 检查是否已点赞
     */
    int checkLike(@Param("feedId") Integer feedId, @Param("userId") Long userId);

    /**
     * 根据动态ID删除所有点赞记录
     */
    int deleteByFeedId(@Param("feedId") Integer feedId);
}

