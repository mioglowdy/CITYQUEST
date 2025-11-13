package com.cityquest.controller;

import com.cityquest.dto.chat.ChatMessageDTO;
import com.cityquest.dto.chat.ChatSendRequest;
import com.cityquest.dto.chat.ChatSessionDTO;
import com.cityquest.dto.chat.ChatSessionCreateRequest;
import com.cityquest.entity.ChatMessage;
import com.cityquest.entity.ChatSession;
import com.cityquest.service.ChatService;
import com.cityquest.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/sessions")
    public Map<String, Object> listSessions(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<ChatSession> sessions = chatService.listUserSessions(userId);
        List<ChatSessionDTO> dtoList = sessions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("list", dtoList);
        result.put("items", dtoList);
        return result;
    }

    @PostMapping("/sessions")
    public Map<String, Object> createSession(HttpServletRequest request, @RequestBody ChatSessionCreateRequest body) {
        Long userId = getUserId(request);
        if (body == null || body.getTargetUserId() == null) {
            throw new RuntimeException("targetUserId不能为空");
        }
        ChatSession session = chatService.getOrCreateSession(userId, body.getTargetUserId());
        ChatSessionDTO dto = toDTO(session);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("session", dto);
        return result;
    }

    @GetMapping("/messages")
    public Map<String, Object> listMessages(HttpServletRequest request,
                                            @RequestParam String sessionId,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = getUserId(request);
        
        // 处理sessionId参数（可能是Long或String类型）
        Long finalSessionId;
        try {
            finalSessionId = Long.parseLong(sessionId);
        } catch (NumberFormatException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无效的sessionId格式: " + sessionId);
            return result;
        }
        
        List<ChatMessage> messages = chatService.getSessionMessages(finalSessionId, userId, page, pageSize);
        List<ChatMessageDTO> dtoList = messages.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("list", dtoList);
        result.put("items", dtoList);
        return result;
    }

    @PostMapping("/messages")
    public Map<String, Object> sendMessage(HttpServletRequest request, @RequestBody ChatSendRequest sendRequest) {
        Long senderId = getUserId(request);
        if (sendRequest.getReceiverId() == null) {
            throw new RuntimeException("receiverId不能为空");
        }

        ChatMessage message = chatService.sendMessage(
                senderId,
                sendRequest.getReceiverId(),
                sendRequest.getContentType(),
                sendRequest.getContent(),
                sendRequest.getExtra()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", toDTO(message));
        return result;
    }

    @PostMapping("/sessions/{sessionId}/read")
    public Map<String, Object> markAsRead(HttpServletRequest request, @PathVariable String sessionId) {
        Long userId = getUserId(request);
        
        // 处理sessionId参数（可能是Long或String类型）
        Long finalSessionId;
        try {
            finalSessionId = Long.parseLong(sessionId);
        } catch (NumberFormatException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无效的sessionId格式: " + sessionId);
            return result;
        }
        
        chatService.markSessionMessagesAsRead(finalSessionId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @GetMapping("/mutual-follow")
    public Map<String, Object> isMutualFollow(HttpServletRequest request, @RequestParam Long targetUserId) {
        Long userId = getUserId(request);
        boolean mutual = chatService.isMutualFollow(userId, targetUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("mutual", mutual);
        return result;
    }

    private Long getUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未授权");
        }
        String token = authHeader.substring(7);
        String userIdStr = jwtUtil.getUserIdFromToken(token);
        if (userIdStr == null) {
            throw new RuntimeException("token无效");
        }
        return Long.parseLong(userIdStr);
    }

    private ChatSessionDTO toDTO(ChatSession session) {
        return ChatSessionDTO.builder()
                .id(session.getId())
                .userAId(session.getUserAId())
                .userBId(session.getUserBId())
                .lastMessageId(session.getLastMessageId())
                .lastMessagePreview(session.getLastMessagePreview())
                .lastMessageTime(session.getLastMessageTime())
                .unreadCountA(session.getUnreadCountA())
                .unreadCountB(session.getUnreadCountB())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
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
}

