-- 诊断用户外键问题
-- 检查 user_info 表中的所有用户ID
SELECT 
    id,
    username,
    nickname,
    role,
    status,
    create_time
FROM user_info
ORDER BY id;

-- 检查是否有大整数ID的用户
SELECT 
    id,
    username,
    CASE 
        WHEN id > 2147483647 THEN '大整数ID（超过INT范围）'
        ELSE '正常ID'
    END as id_type
FROM user_info
WHERE id > 2147483647;

-- 检查最近尝试插入的 user_id（如果有日志的话）
-- 这个查询可以帮助确认是哪个 user_id 导致的问题

-- 检查 record_info 表中的所有 user_id
SELECT DISTINCT user_id 
FROM record_info
ORDER BY user_id;

-- 检查哪些 user_id 在 record_info 中存在但在 user_info 中不存在（如果有数据的话）
SELECT DISTINCT r.user_id
FROM record_info r
LEFT JOIN user_info u ON r.user_id = u.id
WHERE u.id IS NULL;

