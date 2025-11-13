-- 诊断 friendship_info 外键约束问题的脚本
-- 使用此脚本来检查数据库状态并识别问题

USE cityquest_db;

-- ==================== 步骤1: 检查表结构 ====================
-- 检查 friendship_info 表的列类型
SELECT 
    'friendship_info 表结构检查' AS check_type,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'cityquest_db'
    AND TABLE_NAME = 'friendship_info'
    AND COLUMN_NAME IN ('follower_id', 'followee_id')
ORDER BY COLUMN_NAME;

-- 检查 user_info 表的 id 列类型
SELECT 
    'user_info 表结构检查' AS check_type,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'cityquest_db'
    AND TABLE_NAME = 'user_info'
    AND COLUMN_NAME = 'id';

-- ==================== 步骤2: 检查数据类型是否匹配 ====================
-- 如果 friendship_info.follower_id 或 followee_id 是 INT，而 user_info.id 是 BIGINT
-- 需要修改表结构

-- ==================== 步骤3: 列出所有用户ID（用于参考） ====================
SELECT 
    '所有用户ID列表' AS check_type,
    id,
    username,
    nickname,
    create_time
FROM user_info
ORDER BY id;

-- ==================== 步骤4: 检查是否存在无效的外键关系 ====================
-- 查找 followee_id 不在 user_info 中的记录
SELECT 
    '无效的 followee_id' AS issue_type,
    fi.id AS friendship_id,
    fi.follower_id,
    fi.followee_id,
    fi.create_time
FROM friendship_info fi
LEFT JOIN user_info ui ON fi.followee_id = ui.id
WHERE ui.id IS NULL;

-- 查找 follower_id 不在 user_info 中的记录
SELECT 
    '无效的 follower_id' AS issue_type,
    fi.id AS friendship_id,
    fi.follower_id,
    fi.followee_id,
    fi.create_time
FROM friendship_info fi
LEFT JOIN user_info ui ON fi.follower_id = ui.id
WHERE ui.id IS NULL;

-- ==================== 步骤5: 修复方案 ====================
-- 如果表结构不匹配（INT vs BIGINT），执行以下SQL：

-- 方案A: 如果 friendship_info 表的列类型是 INT，需要改为 BIGINT
-- ALTER TABLE friendship_info MODIFY COLUMN follower_id BIGINT NOT NULL;
-- ALTER TABLE friendship_info MODIFY COLUMN followee_id BIGINT NOT NULL;

-- 方案B: 如果存在无效的外键关系，可以选择删除（谨慎操作）
-- DELETE FROM friendship_info 
-- WHERE followee_id NOT IN (SELECT id FROM user_info)
--    OR follower_id NOT IN (SELECT id FROM user_info);

-- ==================== 步骤6: 验证修复 ====================
-- 执行修复后，运行此查询验证表结构
SELECT 
    '修复后验证' AS check_type,
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'cityquest_db'
    AND TABLE_NAME IN ('friendship_info', 'user_info')
    AND COLUMN_NAME IN ('id', 'follower_id', 'followee_id')
ORDER BY TABLE_NAME, COLUMN_NAME;

