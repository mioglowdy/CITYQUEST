package com.cityquest.service.impl;

import com.cityquest.entity.NotificationInfo;
import com.cityquest.entity.RecordInfo;
import com.cityquest.entity.TaskInfo;
import com.cityquest.mapper.RecordMapper;
import com.cityquest.mapper.TaskMapper;
import com.cityquest.mapper.UserMapper;
import com.cityquest.mapper.NotificationMapper;
import com.cityquest.service.RecordService;
import com.cityquest.util.DistanceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 打卡记录服务实现类
 */
@Service
public class RecordServiceImpl implements RecordService {

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    @Transactional
    public boolean submitRecord(RecordInfo recordInfo) {
        System.out.println("========= 开始处理打卡记录提交 =========");
        System.out.println("用户ID: " + recordInfo.getUserId() + " (类型: " + (recordInfo.getUserId() != null ? recordInfo.getUserId().getClass().getSimpleName() : "null") + ")");
        System.out.println("任务ID: " + recordInfo.getTaskId());
        
        // 验证用户是否存在
        try {
            Long userId = recordInfo.getUserId();
            System.out.println("RecordService - 准备查询用户，userId: " + userId + " (类型: " + (userId != null ? userId.getClass().getSimpleName() : "null") + ")");
            com.cityquest.entity.UserInfo userInfo = userMapper.selectById(userId);
            if (userInfo == null) {
                System.err.println("用户不存在 - userId: " + userId);
                // 尝试直接查询数据库验证
                System.err.println("尝试直接SQL查询验证用户是否存在...");
                throw new RuntimeException("用户不存在，userId: " + userId);
            }
            System.out.println("用户验证通过 - 用户名: " + userInfo.getUsername() + ", ID: " + userInfo.getId() + " (类型: " + (userInfo.getId() != null ? userInfo.getId().getClass().getSimpleName() : "null") + ")");
        } catch (Exception e) {
            System.err.println("验证用户时出错: " + e.getMessage());
            System.err.println("异常类型: " + e.getClass().getName());
            e.printStackTrace();
            throw new RuntimeException("用户验证失败: " + e.getMessage(), e);
        }
        
        // 检查用户是否已完成该任务
        if (checkTaskCompletion(recordInfo.getUserId(), recordInfo.getTaskId())) {
            throw new RuntimeException("您已完成该任务");
        }
        
        // 验证位置：检查用户是否在任务地点允许范围内
        TaskInfo taskInfo = taskMapper.selectById(recordInfo.getTaskId());
        if (taskInfo == null) {
            throw new RuntimeException("任务不存在");
        }
        
        // 验证位置信息
        if (recordInfo.getLatitude() == null || recordInfo.getLongitude() == null) {
            throw new RuntimeException("位置信息不完整，请重新获取位置");
        }
        
        if (taskInfo.getLatitude() == null || taskInfo.getLongitude() == null) {
            throw new RuntimeException("任务位置信息不完整");
        }
        
        // 计算距离
        double distance = DistanceUtil.calculateDistance(
            recordInfo.getLatitude(), 
            recordInfo.getLongitude(),
            taskInfo.getLatitude(), 
            taskInfo.getLongitude()
        );
        
        System.out.println("位置验证 - 用户位置: (" + recordInfo.getLatitude() + ", " + recordInfo.getLongitude() + ")");
        System.out.println("位置验证 - 任务位置: (" + taskInfo.getLatitude() + ", " + taskInfo.getLongitude() + ")");
        System.out.println("位置验证 - 距离: " + String.format("%.2f", distance) + "米");
        
        // 允许的最大误差范围：2000米（2公里）
        double maxAllowedDistance = 2000.0;
        boolean isWithinRange = distance <= maxAllowedDistance;
        
        // 设置记录信息
        recordInfo.setAuditStatus(0); // 初始状态为待审核
        recordInfo.setCreateTime(new Date());
        recordInfo.setUpdateTime(new Date());
        
        // 插入打卡记录（会自动设置recordInfo.id）
        System.out.println("准备插入打卡记录到数据库:");
        System.out.println("  - userId: " + recordInfo.getUserId());
        System.out.println("  - taskId: " + recordInfo.getTaskId());
        System.out.println("  - longitude: " + recordInfo.getLongitude());
        System.out.println("  - latitude: " + recordInfo.getLatitude());
        System.out.println("  - description: " + recordInfo.getDescription());
        System.out.println("  - photoUrl: " + recordInfo.getPhotoUrl());
        System.out.println("  - auditStatus: " + recordInfo.getAuditStatus());
        System.out.println("  - createTime: " + recordInfo.getCreateTime());
        System.out.println("  - updateTime: " + recordInfo.getUpdateTime());
        
        try {
            int insertResult = recordMapper.insert(recordInfo);
            System.out.println("数据库插入操作返回结果: " + insertResult);
            
            if (insertResult <= 0) {
                System.err.println("打卡记录插入失败 - insertResult: " + insertResult);
                return false;
            }
            
            System.out.println("打卡记录插入成功，记录ID: " + recordInfo.getId());
            System.out.println("地点信息 - 经度: " + recordInfo.getLongitude() + ", 纬度: " + recordInfo.getLatitude());
        } catch (Exception e) {
            System.err.println("========= 数据库插入异常 =========");
            System.err.println("错误信息: " + e.getMessage());
            System.err.println("错误类型: " + e.getClass().getName());
            e.printStackTrace();
            System.err.println("========= 数据库插入异常结束 =========");
            throw new RuntimeException("数据库插入失败: " + e.getMessage(), e);
        }
        
        // 根据距离判断是否需要管理员审核
        if (isWithinRange) {
            // 距离在允许范围内：自动审核通过并给予积分
            System.out.println("位置验证通过: 距离 " + String.format("%.2f", distance) + " 米，在允许范围内，自动审核通过");
            
            if (taskInfo != null && taskInfo.getReward() != null && taskInfo.getReward() > 0) {
                System.out.println("任务奖励积分: " + taskInfo.getReward());
                // 自动审核通过（状态1表示审核通过）
                int auditResult = recordMapper.updateAuditStatus(recordInfo.getId(), 1, "自动审核通过：位置在允许范围内", null);
                System.out.println("审核状态更新结果: " + auditResult);
                
                // 更新用户积分
                int pointsResult = userMapper.updatePoints(recordInfo.getUserId().longValue(), taskInfo.getReward());
                System.out.println("积分更新结果: " + pointsResult);
                
                // 更新任务完成数
                taskMapper.updateCompletionCount(taskInfo.getId());
                System.out.println("任务完成数已更新");
            } else {
                System.out.println("任务信息为空或奖励积分为0，跳过积分更新");
            }
        } else {
            // 距离超过允许范围：需要管理员审核
            String distanceRemark = String.format("距离任务地点 %.0f 米，超过允许范围（2000米），需要管理员审核", distance);
            System.out.println("位置验证: 距离 " + String.format("%.2f", distance) + " 米，超过允许范围，已提交待审核");
            System.out.println("打卡记录ID: " + recordInfo.getId() + ", 审核状态: " + recordInfo.getAuditStatus());
            // 记录距离信息到审核备注中（但不更新审核状态，保持为0-待审核）
            recordInfo.setAuditRemark(distanceRemark);
            int updateResult = recordMapper.updateAuditStatus(recordInfo.getId(), 0, distanceRemark, null);
            System.out.println("更新审核备注结果: " + updateResult + ", 记录ID: " + recordInfo.getId());
            System.out.println("打卡记录已提交，等待管理员审核，状态应为0（待审核）");
        }
        
        return true;
    }

