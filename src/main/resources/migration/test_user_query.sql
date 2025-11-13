-- 测试查询用户 ID: 245047534056116220
-- 方法1: 直接查询
SELECT * FROM user_info WHERE id = 245047534056116220;

-- 方法2: 使用 CAST 查询（模拟 MyBatis 可能的行为）
SELECT * FROM user_info WHERE id = CAST(245047534056116220 AS UNSIGNED);

-- 方法3: 使用字符串转换（检查是否有类型问题）
SELECT * FROM user_info WHERE id = CAST('245047534056116220' AS UNSIGNED);

-- 方法4: 查看所有用户ID（确认该ID是否存在）
SELECT id, username, nickname FROM user_info ORDER BY id;

-- 方法5: 检查该ID是否在范围内
SELECT 
    CASE 
        WHEN 245047534056116220 BETWEEN 0 AND 9223372036854775807 THEN 'ID在BIGINT范围内'
        ELSE 'ID超出BIGINT范围'
    END as id_check;

-- 方法6: 检查数据库字段类型
SHOW COLUMNS FROM user_info WHERE Field = 'id';

