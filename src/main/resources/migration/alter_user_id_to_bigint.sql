-- 将 user_info 表的 id 字段从 INT 改为 BIGINT，以支持雪花算法生成的64位ID
-- 注意：执行此脚本前请备份数据库！

USE cityquest_db;

-- 1. 修改 user_info 表的 id 字段为 BIGINT
ALTER TABLE user_info MODIFY COLUMN id BIGINT NOT NULL;

-- 2. 修改所有外键关联表的 user_id 字段为 BIGINT
ALTER TABLE task_info MODIFY COLUMN create_by BIGINT;
ALTER TABLE record_info MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE record_info MODIFY COLUMN admin_id BIGINT;
ALTER TABLE friendship_info MODIFY COLUMN follower_id BIGINT NOT NULL;
ALTER TABLE friendship_info MODIFY COLUMN followee_id BIGINT NOT NULL;
ALTER TABLE user_feed_info MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE feed_like_info MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE feed_comment_info MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE notification_info MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE notification_info MODIFY COLUMN from_user_id BIGINT;

-- 3. 移除 AUTO_INCREMENT 属性（因为现在使用雪花算法生成ID）
-- 注意：如果表中已有数据，需要先处理现有数据的ID
-- 这里假设是新系统或可以接受现有ID保持不变

-- 4. 验证修改
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'cityquest_db'
    AND COLUMN_NAME IN ('id', 'user_id', 'create_by', 'admin_id', 'follower_id', 'followee_id', 'from_user_id')
ORDER BY TABLE_NAME, COLUMN_NAME;

