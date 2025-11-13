-- 检查并修复 friendship_info 表的外键约束问题
-- 此脚本用于诊断和修复外键约束失败的问题

USE cityquest_db;

-- 1. 检查 friendship_info 表结构
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'cityquest_db'
    AND TABLE_NAME = 'friendship_info'
    AND COLUMN_NAME IN ('follower_id', 'followee_id')
ORDER BY COLUMN_NAME;

-- 2. 检查 user_info 表结构
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'cityquest_db'
    AND TABLE_NAME = 'user_info'
    AND COLUMN_NAME = 'id';

-- 3. 检查是否存在类型不匹配的问题
-- 如果 friendship_info.followee_id 是 INT，而 user_info.id 是 BIGINT，需要修改

-- 4. 修改 friendship_info 表的列类型（如果需要）
-- 注意：如果表中有数据，可能需要先删除无效的外键关系
ALTER TABLE friendship_info MODIFY COLUMN follower_id BIGINT NOT NULL;
ALTER TABLE friendship_info MODIFY COLUMN followee_id BIGINT NOT NULL;

-- 5. 检查是否存在无效的外键关系（followee_id 不在 user_info 中）
-- 查询无效的 followee_id
SELECT 
    fi.follower_id,
    fi.followee_id,
    fi.create_time
FROM friendship_info fi
LEFT JOIN user_info ui ON fi.followee_id = ui.id
WHERE ui.id IS NULL;

-- 6. 如果发现无效数据，可以选择删除（谨慎操作）
-- DELETE FROM friendship_info 
-- WHERE followee_id NOT IN (SELECT id FROM user_info);

-- 7. 验证修改后的表结构
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'cityquest_db'
    AND TABLE_NAME IN ('friendship_info', 'user_info')
    AND COLUMN_NAME IN ('id', 'follower_id', 'followee_id')
ORDER BY TABLE_NAME, COLUMN_NAME;

