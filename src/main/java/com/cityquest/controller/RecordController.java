package com.cityquest.controller;

import com.cityquest.entity.RecordInfo;
import com.cityquest.entity.TaskInfo;
import com.cityquest.service.RecordService;
import com.cityquest.service.TaskService;
import com.cityquest.service.UserService;
import com.cityquest.service.QiniuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * 打卡记录控制器
 */
@RestController
@RequestMapping("/record")
public class RecordController {

    @Autowired
    private RecordService recordService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private QiniuService qiniuService;

    /**
     * 提交打卡记录
     */
    @PostMapping("/submit")
    public Map<String, Object> submitRecord(
            @RequestParam String userId,
            @RequestParam String taskId,
            @RequestParam String longitude,
            @RequestParam String latitude,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile photo) {
        
        System.out.println("========= 接收到打卡记录提交请求 =========");
        System.out.println("原始参数 - userId: " + userId + " (类型: " + (userId != null ? userId.getClass().getSimpleName() : "null") + ")");
        System.out.println("原始参数 - taskId: " + taskId + " (类型: " + (taskId != null ? taskId.getClass().getSimpleName() : "null") + ")");
        System.out.println("原始参数 - longitude: " + longitude + " (类型: " + (longitude != null ? longitude.getClass().getSimpleName() : "null") + ")");
        System.out.println("原始参数 - latitude: " + latitude + " (类型: " + (latitude != null ? latitude.getClass().getSimpleName() : "null") + ")");
        System.out.println("描述: " + description);
        System.out.println("是否有照片: " + (photo != null && !photo.isEmpty()));
        
        // 解析参数
        Long parsedUserId;
        Integer parsedTaskId;
        Double parsedLongitude;
        Double parsedLatitude;
        
        try {
            parsedUserId = Long.parseLong(userId);
            parsedTaskId = Integer.parseInt(taskId);
            parsedLongitude = Double.parseDouble(longitude);
            parsedLatitude = Double.parseDouble(latitude);
            
            System.out.println("解析后的参数:");
            System.out.println("  - userId: " + parsedUserId + " (Long类型)");
            System.out.println("  - taskId: " + parsedTaskId + " (Integer类型)");
            System.out.println("  - longitude: " + parsedLongitude + " (Double类型)");
            System.out.println("  - latitude: " + parsedLatitude + " (Double类型)");
            
            // 验证用户是否存在（在插入前检查）
            try {
                com.cityquest.entity.UserInfo userInfo = userService.getUserById(parsedUserId);
                if (userInfo == null) {
                    System.err.println("用户不存在 - userId: " + parsedUserId);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "用户不存在，userId: " + parsedUserId);
                    return response;
                }
                System.out.println("用户验证通过 - 用户名: " + userInfo.getUsername() + ", ID: " + userInfo.getId());
            } catch (Exception e) {
                System.err.println("验证用户时出错: " + e.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "用户验证失败: " + e.getMessage());
                return response;
            }
        } catch (NumberFormatException e) {
            System.err.println("参数解析失败: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "参数格式错误: " + e.getMessage());
            return response;
        }
        
        try {
            // 创建RecordInfo对象
            RecordInfo recordInfo = new RecordInfo();
            recordInfo.setUserId(parsedUserId);
            recordInfo.setTaskId(parsedTaskId);
            recordInfo.setLongitude(parsedLongitude);
            recordInfo.setLatitude(parsedLatitude);
            recordInfo.setDescription(description != null ? description : "");
            
            System.out.println("RecordInfo对象创建完成:");
            System.out.println("  - userId: " + recordInfo.getUserId());
            System.out.println("  - taskId: " + recordInfo.getTaskId());
            System.out.println("  - longitude: " + recordInfo.getLongitude());
            System.out.println("  - latitude: " + recordInfo.getLatitude());
            
            // 处理图片上传（使用七牛云）
            if (photo != null && !photo.isEmpty()) {
                System.out.println("========= 开始处理图片上传（七牛云） =========");
                try {
                    // 上传到七牛云
                    String photoUrl = qiniuService.uploadFile(photo, "records", null);
                    recordInfo.setPhotoUrl(photoUrl);
                    System.out.println("图片上传成功！");
                    System.out.println("  七牛云URL: " + photoUrl);
                    System.out.println("========= 图片上传处理完成 =========");
                } catch (Exception e) {
                    System.err.println("========= 图片上传失败 =========");
                    System.err.println("错误信息: " + e.getMessage());
                    e.printStackTrace();
                    System.err.println("========= 图片上传失败结束 =========");
                    throw new RuntimeException("图片上传失败: " + e.getMessage(), e);
                }
            } else {
                System.out.println("未提供图片文件");
            }
            
            boolean result = recordService.submitRecord(recordInfo);
            Map<String, Object> response = new HashMap<>();
            response.put("success", result);
            
            if (result) {
                // 获取任务信息以返回详细信息
                try {
                    TaskInfo taskInfo = taskService.getTaskById(parsedTaskId);
                    if (taskInfo != null) {
                        response.put("message", "打卡成功！积分已增加");
                        response.put("reward", taskInfo.getReward());
                        response.put("location", String.format("经度: %.6f, 纬度: %.6f", recordInfo.getLongitude(), recordInfo.getLatitude()));
                        response.put("taskTitle", taskInfo.getTitle());
                        response.put("address", taskInfo.getAddress());
                    } else {
                        response.put("message", "打卡成功！积分已增加");
                    }
                } catch (Exception e) {
                    System.err.println("获取任务信息失败: " + e.getMessage());
                    response.put("message", "打卡成功！积分已增加");
                }
            } else {
                response.put("message", "打卡失败");
            }
            
            System.out.println("打卡记录处理结果: " + result);
            System.out.println("========= 打卡请求处理完成 =========");
            return response;
        } catch (Exception e) {
            System.err.println("处理打卡记录时发生错误: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "处理请求时发生错误: " + e.getMessage());
            return response;
        }
    }

    /**
     * 获取用户的打卡记录
     */
    @GetMapping("/user/list")
    public Map<String, Object> getUserRecords(
            @RequestParam String userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long parsedUserId = parseLong(userId, "userId");
        return recordService.getUserRecords(parsedUserId, page, pageSize);
    }

    /**
     * 获取任务的打卡记录
     */
    @GetMapping("/task/list")
    public Map<String, Object> getTaskRecords(
            @RequestParam Integer taskId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return recordService.getTaskRecords(taskId, page, pageSize);
    }

    /**
     * 获取审核列表（管理员）
     */
    @GetMapping("/audit/list")
    public Map<String, Object> getAuditList(
            @RequestParam(defaultValue = "0") Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return recordService.getAuditList(status, page, pageSize);
    }

    /**
     * 审核打卡记录（管理员）
     */
    @PostMapping("/audit")
    public Map<String, Object> auditRecord(
            @RequestParam Integer id,
            @RequestParam Integer status,
            @RequestParam String remark,
            @RequestParam Integer adminId) {
        boolean result = recordService.auditRecord(id, status, remark, adminId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "审核成功" : "审核失败");
        return response;
    }

    /**
     * 检查用户是否已完成任务
     */
    @GetMapping("/check")
    public Map<String, Object> checkTaskCompletion(
            @RequestParam String userId,
            @RequestParam Integer taskId) {
        Long parsedUserId = parseLong(userId, "userId");
        boolean completed = recordService.checkTaskCompletion(parsedUserId, taskId);
        Map<String, Object> response = new HashMap<>();
        response.put("completed", completed);
        return response;
    }

    private Long parseLong(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " 参数不能为空");
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " 参数格式不正确，应为长整型: " + value);
        }
    }
}