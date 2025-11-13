package com.cityquest.service;

import com.cityquest.entity.ChatMessage;
import com.cityquest.entity.ChatSession;

import java.util.List;

/**
 * 聊天服务接口
 */
public interface ChatService {

    /**
     * 获取或创建会话（会校验互相关注）。
     */
    ChatSession getOrCreateSession(Long userId, Long targetUserId);

    /**
     * 发送消息（互粉校验、持久化、未读数更新）。
     */
    ChatMessage sendMessage(Long senderId, Long receiverId, Integer contentType, String content, String extra);

    /**
     * 获取用户的会话列表。
     */
    List<ChatSession> listUserSessions(Long userId);

    /**
     * 分页获取会话消息，校验用户是否属于该会话。
     */
    List<ChatMessage> getSessionMessages(Long sessionId, Long userId, Integer page, Integer pageSize);

    /**
     * 将会话中针对该用户的消息标记为已读。
     */
    void markSessionMessagesAsRead(Long sessionId, Long userId);

    /**
     * 是否互相关注（供前端或其他服务调用）。
     */
    boolean isMutualFollow(Long userId, Long targetUserId);
}