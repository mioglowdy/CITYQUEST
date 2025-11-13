package com.cityquest.mapper;

import com.cityquest.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatSessionMapper {

    ChatSession selectById(@Param("id") Long id);

    ChatSession selectByUsers(@Param("userAId") Long userAId, @Param("userBId") Long userBId);

    List<ChatSession> selectByUserId(@Param("userId") Long userId);

    int insert(ChatSession session);

    int update(ChatSession session);

    int updateUnreadCount(@Param("sessionId") Long sessionId, @Param("userId") Long userId, @Param("unreadCount") Integer unreadCount);

    int incrementUnreadCount(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}

