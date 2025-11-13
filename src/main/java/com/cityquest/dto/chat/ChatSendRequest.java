package com.cityquest.dto.chat;

import lombok.Data;

@Data
public class ChatSendRequest {
    private Long sessionId;
    private Long receiverId;
    private Integer contentType;
    private String content;
    private String extra;
}

