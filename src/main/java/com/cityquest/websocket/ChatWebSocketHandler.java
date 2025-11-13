package com.cityquest.websocket;

import com.cityquest.dto.chat.ChatMessageDTO;
import com.cityquest.dto.chat.ChatMessagePayload;
import com.cityquest.dto.chat.ChatWebSocketResponse;
import com.cityquest.entity.ChatMessage;
import com.cityquest.entity.ChatSession;
import com.cityquest.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final String USER_ID_ATTR = "userId";

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private ChatService chatService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get(USER_ID_ATTR);
        if (userId == null) {
            closeSession(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        userSessions.compute(userId, (k, v) -> {
            if (v == null) {
                Set<WebSocketSession> set = ConcurrentHashMap.newKeySet();
                set.add(session);
                return set;
            } else {
                v.add(session);
                return v;
            }
        });

        logger.info("WebSocket connected: userId={} sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get(USER_ID_ATTR);
        if (userId == null) {
            closeSession(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        ChatMessagePayload payload = objectMapper.readValue(message.getPayload(), ChatMessagePayload.class);
        if (payload.getType() == null) {
            return;
        }

        switch (payload.getType()) {
            case "chat":
                handleChatMessage(userId, session, payload);
                break;
            case "read":
                handleRead(userId, session, payload);
                break;
            case "ping":
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
                break;
            default:
                logger.warn("Unknown message type: {}", payload.getType());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket error: sessionId={}", session.getId(), exception);
        closeSession(session, CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(USER_ID_ATTR);
        if (userId != null) {
            userSessions.computeIfPresent(userId, (k, v) -> {
                v.remove(session);
                return v.isEmpty() ? null : v;
            });
            logger.info("WebSocket disconnected: userId={} sessionId={} status={}", userId, session.getId(), status);
        }
    }

    private void handleChatMessage(Long senderId, WebSocketSession session, ChatMessagePayload payload) throws IOException {
        if (payload.getReceiverId() == null) {
            sendError(session, "receiverId不能为空");
            return;
        }
        if (payload.getContent() == null || payload.getContent().trim().isEmpty()) {
            sendError(session, "消息内容不能为空");
            return;
        }

        ChatMessage chatMessage = chatService.sendMessage(
                senderId,
                payload.getReceiverId(),
                payload.getContentType(),
                payload.getContent(),
                payload.getExtra()
        );

        ChatMessageDTO dto = toDTO(chatMessage);

        // 发送 ack 给发送者
        ChatWebSocketResponse ack = ChatWebSocketResponse.builder()
                .type("chat_ack")
                .message(dto)
                .build();
        sendJson(session, ack);

        // 推送给接收者
        pushToUser(payload.getReceiverId(), ChatWebSocketResponse.builder()
                .type("chat")
                .message(dto)
                .build());
    }

    private void handleRead(Long userId, WebSocketSession session, ChatMessagePayload payload) throws IOException {
        if (payload.getSessionId() == null) {
            sendError(session, "sessionId不能为空");
            return;
        }

        chatService.markSessionMessagesAsRead(payload.getSessionId(), userId);
        ChatWebSocketResponse response = ChatWebSocketResponse.builder()
                .type("read_ack")
                .message(ChatMessageDTO.builder()
                        .sessionId(payload.getSessionId())
                        .senderId(userId)
                        .createTime(new Date())
                        .build())
                .build();
        sendJson(session, response);
    }

    private void pushToUser(Long userId, ChatWebSocketResponse response) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        sessions.forEach(ws -> {
            try {
                sendJson(ws, response);
            } catch (IOException e) {
                logger.error("Failed to push message to userId={} sessionId={}", userId, ws.getId(), e);
            }
        });
    }

    private void sendJson(WebSocketSession session, Object payload) throws IOException {
        String text = objectMapper.writeValueAsString(payload);
        session.sendMessage(new TextMessage(text));
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        ChatWebSocketResponse response = ChatWebSocketResponse.builder()
                .type("error")
                .error(message)
                .build();
        sendJson(session, response);
    }

    private ChatMessageDTO toDTO(ChatMessage message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .contentType(message.getContentType())
                .content(message.getContent())
                .status(message.getStatus())
                .extra(message.getExtra())
                .createTime(message.getCreateTime())
                .build();
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ignored) {
        }
    }
}

