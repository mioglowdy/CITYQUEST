package com.cityquest.controller;

import com.cityquest.entity.TaskInfo;
import com.cityquest.entity.UserInfo;
import com.cityquest.entity.ProductInfo;
import com.cityquest.entity.ProductCategory;
import com.cityquest.entity.ExchangeOrder;
import com.cityquest.service.UserService;
import com.cityquest.service.RecordService;
import com.cityquest.service.TaskService;
import com.cityquest.service.OnlineUserService;
import com.cityquest.service.ProductService;
import com.cityquest.service.ProductCategoryService;
import com.cityquest.service.ExchangeOrderService;
import com.cityquest.service.QiniuService;
import com.cityquest.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理后台控制器
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private OnlineUserService onlineUserService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.cityquest.mapper.RecordMapper recordMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductCategoryService categoryService;

    @Autowired
    private ExchangeOrderService orderService;

    @Autowired
    private QiniuService qiniuService;

    

    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 获取用户总数
            Map<String, Object> userList = userService.getUserList(1, 1, null, null, null);
            int totalUsers = ((Number) userList.getOrDefault("total", 0)).intValue();

            // 获取任务总数
            Map<String, Object> taskList = taskService.getTaskList(null, null, 1, 1, null, null);
            int totalTasks = ((Number) taskList.getOrDefault("total", 0)).intValue();

            // 获取记录总数（所有状态的记录）
            Map<String, Object> recordList = recordService.getAuditList(null, 1, 1);
            int totalRecords = ((Number) recordList.getOrDefault("total", 0)).intValue();

            // 获取待审核数量（status=0表示待审核）
            Map<String, Object> pendingList = recordService.getAuditList(0, 1, 1);
            int pendingAudits = ((Number) pendingList.getOrDefault("total", 0)).intValue();

            response.put("totalUsers", totalUsers);
            response.put("totalTasks", totalTasks);
            response.put("totalRecords", totalRecords);
            response.put("pendingAudits", pendingAudits);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    

    

    /**
     * 获取任务类型分布
     */
    @GetMapping("/statistics/task-types")
    public List<Map<String, Object>> getTaskTypeDistribution() {
        try {
            // 从数据库查询各类型任务数量
            List<Map<String, Object>> distribution = new java.util.ArrayList<>();
            String[] typeNames = {"历史文化", "自然风光", "美食探索", "艺术体验", "科技创新"};
            
            for (int i = 1; i <= 5; i++) {
                Map<String, Object> taskList = taskService.getTaskList(i, null, 1, 1, null, null);
                int count = ((Number) taskList.getOrDefault("total", 0)).intValue();
                Map<String, Object> item = new HashMap<>();
                item.put("value", count);
                item.put("name", typeNames[i - 1]);
                distribution.add(item);
            }
            
            return distribution;
        } catch (Exception e) {
            // 出错时返回默认数据
            return List.of(
                Map.of("value", 0, "name", "历史文化"),
                Map.of("value", 0, "name", "自然风光"),
                Map.of("value", 0, "name", "美食探索"),
                Map.of("value", 0, "name", "艺术体验"),
                Map.of("value", 0, "name", "科技创新")
            );
        }
    }

    /**
     * 获取最近活动（来源数据库记录）
     */
    @GetMapping("/activities/recent")
    public List<Map<String, Object>> getRecentActivities() {
        try {
            // 取最近的审核列表（不限状态），作为最近活动数据源
            Map<String, Object> recordList = recordService.getAuditList(null, 1, 20);
            @SuppressWarnings("unchecked")
            List<com.cityquest.entity.RecordInfo> records = (List<com.cityquest.entity.RecordInfo>) recordList.get("list");
            if (records == null) return List.of();

            List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (com.cityquest.entity.RecordInfo record : records) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", record.getId());
                // 用户名
                String username = "用户" + record.getUserId();
                try {
                    com.cityquest.entity.UserInfo u = userService.getUserById(record.getUserId().longValue());
                    if (u != null && u.getUsername() != null) username = u.getUsername();
                } catch (Exception ignored) {}
                item.put("username", username);

                // 任务标题
                String taskTitle = "任务" + record.getTaskId();
                try {
                    com.cityquest.entity.TaskInfo t = taskService.getTaskById(record.getTaskId());
                    if (t != null && t.getTitle() != null) taskTitle = t.getTitle();
                } catch (Exception ignored) {}
                item.put("task", taskTitle);

                // 动作文案：根据审核状态
                // 0 待审核、1 已通过、2 已拒绝（按业务约定）
                String action = "提交了打卡";
                Integer st = record.getAuditStatus();
                if (st != null) {
                    if (st == 1) action = "打卡审核通过";
                    else if (st == 2) action = "打卡被拒绝";
                }
                item.put("action", action);

                // 时间
                item.put("time", record.getCreateTime());

                items.add(item);
            }

            return items;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 获取任务列表（管理后台）
     */
    @GetMapping("/tasks")
    public Map<String, Object> getTaskList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            // 转换status参数
            Integer statusInt = null;
            if (status != null && !status.isEmpty()) {
                if ("active".equals(status)) {
                    statusInt = 1;
                } else if ("inactive".equals(status)) {
                    statusInt = 0;
                }
            }
            
            Map<String, Object> result = taskService.getTaskList(null, statusInt, page, size, search, null);
            
            // 如果前端需要items字段
            if (result.containsKey("list")) {
                result.put("items", result.get("list"));
            }
            
            return result;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("items", List.of());
            response.put("total", 0);
            return response;
        }
    }

    /**
     * 创建任务
     */
    @PostMapping("/tasks")
    public Map<String, Object> createTask(@RequestBody TaskInfo taskInfo, javax.servlet.http.HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 默认值填充
            if (taskInfo.getStatus() == null) {
                taskInfo.setStatus(0); // 默认关闭
            }
            if (taskInfo.getType() == null) {
                taskInfo.setType(0); // 默认类型
            }
            if (taskInfo.getCreateBy() == null) {
                try {
                    String token = request.getHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
                    if (token != null && !token.isEmpty()) {
                        String uid = jwtUtil.getUserIdFromToken(token);
                        if (uid != null) taskInfo.setCreateBy(Long.parseLong(uid));
                    }
                } catch (Exception ignored) {}
            }
            boolean result = taskService.createTask(taskInfo);
            if (result) {
                response.put("success", true);
                response.put("message", "创建成功");
                response.put("data", taskInfo);
            } else {
                response.put("success", false);
                response.put("message", "创建失败");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 上传任务图片（使用七牛云）
     */
    @PostMapping(value = "/tasks/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadTaskImage(@RequestParam("file") MultipartFile file, javax.servlet.http.HttpServletRequest request) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if (file == null || file.isEmpty()) {
                resp.put("success", false);
                resp.put("message", "文件为空");
                return resp;
            }

            // 上传到七牛云
            String url = qiniuService.uploadFile(file, "tasks", null);
            
            // 从URL中提取文件名（用于兼容前端）
            String filename = url.substring(url.lastIndexOf('/') + 1);

            resp.put("success", true);
            resp.put("url", url);
            resp.put("filename", filename);
            return resp;
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", "上传失败: " + e.getMessage());
            e.printStackTrace();
            return resp;
        }
    }

    /**
     * 更新任务
     */
    @PutMapping("/tasks/update")
    public Map<String, Object> updateTask(@RequestBody TaskInfo taskInfo) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean result = taskService.updateTask(taskInfo);
            if (result) {
                TaskInfo updated = taskService.getTaskById(taskInfo.getId());
                response.put("success", true);
                response.put("data", updated);
            } else {
                response.put("success", false);
                response.put("message", "更新失败");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/tasks/{id}")
    public Map<String, Object> deleteTask(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean result = taskService.deleteTask(id);
            response.put("success", result);
            response.put("message", result ? "删除成功" : "删除失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 更新任务状态
     */
    @PatchMapping("/tasks/{id}/status")
    public Map<String, Object> updateTaskStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            TaskInfo task = taskService.getTaskById(id);
            if (task != null) {
                String status = body.get("status");
                if ("active".equals(status)) {
                    task.setStatus(1);
                } else if ("inactive".equals(status)) {
                    task.setStatus(0);
                }
                boolean result = taskService.updateTask(task);
                response.put("success", result);
                response.put("message", result ? "更新成功" : "更新失败");
            } else {
                response.put("success", false);
                response.put("message", "任务不存在");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 批量更新任务状态
     */
    @PatchMapping("/tasks/batch/status")
    public Map<String, Object> batchUpdateTaskStatus(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) body.get("ids");
            String status = (String) body.get("status");
            
            Integer statusInt = null;
            if ("active".equals(status)) {
                statusInt = 1;
            } else if ("inactive".equals(status)) {
                statusInt = 0;
            }
            
            int successCount = 0;
            for (Integer id : ids) {
                TaskInfo task = taskService.getTaskById(id);
                if (task != null && statusInt != null) {
                    task.setStatus(statusInt);
                    if (taskService.updateTask(task)) {
                        successCount++;
                    }
                }
            }
            
            response.put("success", true);
            response.put("message", "批量更新成功");
            response.put("successCount", successCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 批量删除任务
     */
    @DeleteMapping("/tasks/batch")
    public Map<String, Object> batchDeleteTasks(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) body.get("ids");
            
            int successCount = 0;
            for (Integer id : ids) {
                if (taskService.deleteTask(id)) {
                    successCount++;
                }
            }
            
            response.put("success", true);
            response.put("message", "批量删除成功");
            response.put("successCount", successCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 导入任务
     */
    @PostMapping("/tasks/import")
    public Map<String, Object> importTasks(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                           HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file == null || file.isEmpty()) {
                response.put("success", false);
                response.put("message", "文件不能为空");
                return response;
            }
            
            try (InputStream inputStream = file.getInputStream()) {
                Integer adminId = getCurrentAdminId(request);
                com.cityquest.dto.task.TaskImportResult result =
                        taskService.importTasks(inputStream, file.getOriginalFilename(),
                                adminId != null ? adminId.longValue() : null);

                boolean allSuccessful = result.getFailedCount() == 0 && result.getSuccessCount() > 0;
                response.put("success", allSuccessful);
                response.put("message", String.format("导入完成：成功 %d 条，失败 %d 条",
                        result.getSuccessCount(), result.getFailedCount()));
                response.put("successCount", result.getSuccessCount());
                response.put("failedCount", result.getFailedCount());
                response.put("totalRows", result.getTotalRows());
                response.put("failures", result.getFailures());
            }
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 导出模板
     */
    @GetMapping("/tasks/export-template")
    public void exportTemplate(javax.servlet.http.HttpServletResponse httpResponse) {
        try {
            // 简化实现：返回提示信息
            httpResponse.setContentType("text/plain;charset=UTF-8");
            httpResponse.setHeader("Content-Disposition", "attachment; filename=task_template.txt");
            String content = "任务名称\t任务描述\t经度\t纬度\t地址\t积分奖励\t任务类型\t状态\n" +
                           "示例：城市地标打卡\t在城市地标建筑前拍照打卡\t116.3974\t39.9093\t城市中心广场\t50\t1\t1\n" +
                           "说明：\n" +
                           "- 任务类型：1=历史文化, 2=自然风光, 3=美食探索, 4=艺术体验, 5=科技创新\n" +
                           "- 状态：1=进行中, 0=已结束\n" +
                           "注意：当前版本暂不支持Excel导入，请使用手动添加任务功能。";
            httpResponse.getWriter().write(content);
            httpResponse.getWriter().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取用户列表（管理后台）
     */
    @GetMapping("/users")
    public Map<String, Object> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            Integer statusInt = null;
            if (status != null && !status.isEmpty()) {
                if ("active".equalsIgnoreCase(status)) {
                    statusInt = 1;
                } else if ("inactive".equalsIgnoreCase(status)) {
                    statusInt = 0;
                } else {
                    try {
                        statusInt = Integer.parseInt(status);
                    } catch (NumberFormatException ignored) {}
                }
            }

            Map<String, Object> result = userService.getUserList(page, size, search, role, statusInt);
            
            // 转换为Map格式，添加完成任务数
            @SuppressWarnings("unchecked")
            List<com.cityquest.entity.UserInfo> userList = (List<com.cityquest.entity.UserInfo>) result.get("list");
            List<Map<String, Object>> enrichedUsers = new java.util.ArrayList<>();
            if (userList != null) {
                for (com.cityquest.entity.UserInfo user : userList) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("nickname", user.getNickname());
                    userMap.put("avatar", user.getAvatar());
                    userMap.put("email", user.getEmail());
                    userMap.put("phone", user.getPhone());
                    userMap.put("role", user.getRole());
                    userMap.put("points", user.getPoints());
                    userMap.put("status", user.getStatus());
                    userMap.put("createTime", user.getCreateTime());
                    userMap.put("updateTime", user.getUpdateTime());
                    // 查询用户完成的任务数（audit_status=1的记录数）
                    int completedTaskCount = recordMapper.selectCount(user.getId(), null, 1);
                    userMap.put("completedTasks", completedTaskCount);
                    enrichedUsers.add(userMap);
                }
            }
            
            result.put("list", enrichedUsers);
            result.put("items", enrichedUsers); // 兼容前端需要的items字段
            
            return result;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("items", List.of());
            response.put("total", 0);
            return response;
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody UserInfo userInfo) {
        Map<String, Object> response = new HashMap<>();
        try {
            userInfo.setId(id);
            boolean result = userService.updateUser(userInfo);
            if (result) {
                UserInfo updated = userService.getUserById(id);
                response.put("success", true);
                response.put("data", updated);
            } else {
                response.put("success", false);
                response.put("message", "更新失败");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean result = userService.deleteUser(id);
            response.put("success", result);
            response.put("message", result ? "删除成功" : "删除失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 更新用户状态（管理员踢下线功能）
     * status: 0=离线/未登录, 1=在线/已登录
     */
    @PatchMapping("/users/{id}/status")
    public Map<String, Object> updateUserStatus(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserInfo user = userService.getUserById(id.longValue());
            if (user != null) {
                // 检查是否为管理员用户
                if ("admin".equals(user.getRole())) {
                    response.put("success", false);
                    response.put("message", "不能修改管理员用户状态");
                    return response;
                }
                
                // 支持数字和字符串两种格式
                Object statusObj = body.get("status");
                Integer statusInt = null;
                
                if (statusObj instanceof Integer) {
                    statusInt = (Integer) statusObj;
                } else if (statusObj instanceof Number) {
                    statusInt = ((Number) statusObj).intValue();
                } else if (statusObj instanceof String) {
                    String status = (String) statusObj;
                    if ("active".equals(status) || "1".equals(status)) {
                        statusInt = 1;
                    } else if ("inactive".equals(status) || "0".equals(status)) {
                        statusInt = 0;
                    }
                }
                
                if (statusInt == null || (statusInt != 0 && statusInt != 1)) {
                    response.put("success", false);
                    response.put("message", "状态值无效，必须为0或1");
                    return response;
                }
                
                user.setStatus(statusInt);
                boolean result = userService.updateUser(user);
                if (result) {
                    if (statusInt == 0) {
                        onlineUserService.markOffline(user.getId());
                    } else if (statusInt == 1) {
                        onlineUserService.markOnline(user.getId(), null);
                    }
                }
                response.put("success", result);
                response.put("message", result ? (statusInt == 1 ? "用户已上线" : "用户已强制下线") : "更新失败");
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/users/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String password = body.get("password");
            if (password == null || password.isEmpty()) {
                response.put("success", false);
                response.put("message", "密码不能为空");
                return response;
            }
            boolean result = userService.resetPassword(id, password);
            response.put("success", result);
            response.put("message", result ? "密码重置成功" : "密码重置失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 批量更新用户状态（管理员批量踢下线功能）
     * status: 0=离线/未登录, 1=在线/已登录
     */
    @PatchMapping("/users/batch/status")
    public Map<String, Object> batchUpdateUserStatus(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<?> idsRaw = (List<?>) body.get("ids");
            List<Long> ids = new ArrayList<>();
            if (idsRaw != null) {
                for (Object obj : idsRaw) {
                    if (obj == null) continue;
                    try {
                        ids.add(Long.parseLong(String.valueOf(obj)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
 
            Object statusObj = body.get("status");
            
            Integer statusInt = null;
            // 支持数字和字符串两种格式
            if (statusObj instanceof Integer) {
                statusInt = (Integer) statusObj;
            } else if (statusObj instanceof Number) {
                statusInt = ((Number) statusObj).intValue();
            } else if (statusObj instanceof String) {
                String status = (String) statusObj;
                if ("active".equals(status) || "1".equals(status)) {
                    statusInt = 1;
                } else if ("inactive".equals(status) || "0".equals(status)) {
                    statusInt = 0;
                }
            }
            
            if (statusInt == null || (statusInt != 0 && statusInt != 1)) {
                response.put("success", false);
                response.put("message", "状态值无效，必须为0或1");
                return response;
            }
            
            int successCount = 0;
            for (Long id : ids) {
                UserInfo user = userService.getUserById(id);
                if (user != null && !"admin".equals(user.getRole())) {
                    user.setStatus(statusInt);
                    if (userService.updateUser(user)) {
                        if (statusInt == 0) {
                            onlineUserService.markOffline(id);
                        } else if (statusInt == 1) {
                            onlineUserService.markOnline(id, null);
                        }
                        successCount++;
                    }
                }
            }
            
            response.put("success", true);
            response.put("message", statusInt == 1 ? "已批量上线 " + successCount + " 个用户" : "已批量强制下线 " + successCount + " 个用户");
            response.put("successCount", successCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 批量重置密码
     */
    @PostMapping("/users/batch/reset-password")
    public Map<String, Object> batchResetPassword(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) body.get("ids");
            String password = (String) body.get("password");
            
            if (password == null || password.isEmpty()) {
                response.put("success", false);
                response.put("message", "密码不能为空");
                return response;
            }
            
            int successCount = 0;
            for (Integer id : ids) {
                try {
                    if (userService.resetPassword(id.longValue(), password)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    // 跳过管理员用户
                }
            }
            
            response.put("success", true);
            response.put("message", "批量重置密码成功");
            response.put("successCount", successCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取记录列表（管理后台）
     */
    @GetMapping("/records")
    public Map<String, Object> getRecordList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            System.out.println("========= 管理后台获取记录列表 =========");
            System.out.println("请求参数 - page: " + page + ", pageSize: " + pageSize + ", status: " + status);
            
            Integer statusInt = null;
            if (status != null && !status.isEmpty()) {
                if ("pending".equals(status)) {
                    statusInt = 0;
                    System.out.println("状态转换: pending -> 0 (待审核)");
                } else if ("approved".equals(status)) {
                    statusInt = 1;
                    System.out.println("状态转换: approved -> 1 (已通过)");
                } else if ("rejected".equals(status)) {
                    statusInt = 2;
                    System.out.println("状态转换: rejected -> 2 (已拒绝)");
                }
            } else {
                System.out.println("未指定状态，查询所有记录");
            }
            
            Map<String, Object> result = recordService.getAuditList(statusInt, page, pageSize);
            System.out.println("Service返回结果 - list大小: " + (result.get("list") != null ? ((java.util.List<?>) result.get("list")).size() : 0) + ", total: " + result.get("total"));
            @SuppressWarnings("unchecked")
            java.util.List<com.cityquest.entity.RecordInfo> list = (java.util.List<com.cityquest.entity.RecordInfo>) result.get("list");

            // 富化数据，便于前端直接展示（用户名、任务名、状态文本、积分、图片、位置等）
            java.util.List<java.util.Map<String, Object>> enriched = new java.util.ArrayList<>();
            if (list != null) {
                for (com.cityquest.entity.RecordInfo r : list) {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", r.getId());
                    m.put("userId", r.getUserId());
                    m.put("taskId", r.getTaskId());
                    m.put("longitude", r.getLongitude());
                    m.put("latitude", r.getLatitude());
                    m.put("description", r.getDescription());
                    m.put("createTime", r.getCreateTime());
                    m.put("auditTime", r.getUpdateTime());
                    m.put("auditRemark", r.getAuditRemark());

                    // 审核状态映射
                    String statusText = "pending";
                    if (r.getAuditStatus() != null) {
                        if (r.getAuditStatus() == 1) statusText = "approved";
                        else if (r.getAuditStatus() == 2) statusText = "rejected";
                    }
                    m.put("status", statusText);

                    // 用户名、头像
                    try {
                        com.cityquest.entity.UserInfo u = userService.getUserById(r.getUserId().longValue());
                        if (u != null) {
                            m.put("username", u.getUsername());
                            m.put("avatar", u.getAvatar());
                        }
                    } catch (Exception ignored) {}

                    // 任务标题、积分、地址
                    try {
                        com.cityquest.entity.TaskInfo t = taskService.getTaskById(r.getTaskId());
                        if (t != null) {
                            m.put("taskTitle", t.getTitle());
                            m.put("points", t.getReward());
                            m.put("address", t.getAddress());
                            // 展示位置（优先任务地址，否则经纬度）
                            String location = t.getAddress();
                            if (location == null || location.isEmpty()) {
                                if (r.getLongitude() != null && r.getLatitude() != null) {
                                    location = r.getLongitude() + ", " + r.getLatitude();
                                }
                            }
                            m.put("location", location);
                        }
                    } catch (Exception ignored) {}

                    // 图片 - 转换为完整URL，并验证文件是否存在
                    String photoUrl = r.getPhotoUrl();
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        // 如果已经是完整URL，直接使用；否则添加基础路径
                        String imageUrl = null;
                        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
                            imageUrl = photoUrl;
                        } else {
                            // 相对路径转换为完整URL
                            // 注意：这里使用 /api/uploads/ 因为context-path是 /api
                            if (photoUrl.startsWith("/")) {
                                imageUrl = "/api" + photoUrl;
                            } else {
                                imageUrl = "/api/uploads/" + photoUrl;
                            }
                        }
                        
                        // 验证文件是否真的存在（处理旧数据中文件不存在的情况）
                        try {
                            String userDir = System.getProperty("user.dir");
                            String filePath = null;
                            
                            // 从URL中提取实际文件路径
                            if (photoUrl.startsWith("/uploads/")) {
                                // 移除 /uploads/ 前缀，获取相对路径
                                String relativePath = photoUrl.substring("/uploads/".length());
                                filePath = userDir + java.io.File.separator + "uploads" + java.io.File.separator + relativePath;
                            } else if (!photoUrl.startsWith("http://") && !photoUrl.startsWith("https://")) {
                                // 如果不是完整URL，尝试直接拼接
                                filePath = userDir + java.io.File.separator + "uploads" + java.io.File.separator + photoUrl;
                            }
                            
                            // 检查文件是否存在
                            if (filePath != null) {
                                java.io.File file = new java.io.File(filePath);
                                if (!file.exists() || file.length() == 0) {
                                    System.out.println("警告: 图片文件不存在或为空: " + filePath);
                                    System.out.println("  数据库URL: " + photoUrl);
                                    System.out.println("  记录ID: " + r.getId());
                                    // 文件不存在，设置为null，前端会显示占位符
                                    imageUrl = null;
                                }
                            }
                        } catch (Exception e) {
                            // 验证失败，仍然返回URL，让前端处理
                            System.err.println("验证图片文件时出错: " + e.getMessage());
                        }
                        
                        m.put("imageUrl", imageUrl);
                    } else {
                        m.put("imageUrl", null);
                    }

                    enriched.add(m);
                }
            }

            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("records", enriched);
            resp.put("items", enriched); // 兼容前端需要的items字段
            resp.put("total", result.getOrDefault("total", 0));
            // 保留原始列表以便需要时兼容
            resp.put("list", enriched);
            resp.put("success", true);
            System.out.println("返回给前端 - records数量: " + enriched.size() + ", total: " + result.getOrDefault("total", 0));
            System.out.println("========= 管理后台获取记录列表完成 =========");
            return resp;
        } catch (Exception e) {
            System.err.println("========= 管理后台获取记录列表出错 =========");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("records", List.of());
            response.put("items", List.of());
            response.put("total", 0);
            return response;
        }
    }

    /**
     * 获取当前管理员ID
     */
    private Integer getCurrentAdminId(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token != null && !token.isEmpty()) {
                String userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    return Integer.parseInt(userId);
                }
            }
        } catch (Exception e) {
            // 忽略错误，返回null
        }
        return null;
    }

    /**
     * 审核通过记录
     */
    @PostMapping("/records/{id}/approve")
    public Map<String, Object> approveRecord(@PathVariable Integer id, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer adminId = getCurrentAdminId(request);
            boolean result = recordService.auditRecord(id, 1, null, adminId);
            response.put("success", result);
            response.put("message", result ? "审核通过" : "审核失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 审核拒绝记录
     */
    @PostMapping("/records/{id}/reject")
    public Map<String, Object> rejectRecord(@PathVariable Integer id, @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String remark = body != null ? body.get("remark") : null;
            Integer adminId = getCurrentAdminId(request);
            boolean result = recordService.auditRecord(id, 2, remark, adminId);
            response.put("success", result);
            response.put("message", result ? "已拒绝" : "拒绝失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 批量审核通过记录
     */
    @PostMapping("/records/batch/approve")
    public Map<String, Object> batchApproveRecords(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) body.get("ids");
            Integer adminId = getCurrentAdminId(request);
            int successCount = 0;
            for (Integer id : ids) {
                try {
                    if (recordService.auditRecord(id, 1, null, adminId)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    // 忽略单个记录的错误，继续处理其他记录
                }
            }
            response.put("success", true);
            response.put("message", "已通过 " + successCount + " 条记录");
            response.put("successCount", successCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 批量审核拒绝记录
     */
    @PostMapping("/records/batch/reject")
    public Map<String, Object> batchRejectRecords(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) body.get("ids");
            String remark = (String) body.get("remark");
            Integer adminId = getCurrentAdminId(request);
            int successCount = 0;
            for (Integer id : ids) {
                try {
                    if (recordService.auditRecord(id, 2, remark, adminId)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    // 忽略单个记录的错误，继续处理其他记录
                }
            }
            response.put("success", true);
            response.put("message", "已拒绝 " + successCount + " 条记录");
            response.put("successCount", successCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 批量删除记录
     */
    @DeleteMapping("/records/batch")
    public Map<String, Object> batchDeleteRecords(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) body.get("ids");
            int successCount = recordService.batchDeleteApprovedRecords(ids);
            response.put("success", true);
            response.put("message", "已删除 " + successCount + " 条记录（仅删除已通过的）");
            response.put("successCount", successCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 删除单条记录（仅允许已通过）
     */
    @DeleteMapping("/records/{id}")
    public Map<String, Object> deleteRecord(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean result = recordService.deleteApprovedRecord(id);
            response.put("success", result);
            response.put("message", result ? "删除成功" : "删除失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取活动日志
     */
    @GetMapping("/activity-logs")
    public Map<String, Object> getActivityLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String sortBy) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 从打卡记录生成活动日志
            Map<String, Object> recordList = recordService.getAuditList(null, page, pageSize);
            @SuppressWarnings("unchecked")
            List<com.cityquest.entity.RecordInfo> records = (List<com.cityquest.entity.RecordInfo>) recordList.get("list");
            
            List<Map<String, Object>> logs = new java.util.ArrayList<>();
            for (com.cityquest.entity.RecordInfo record : records) {
                Map<String, Object> log = new HashMap<>();
                log.put("id", record.getId());
                log.put("username", "用户" + record.getUserId()); // 简化，实际应该查询用户名
                log.put("type", "record");
                log.put("level", "info");
                log.put("action", "提交打卡记录");
                log.put("userId", record.getUserId());
                log.put("taskId", record.getTaskId());
                log.put("time", record.getCreateTime());
                log.put("createdAt", record.getCreateTime());
                log.put("description", "用户提交了打卡记录");
                log.put("ip", "127.0.0.1"); // 简化
                log.put("userAgent", "Unknown"); // 简化
                log.put("params", new HashMap<>());
                log.put("result", new HashMap<>());
                logs.add(log);
            }
            
            response.put("items", logs);
            response.put("total", recordList.get("total"));
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("items", List.of());
            response.put("total", 0);
        }
        return response;
    }

    /**
     * 导出活动日志（CSV）
     * 前端以 POST /admin/activity-logs/export 调用，返回二进制流用于下载
     */
    @PostMapping("/activity-logs/export")
    public void exportActivityLogs(@RequestBody(required = false) Map<String, Object> body,
                                   javax.servlet.http.HttpServletResponse httpResponse) {
        try {
            // 读取筛选条件（目前简单透传，后续可按需对 recordService 做筛选扩展）
            String searchQuery = body != null ? (String) body.get("searchQuery") : null;
            String logType = body != null ? (String) body.get("logType") : null;
            String logLevel = body != null ? (String) body.get("logLevel") : null;
            // dateRange 可能是数组，这里先不强依赖解析

            // 暂用较大分页拉取一定规模数据（后续可改为游标/流式导出）
            int page = 1;
            int pageSize = 10000;

            Map<String, Object> recordList = recordService.getAuditList(null, page, pageSize);
            @SuppressWarnings("unchecked")
            List<com.cityquest.entity.RecordInfo> records = (List<com.cityquest.entity.RecordInfo>) recordList.get("list");

            // 生成 CSV 内容（UTF-8 含 BOM，便于 Excel 正常显示中文）
            StringBuilder sb = new StringBuilder();
            // 表头
            sb.append("ID,用户,类型,级别,操作,用户ID,任务ID,时间,审核状态,审核备注\r\n");

            if (records != null) {
                for (com.cityquest.entity.RecordInfo record : records) {
                    // 简化：类型/级别固定；用户名可后续从 userService 查询
                    String username = "用户" + record.getUserId();
                    String type = logType != null && !logType.isEmpty() ? logType : "record";
                    String level = logLevel != null && !logLevel.isEmpty() ? logLevel : "info";
                    String action = "提交打卡记录";
                    // 使用Excel能识别的日期时间格式：yyyy/MM/dd HH:mm:ss（斜杠分隔日期，Excel更容易识别）
                    String timeStr = record.getCreateTime() != null ? new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(record.getCreateTime()) : "";
                    String statusText = "";
                    if (record.getAuditStatus() != null) {
                        if (record.getAuditStatus() == 0) statusText = "pending";
                        else if (record.getAuditStatus() == 1) statusText = "approved";
                        else if (record.getAuditStatus() == 2) statusText = "rejected";
                    }
                    String remark = record.getAuditRemark() != null ? record.getAuditRemark().replace('\n', ' ').replace('\r', ' ') : "";

                    // 简单过滤：按搜索关键字过滤用户名/操作/备注
                    if (searchQuery != null && !searchQuery.isEmpty()) {
                        String lower = searchQuery.toLowerCase();
                        String combined = (username + " " + action + " " + remark).toLowerCase();
                        if (!combined.contains(lower)) {
                            continue;
                        }
                    }

                    // CSV 转义：用双引号包裹包含逗号或特殊字符的字段
                    // 时间字段不使用引号，使用Excel能识别的格式
                    sb.append(record.getId()).append(',')
                      .append('"').append(username.replace("\"", "\"\"")) .append('"').append(',')
                      .append(type).append(',')
                      .append(level).append(',')
                      .append('"').append(action).append('"').append(',')
                      .append(record.getUserId()).append(',')
                      .append(record.getTaskId()).append(',')
                      .append(timeStr).append(',')  // 时间不使用引号，使用Excel能识别的格式
                      .append(statusText).append(',')
                      .append('"').append(remark.replace("\"", "\"\"")) .append('"')
                      .append("\r\n");
                }
            }

            // 使用UTF-8 BOM编码，Excel能更好地识别中文和日期格式
            byte[] bom = new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF};
            byte[] data = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

            String filename = "activity-logs-" + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date()) + ".csv";
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");

            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.setContentType("text/csv; charset=UTF-8");
            // 同时设置传统 filename 和 RFC5987 filename*，兼容不同浏览器
            httpResponse.setHeader("Content-Disposition", "attachment; filename=" + filename + "; filename*=UTF-8''" + encodedFilename);

            try (java.io.OutputStream os = httpResponse.getOutputStream()) {
                os.write(bom);  // 写入UTF-8 BOM，帮助Excel正确识别编码
                os.write(data);
                os.flush();
            }
        } catch (Exception e) {
            try {
                httpResponse.reset();
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.setStatus(javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                httpResponse.getWriter().write("{\"success\":false,\"message\":\"导出失败: " + e.getMessage().replace("\"", "'") + "\"}");
                httpResponse.getWriter().flush();
            } catch (Exception ignore) {}
        }
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            if (token != null && !token.isEmpty()) {
                try {
                    UserInfo userInfo = userService.getCurrentUserInfo(token);
                    if (userInfo != null) {
                        // 退出登录，将状态设置为0（离线）
                        userInfo.setStatus(0);
                        userInfo.setUpdateTime(new java.util.Date());
                        userService.updateUser(userInfo);
                        onlineUserService.markOffline(userInfo.getId());
                        response.put("success", true);
                        response.put("message", "退出登录成功");
                    } else {
                        response.put("success", false);
                        response.put("message", "用户信息不存在");
                    }
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("message", "退出登录失败: " + e.getMessage());
                }
            } else {
                response.put("success", false);
                response.put("message", "未提供Token");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "退出登录失败: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/users/online")
    public Map<String, Object> getOnlineUsers() {
        Map<String, Object> response = new HashMap<>();
        try {
            Set<String> onlineIds = onlineUserService.getOnlineUserIds();
            List<Map<String, Object>> list = new ArrayList<>();
            for (String idStr : onlineIds) {
                try {
                    Long userId = Long.parseLong(idStr);
                    UserInfo userInfo = userService.getUserById(userId);
                    Map<String, String> sessionInfo = onlineUserService.getSessionInfo(userId);
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", userId);
                    if (userInfo != null) {
                        item.put("username", userInfo.getUsername());
                        item.put("nickname", userInfo.getNickname());
                        item.put("avatar", userInfo.getAvatar());
                        item.put("role", userInfo.getRole());
                        item.put("points", userInfo.getPoints());
                        item.put("status", userInfo.getStatus());
                        item.put("email", userInfo.getEmail());
                        item.put("phone", userInfo.getPhone());
                    }
                    item.put("session", sessionInfo);
                    list.add(item);
                } catch (NumberFormatException ignored) {
                }
            }
            response.put("success", true);
            response.put("list", list);
            response.put("total", list.size());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/users/{id}/force-logout")
    public Map<String, Object> forceLogout(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            onlineUserService.forceLogout(id);
            UserInfo update = new UserInfo();
            update.setId(id);
            update.setStatus(0);
            update.setUpdateTime(new Date());
            userService.updateUser(update);
            response.put("success", true);
            response.put("message", "已强制下线该用户");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "强制下线失败: " + e.getMessage());
        }
        return response;
    }

    // ==================== 商城管理 ====================

    /**
     * 获取商品分类列表
     */
    @GetMapping("/mall/categories")
    public Map<String, Object> getCategories() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ProductCategory> categories = categoryService.getAllCategories(null);
            response.put("success", true);
            response.put("data", categories);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 创建商品分类
     */
    @PostMapping("/mall/categories")
    public Map<String, Object> createCategory(@RequestBody ProductCategory category) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean result = categoryService.createCategory(category);
            response.put("success", result);
            response.put("message", result ? "创建成功" : "创建失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 更新商品分类
     */
    @PutMapping("/mall/categories/{id}")
    public Map<String, Object> updateCategory(@PathVariable Integer id, @RequestBody ProductCategory category) {
        Map<String, Object> response = new HashMap<>();
        try {
            category.setId(id);
            boolean result = categoryService.updateCategory(category);
            response.put("success", result);
            response.put("message", result ? "更新成功" : "更新失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 删除商品分类
     */
    @DeleteMapping("/mall/categories/{id}")
    public Map<String, Object> deleteCategory(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean result = categoryService.deleteCategory(id);
            response.put("success", result);
            response.put("message", result ? "删除成功" : "删除失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取商品列表
     */
    @GetMapping("/mall/products")
    public Map<String, Object> getProducts(@RequestParam(required = false) Integer categoryId,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = productService.getProductList(categoryId, status, keyword, page, pageSize);
            response.put("success", true);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 创建商品
     */
    @PostMapping("/mall/products")
    public Map<String, Object> createProduct(@RequestBody ProductInfo product, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long adminId = getCurrentUserId(request);
            product.setCreateBy(adminId);
            boolean result = productService.createProduct(product);
            response.put("success", result);
            response.put("message", result ? "创建成功" : "创建失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 更新商品
     */
    @PutMapping("/mall/products/{id}")
    public Map<String, Object> updateProduct(@PathVariable Integer id, @RequestBody ProductInfo product) {
        Map<String, Object> response = new HashMap<>();
        try {
            product.setId(id);
            boolean result = productService.updateProduct(product);
            response.put("success", result);
            response.put("message", result ? "更新成功" : "更新失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/mall/products/{id}")
    public Map<String, Object> deleteProduct(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean result = productService.deleteProduct(id);
            response.put("success", result);
            response.put("message", result ? "删除成功" : "删除失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 上架/下架商品
     */
    @PostMapping("/mall/products/{id}/status")
    public Map<String, Object> updateProductStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer status = body.get("status");
            boolean result = productService.updateProductStatus(id, status);
            response.put("success", result);
            response.put("message", result ? "操作成功" : "操作失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取订单列表
     */
    @GetMapping("/mall/orders")
    public Map<String, Object> getOrders(@RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = orderService.getOrderList(null, status, keyword, page, pageSize);
            response.put("success", true);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/mall/orders/{id}")
    public Map<String, Object> getOrderDetail(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            ExchangeOrder order = orderService.getOrderById(id);
            if (order == null) {
                response.put("success", false);
                response.put("message", "订单不存在");
                return response;
            }
            response.put("success", true);
            response.put("data", order);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 更新订单状态
     */
    @PostMapping("/mall/orders/{id}/status")
    public Map<String, Object> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer status = body.get("status");
            boolean result = orderService.updateOrderStatus(id, status);
            response.put("success", result);
            response.put("message", result ? "更新成功" : "更新失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 更新物流信息
     */
    @PostMapping("/mall/orders/{id}/logistics")
    public Map<String, Object> updateLogistics(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String logisticsInfo = body.get("logisticsInfo");
            boolean result = orderService.updateLogisticsInfo(id, logisticsInfo);
            response.put("success", result);
            response.put("message", result ? "更新成功" : "更新失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取当前用户ID（Long类型）
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token != null && !token.isEmpty()) {
                String userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    return Long.parseLong(userId);
                }
            }
        } catch (Exception e) {
            // 忽略错误，返回null
        }
        return null;
    }
}

