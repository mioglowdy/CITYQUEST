package com.cityquest.service;

import com.cityquest.entity.RecordInfo;

import java.util.Map;

/**
 * 打卡记录服务接口
 */
public interface RecordService {
    /**
     * 提交打卡记录
     */
    boolean submitRecord(RecordInfo recordInfo);

    /**
     * 获取用户的打卡记录
     */
    Map<String, Object> getUserRecords(Long userId, Integer page, Integer pageSize);

    /**
     * 获取任务的打卡记录
     */
    Map<String, Object> getTaskRecords(Integer taskId, Integer page, Integer pageSize);

    /**
     * 获取审核列表
     */
    Map<String, Object> getAuditList(Integer status, Integer page, Integer pageSize);

    /**
     * 审核打卡记录
     */
    boolean auditRecord(Integer id, Integer status, String remark, Integer adminId);

    /**
     * 检查用户是否已完成任务
     */
    boolean checkTaskCompletion(Long userId, Integer taskId);

    /**
     * 删除单条记录（仅允许删除已通过记录）
     */
    boolean deleteApprovedRecord(Integer id);

    /**
     * 批量删除记录（仅允许删除已通过记录）
     */
    int batchDeleteApprovedRecords(java.util.List<Integer> ids);
}