    @Override
    public Map<String, Object> getUserRecords(Long userId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<RecordInfo> recordList = recordMapper.selectByUserId(userId, offset, pageSize);
        int total = recordMapper.selectCount(userId, null, null);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", recordList);
        result.put("records", recordList); // 兼容前端需要的records字段
        result.put("total", total);
        return result;
    }

    @Override
    public Map<String, Object> getTaskRecords(Integer taskId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<RecordInfo> recordList = recordMapper.selectByTaskId(taskId, offset, pageSize);
        int total = recordMapper.selectCount(null, taskId, null);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", recordList);
        result.put("records", recordList); // 兼容前端需要的records字段
        result.put("total", total);
        return result;
    }

    @Override
    public Map<String, Object> getAuditList(Integer status, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        System.out.println("查询审核列表 - status: " + status + ", page: " + page + ", pageSize: " + pageSize + ", offset: " + offset);
        List<RecordInfo> recordList = recordMapper.selectAuditList(status, offset, pageSize);
        int total = recordMapper.selectCount(null, null, status);
        System.out.println("查询审核列表 - 找到 " + recordList.size() + " 条记录，总数: " + total);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", recordList);
        result.put("records", recordList); // 兼容前端需要的records字段
        result.put("total", total);
        return result;
    }

    @Override
    @Transactional
    public boolean auditRecord(Integer id, Integer status, String remark, Integer adminId) {
        // 获取打卡记录
        RecordInfo recordInfo = recordMapper.selectById(id);
        if (recordInfo == null) {
            throw new RuntimeException("打卡记录不存在");
        }

        // 获取任务信息用于通知及积分处理
        TaskInfo taskInfo = taskMapper.selectById(recordInfo.getTaskId());
        
        // 更新审核状态
        int result = recordMapper.updateAuditStatus(id, status, remark, adminId != null ? adminId.longValue() : null);
        
        // 如果审核通过，给用户加积分
        if (status == 1) {
            if (taskInfo != null) {
                // 更新用户积分
                userMapper.updatePoints(recordInfo.getUserId().longValue(), taskInfo.getReward());
                // 更新任务完成数
                taskMapper.updateCompletionCount(taskInfo.getId());
            }
        }

        if (result > 0) {
            sendAuditNotification(recordInfo, taskInfo, status, remark);
        }
        
        return result > 0;
    }

