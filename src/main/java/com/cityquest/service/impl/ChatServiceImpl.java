package com.cityquest.service.impl;

import com.cityquest.entity.ChatMessage;
import com.cityquest.entity.ChatSession;
import com.cityquest.entity.UserInfo;
import com.cityquest.mapper.ChatMessageMapper;
import com.cityquest.mapper.ChatSessionMapper;
import com.cityquest.mapper.UserMapper;
import com.cityquest.service.ChatService;
import com.cityquest.service.SocialService;
import com.cityquest.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 聊天服务实现
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private SocialService socialService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    @Transactional
    public ChatSession getOrCreateSession(Long userId, Long targetUserId) {
        validateUsers(userId, targetUserId);
        ensureMutualFollow(userId, targetUserId);

        SessionPair pair = normalizeUsers(userId, targetUserId);
        ChatSession session = chatSessionMapper.selectByUsers(pair.userA, pair.userB);
        if (session != null) {
            return session;
        }

        Date now = new Date();
        ChatSession newSession = new ChatSession();
        newSession.setId(snowflakeIdGenerator.nextId());
        newSession.setUserAId(pair.userA);
        newSession.setUserBId(pair.userB);
        newSession.setUnreadCountA(0);
        newSession.setUnreadCountB(0);
        newSession.setCreateTime(now);
        newSession.setUpdateTime(now);

        chatSessionMapper.insert(newSession);
        return newSession;
    }

    @Override
    @Transactional
    public ChatMessage sendMessage(Long senderId, Long receiverId, Integer contentType, String content, String extra) {
        validateUsers(senderId, receiverId);
        ensureMutualFollow(senderId, receiverId);

        ChatSession session = getOrCreateSession(senderId, receiverId);

        if (!Objects.equals(session.getUserAId(), senderId) && !Objects.equals(session.getUserBId(), senderId)) {
            throw new RuntimeException("当前会话不属于该用户");
        }

        Date now = new Date();

        ChatMessage message = new ChatMessage();
        message.setId(snowflakeIdGenerator.nextId());
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContentType(contentType != null ? contentType : 0);
        message.setContent(content);
        message.setStatus(0);
        message.setExtra(extra);
        message.setCreateTime(now);

        int inserted = chatMessageMapper.insert(message);
        if (inserted <= 0) {
            throw new RuntimeException("发送消息失败");
        }

        ChatSession updated = new ChatSession();
        updated.setId(session.getId());
        updated.setLastMessageId(message.getId());
        updated.setLastMessagePreview(generatePreview(message));
        updated.setLastMessageTime(now);
        updated.setUpdateTime(now);
        chatSessionMapper.update(updated);

        chatSessionMapper.updateUnreadCount(session.getId(), senderId, 0);
        chatSessionMapper.incrementUnreadCount(session.getId(), receiverId);

        return message;
    }

    @Override
    public List<ChatSession> listUserSessions(Long userId) {
        return chatSessionMapper.selectByUserId(userId);
    }

    @Override
    public List<ChatMessage> getSessionMessages(Long sessionId, Long userId, Integer page, Integer pageSize) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!Objects.equals(session.getUserAId(), userId) && !Objects.equals(session.getUserBId(), userId)) {
            throw new RuntimeException("无权访问该会话");
        }

        int pageNo = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : pageSize;
        int offset = (pageNo - 1) * size;

        List<ChatMessage> messages = chatMessageMapper.selectBySessionId(sessionId, offset, size);
        Collections.reverse(messages);
        return messages;
    }

    @Override
    @Transactional
    public void markSessionMessagesAsRead(Long sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!Objects.equals(session.getUserAId(), userId) && !Objects.equals(session.getUserBId(), userId)) {
            throw new RuntimeException("无权访问该会话");
        }

        List<ChatMessage> unreadList = chatMessageMapper.selectUnreadBySessionAndReceiver(sessionId, userId);
        if (!unreadList.isEmpty()) {
            List<Long> ids = new ArrayList<>(unreadList.size());
            for (ChatMessage message : unreadList) {
                ids.add(message.getId());
            }
            chatMessageMapper.updateStatusBatch(ids, 2);
            chatSessionMapper.updateUnreadCount(sessionId, userId, 0);
        }
    }

    @Override
    public boolean isMutualFollow(Long userId, Long targetUserId) {
        return socialService.isFollowing(userId, targetUserId)
                && socialService.isFollowing(targetUserId, userId);
    }

    private void validateUsers(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (userId.equals(targetUserId)) {
            throw new RuntimeException("不能与自己聊天");
        }
        UserInfo user = userMapper.selectById(userId);
        UserInfo target = userMapper.selectById(targetUserId);
        if (user == null || target == null) {
            throw new RuntimeException("用户不存在");
        }
    }

    private void ensureMutualFollow(Long userId, Long targetUserId) {
        if (!isMutualFollow(userId, targetUserId)) {
            throw new RuntimeException("双方互相关注后才能聊天");
        }
    }

    private SessionPair normalizeUsers(Long userId, Long targetUserId) {
        if (userId < targetUserId) {
            return new SessionPair(userId, targetUserId);
        } else {
            return new SessionPair(targetUserId, userId);
        }
    }

    private String generatePreview(ChatMessage message) {
        if (message.getContentType() == null || message.getContentType() == 0) {
            String content = message.getContent() == null ? "" : message.getContent();
            return content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }
        switch (message.getContentType()) {
            case 1:
                return "[图片]";
            case 2:
                return "[语音]";
            default:
                return "[消息]";
        }
    }

    private static class SessionPair {
        private final Long userA;
        private final Long userB;

        SessionPair(Long userA, Long userB) {
            this.userA = userA;
            this.userB = userB;
        }
    }
}

