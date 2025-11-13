package com.cityquest.mapper;

import com.cityquest.entity.FeedCommentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 动态评论Mapper接口
 */
@Mapper
public interface FeedCommentMapper {
    /**
     * 添加评论
     */
    int insert(FeedCommentInfo commentInfo);

    /**
     * 根据ID查询评论
     */
    FeedCommentInfo selectById(@Param("id") Integer id);

    /**
     * 查询动态的评论列表
     */
    List<FeedCommentInfo> selectByFeedId(@Param("feedId") Integer feedId, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * 删除评论
     */
    int delete(@Param("id") Integer id);

    /**
     * 查询评论总数
     */
    int selectCount(@Param("feedId") Integer feedId);

    /**
     * 根据动态ID删除所有评论
     */
    int deleteByFeedId(@Param("feedId") Integer feedId);
}

