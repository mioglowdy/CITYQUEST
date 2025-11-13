package com.cityquest.dto.chat;

import lombok.Data;

/**
 * WebSocket 消息请求载体
 */
@Data
public class ChatMessagePayload {
    private String type;          // chat、read、ping 等
    private Long sessionId;       // 会话ID（可选）
    private Long receiverId;      // 接收方ID
    private Integer contentType;  // 消息类型
    private String content;       // 消息内容
    private String extra;         // 附加信息（JSON）
}

