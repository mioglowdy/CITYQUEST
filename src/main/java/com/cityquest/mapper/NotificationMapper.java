package com.cityquest.mapper;

import com.cityquest.entity.NotificationInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知信息Mapper接口
 */
@Mapper
public interface NotificationMapper {
    /**
     * 添加通知
     */
    int insert(NotificationInfo notificationInfo);

    /**
     * 根据ID查询通知
     */
    NotificationInfo selectById(@Param("id") Integer id);

    /**
     * 查询用户的通知列表
     */
    List<NotificationInfo> selectByUserId(@Param("userId") Long userId, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    /**
     * 标记为已读
     */
    int markAsRead(@Param("id") Integer id);

    /**
     * 批量标记为已读
     */
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 查询未读通知数
     */
    int selectUnreadCount(@Param("userId") Long userId);

    /**
     * 删除通知
     */
    int delete(@Param("id") Integer id);
}

