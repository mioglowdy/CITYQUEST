package com.cityquest.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ChatMessageDTO {
    private Long id;
    private Long sessionId;
    private Long senderId;
    private Long receiverId;
    private Integer contentType;
    private String content;
    private Integer status;
    private String extra;
    private Date createTime;
}

