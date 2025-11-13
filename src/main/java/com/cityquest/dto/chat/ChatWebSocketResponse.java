package com.cityquest.dto.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatWebSocketResponse {
    private String type;              // chat, chat_ack, read_ack, error 等
    private ChatMessageDTO message;   // 消息内容
    private String error;             // 错误信息
}