    /**
     * 将审核结果发送到用户通知
     */
    private void sendAuditNotification(RecordInfo recordInfo, TaskInfo taskInfo, Integer status, String remark) {
        if (recordInfo == null || recordInfo.getUserId() == null) {
            return;
        }

        String taskTitle = taskInfo != null ? taskInfo.getTitle() : null;
        String statusText = status != null && status == 1 ? "审核通过" : "审核未通过";
        String title = "打卡" + statusText;

        StringBuilder messageBuilder = new StringBuilder("您提交的打卡");
        if (taskTitle != null && !taskTitle.isEmpty()) {
            messageBuilder.append("《").append(taskTitle).append("》");
        }
        messageBuilder.append(status != null && status == 1 ? "已通过审核" : "未通过审核");

        String finalRemark = remark != null ? remark.trim() : null;
        if (finalRemark != null && !finalRemark.isEmpty()) {
            messageBuilder.append("。审核说明：").append(finalRemark);
        }

        NotificationInfo notification = new NotificationInfo();
        notification.setUserId(recordInfo.getUserId());
        notification.setType("record_audit");
        notification.setTitle(title);
        notification.setMessage(messageBuilder.toString());
        notification.setRelatedId(recordInfo.getTaskId());
        notification.setReadStatus(false);
        notification.setCreateTime(new Date());

        notificationMapper.insert(notification);
    }

    @Override
    public boolean checkTaskCompletion(Long userId, Integer taskId) {
        return recordMapper.checkUserTaskCompletion(userId.longValue(), taskId) > 0;
    }

    @Override
    @Transactional
    public boolean deleteApprovedRecord(Integer id) {
        RecordInfo record = recordMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("记录不存在");
        }
        if (record.getAuditStatus() == null || record.getAuditStatus() != 1) {
            throw new RuntimeException("仅允许删除已通过的记录");
        }
        return recordMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public int batchDeleteApprovedRecords(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        // 过滤出已通过的记录ID
        java.util.List<Integer> approvedIds = new java.util.ArrayList<>();
        for (Integer id : ids) {
            RecordInfo r = recordMapper.selectById(id);
            if (r != null && r.getAuditStatus() != null && r.getAuditStatus() == 1) {
                approvedIds.add(id);
            }
        }
        if (approvedIds.isEmpty()) return 0;
        return recordMapper.batchDelete(approvedIds);
    }
}