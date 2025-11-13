-- 聊天模块表结构变更

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY,
    user_a_id BIGINT NOT NULL COMMENT '用户A ID（较小的ID）',
    user_b_id BIGINT NOT NULL COMMENT '用户B ID（较大的ID）',
    last_message_id BIGINT COMMENT '最近一条消息ID',
    last_message_preview VARCHAR(255) COMMENT '最近消息预览',
    last_message_time DATETIME COMMENT '最近消息时间',
    unread_count_a INT DEFAULT 0 COMMENT '用户A未读数',
    unread_count_b INT DEFAULT 0 COMMENT '用户B未读数',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_chat_pair (user_a_id, user_b_id),
    FOREIGN KEY (user_a_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (user_b_id) REFERENCES user_info(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    content_type TINYINT DEFAULT 0 COMMENT '内容类型：0文本，1图片，2语音等',
    content TEXT NOT NULL COMMENT '消息内容',
    status TINYINT DEFAULT 0 COMMENT '状态：0已发送，1已送达，2已读，3撤回',
    extra TEXT COMMENT '扩展信息（JSON）',
    create_time DATETIME NOT NULL,
    FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES user_info(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_session_user_a ON chat_session(user_a_id);
CREATE INDEX IF NOT EXISTS idx_chat_session_user_b ON chat_session(user_b_id);
CREATE INDEX IF NOT EXISTS idx_chat_session_last_time ON chat_session(last_message_time);
CREATE INDEX IF NOT EXISTS idx_chat_message_session ON chat_message(session_id, create_time);
CREATE INDEX IF NOT EXISTS idx_chat_message_receiver ON chat_message(receiver_id, status);

