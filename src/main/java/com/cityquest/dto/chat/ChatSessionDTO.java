package com.cityquest.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ChatSessionDTO {
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

