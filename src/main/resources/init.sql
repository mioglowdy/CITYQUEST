-- 创建数据库
CREATE DATABASE IF NOT EXISTS cityquest_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cityquest_db;

-- 创建用户表
CREATE TABLE IF NOT EXISTS user_info (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(100),
    avatar VARCHAR(255),
    points INT DEFAULT 0,
    role VARCHAR(20) DEFAULT 'user',
    email VARCHAR(100),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
);

-- 创建任务表
CREATE TABLE IF NOT EXISTS task_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    longitude DOUBLE,
    latitude DOUBLE,
    address VARCHAR(255),
    reward INT DEFAULT 0,
    type TINYINT DEFAULT 1,
    status TINYINT DEFAULT 1,
    create_by BIGINT,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    cover_image VARCHAR(255),
    completion_count INT DEFAULT 0,
    FOREIGN KEY (create_by) REFERENCES user_info(id)
);

-- 创建打卡记录表
CREATE TABLE IF NOT EXISTS record_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_id INT NOT NULL,
    photo_url VARCHAR(255),
    longitude DOUBLE,
    latitude DOUBLE,
    description TEXT,
    audit_status TINYINT DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    audit_remark TEXT,
    admin_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES user_info(id),
    FOREIGN KEY (task_id) REFERENCES task_info(id),
    FOREIGN KEY (admin_id) REFERENCES user_info(id)
);

-- 创建索引
CREATE INDEX idx_user_username ON user_info(username);
CREATE INDEX idx_user_role ON user_info(role);
CREATE INDEX idx_task_status ON task_info(status);
CREATE INDEX idx_task_type ON task_info(type);
CREATE INDEX idx_record_user ON record_info(user_id);
CREATE INDEX idx_record_task ON record_info(task_id);
CREATE INDEX idx_record_audit ON record_info(audit_status);

-- 插入管理员账号（使用固定的雪花算法ID：1，确保管理员ID始终为1）
-- 注意：实际生产环境应该使用雪花算法生成ID，这里为了兼容性使用固定值
INSERT INTO user_info (id, username, password, nickname, avatar, points, role, email, phone, status, create_time, update_time)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', null, 0, 'admin', 'admin@cityquest.com', '13800138000', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE password = 'e10adc3949ba59abbe56e057f20f883e';

-- 插入示例任务数据
INSERT INTO task_info (title, description, longitude, latitude, address, reward, type, status, create_by, create_time, update_time, cover_image, completion_count)
VALUES ('城市地标打卡', '在城市标志性建筑前拍照留念，展示城市魅力。', 116.397428, 39.90923, '北京市东城区故宫博物院', 100, 1, 1, 1, NOW(), NOW(), 'https://example.com/images/forbidden_city.jpg', 0),
       ('美食探索', '品尝当地特色美食，分享你的美食体验。', 116.407395, 39.915404, '北京市西城区前门大街', 80, 2, 1, 1, NOW(), NOW(), 'https://example.com/images/beijing_food.jpg', 0),
       ('公园休闲', '在城市公园放松心情，感受自然气息。', 116.418758, 39.999741, '北京市海淀区颐和园', 50, 3, 1, 1, NOW(), NOW(), 'https://example.com/images/summer_palace.jpg', 0);

-- ==================== 社交互动模块 ====================

-- 创建好友关系表
CREATE TABLE IF NOT EXISTS friendship_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL COMMENT '关注者ID',
    followee_id BIGINT NOT NULL COMMENT '被关注者ID',
    create_time DATETIME NOT NULL COMMENT '关注时间',
    FOREIGN KEY (follower_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (followee_id) REFERENCES user_info(id) ON DELETE CASCADE,
    UNIQUE KEY uk_friendship (follower_id, followee_id) COMMENT '防止重复关注'
);

-- 创建用户动态表
CREATE TABLE IF NOT EXISTS user_feed_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '发布用户ID',
    content TEXT COMMENT '动态内容',
    image_url VARCHAR(512) COMMENT '动态图片URL',
    task_id INT COMMENT '关联的任务ID（可选）',
    record_id INT COMMENT '关联的打卡记录ID（可选）',
    is_public BOOLEAN DEFAULT TRUE COMMENT '是否公开（true=公开，false=仅好友可见）',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    create_time DATETIME NOT NULL COMMENT '发布时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES task_info(id) ON DELETE SET NULL,
    FOREIGN KEY (record_id) REFERENCES record_info(id) ON DELETE SET NULL
);

-- 创建动态点赞表
CREATE TABLE IF NOT EXISTS feed_like_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    feed_id INT NOT NULL COMMENT '动态ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME NOT NULL COMMENT '点赞时间',
    FOREIGN KEY (feed_id) REFERENCES user_feed_info(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    UNIQUE KEY uk_feed_like (feed_id, user_id) COMMENT '防止重复点赞'
);

-- 创建动态评论表
CREATE TABLE IF NOT EXISTS feed_comment_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    feed_id INT NOT NULL COMMENT '动态ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id INT COMMENT '父评论ID（用于回复）',
    create_time DATETIME NOT NULL COMMENT '评论时间',
    FOREIGN KEY (feed_id) REFERENCES user_feed_info(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES feed_comment_info(id) ON DELETE CASCADE
);

-- 创建通知信息表
CREATE TABLE IF NOT EXISTS notification_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '接收通知的用户ID',
    type VARCHAR(50) NOT NULL COMMENT '通知类型（task_completed, task_audited, new_follower, feed_like, feed_comment等）',
    title VARCHAR(200) COMMENT '通知标题',
    message TEXT COMMENT '通知内容',
    related_id INT COMMENT '关联ID（任务ID、动态ID等）',
    read_status BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE
);

-- 创建社交模块索引
CREATE INDEX idx_friendship_follower ON friendship_info(follower_id);
CREATE INDEX idx_friendship_followee ON friendship_info(followee_id);
CREATE INDEX idx_feed_user ON user_feed_info(user_id);
CREATE INDEX idx_feed_public ON user_feed_info(is_public, create_time);
CREATE INDEX idx_feed_like_feed ON feed_like_info(feed_id);
CREATE INDEX idx_feed_like_user ON feed_like_info(user_id);
CREATE INDEX idx_feed_comment_feed ON feed_comment_info(feed_id);
CREATE INDEX idx_notification_user ON notification_info(user_id, read_status);
CREATE INDEX idx_notification_type ON notification_info(type);

-- ==================== 聊天模块 ====================

-- 会话表
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

-- 消息表
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

-- 聊天模块索引
CREATE INDEX idx_chat_session_user_a ON chat_session(user_a_id);
CREATE INDEX idx_chat_session_user_b ON chat_session(user_b_id);
CREATE INDEX idx_chat_session_last_time ON chat_session(last_message_time);
CREATE INDEX idx_chat_message_session ON chat_message(session_id, create_time);
CREATE INDEX idx_chat_message_receiver ON chat_message(receiver_id, status);
