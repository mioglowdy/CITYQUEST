package com.cityquest.mapper;

import com.cityquest.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ChatMessageMapper {

    int insert(ChatMessage message);

    List<ChatMessage> selectBySessionId(@Param("sessionId") Long sessionId,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);

    ChatMessage selectLastMessage(@Param("sessionId") Long sessionId);

    List<ChatMessage> selectUnreadByReceiver(@Param("receiverId") Long receiverId,
                                             @Param("afterTime") Date afterTime);

    List<ChatMessage> selectUnreadBySessionAndReceiver(@Param("sessionId") Long sessionId,
                                                       @Param("receiverId") Long receiverId);

    int updateStatusBatch(@Param("ids") List<Long> ids, @Param("status") Integer status);
}

