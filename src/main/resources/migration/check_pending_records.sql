-- 检查待审核记录
-- 查询所有待审核记录（audit_status = 0）
SELECT 
    id,
    user_id,
    task_id,
    audit_status,
    audit_remark,
    create_time,
    update_time,
    photo_url
FROM record_info
WHERE audit_status = 0
ORDER BY create_time DESC;

-- 统计待审核记录数量
SELECT COUNT(*) as pending_count
FROM record_info
WHERE audit_status = 0;

-- 查看所有审核状态分布
SELECT 
    audit_status,
    CASE 
        WHEN audit_status = 0 THEN '待审核'
        WHEN audit_status = 1 THEN '已通过'
        WHEN audit_status = 2 THEN '已拒绝'
        ELSE '未知'
    END as status_name,
    COUNT(*) as count
FROM record_info
GROUP BY audit_status;

