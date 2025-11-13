package com.cityquest.entity;

import lombok.Data;

import java.util.Date;

/**
 * 聊天会话实体
 */
@Data
public class ChatSession {
    private Long id;
    private Long userAId;
    private Long userBId;
    private Long lastMessageId;
    private String lastMessagePreview;
    private Date lastMessageTime;
    private Integer unreadCountA;
    private Integer unreadCountB;
    private Date createTime;
    private Date updateTime;
}

