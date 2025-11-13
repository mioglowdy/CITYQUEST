package com.cityquest.entity;

import lombok.Data;

import java.util.Date;

/**
 * 聊天消息实体
 */
@Data
public class ChatMessage {
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

