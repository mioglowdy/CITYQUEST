package com.cityquest.controller;

import com.cityquest.entity.RecordInfo;
import com.cityquest.entity.TaskInfo;
import com.cityquest.entity.UserInfo;
import com.cityquest.entity.dto.LoginRequest;
import com.cityquest.entity.dto.RegisterRequest;
import com.cityquest.entity.dto.ChangePasswordRequest;
import com.cityquest.entity.dto.UserProfileUpdateRequest;
import com.cityquest.mapper.RecordMapper;
import com.cityquest.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private TaskService taskService;

    @Autowired
    private OnlineUserService onlineUserService;

    @Autowired
    private QiniuService qiniuService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest loginRequest) {
        System.out.println("UserController: 接收到登录请求");
        System.out.println("UserController: 用户名 = " + loginRequest.getUsername());
        
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("UserController: 调用userService.login()");
            Map<String, Object> result = userService.login(loginRequest);
            System.out.println("UserController: userService.login() 调用成功");
            
            response.put("success", true);
            response.putAll(result);
            return response;
        } catch (RuntimeException e) {
            System.out.println("UserController: 捕获到异常: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        } catch (Exception e) {
            System.out.println("UserController: 捕获到未知异常: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "服务器内部错误");
            return response;
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest registerRequest) {
        boolean result = userService.register(registerRequest);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "注册成功" : "注册失败");
        return response;
    }

    /**
     * 获取当前登录用户信息（从token中获取）
     */
    @GetMapping("/info")
    public Map<String, Object> getCurrentUserInfo(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (token == null || token.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "未登录");
            return response;
        }
        
        try {
            UserInfo userInfo = userService.getCurrentUserInfo(token);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userInfo);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info/{id}")
    public Map<String, Object> getUserInfo(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = Long.parseLong(id);
            UserInfo userInfo = userService.getUserById(userId);
            if (userInfo == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return response;
            }
            response.put("success", true);
            response.put("data", userInfo);
            // 兼容直接返回UserInfo的情况
            response.put("id", userInfo.getId());
            response.put("username", userInfo.getUsername());
            response.put("nickname", userInfo.getNickname());
            response.put("avatar", userInfo.getAvatar());
            response.put("points", userInfo.getPoints());
            return response;
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("message", "无效的用户ID格式");
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public Map<String, Object> updateUser(@RequestBody UserInfo userInfo) {
        boolean result = userService.updateUser(userInfo);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "更新成功" : "更新失败");
        return response;
    }

    /**
     * 获取积分排行榜
     */
    @GetMapping("/rank")
    public Map<String, Object> getRankList(@RequestParam(defaultValue = "10") Integer limit) {
        List<UserInfo> rankList = userService.getRankList(limit);
        Map<String, Object> response = new HashMap<>();
        response.put("list", rankList);
        return response;
    }

    /**
     * 获取用户列表（管理员）
     */
    @GetMapping("/list")
    public Map<String, Object> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return userService.getUserList(page, pageSize, null, null, null);
    }

    /**
     * 用户退出登录（将状态设置为0）
     */
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
                    // token无效，仍然返回成功
                    response.put("success", true);
                    response.put("message", "退出登录成功");
                }
            } else {
                response.put("success", true);
                response.put("message", "退出登录成功");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "退出登录失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取个人中心信息
     */
    @GetMapping("/profile")
    public Map<String, Object> getProfile(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        Long userId = extractUserId(request);
        if (userId == null) {
            response.put("success", false);
            response.put("message", "未登录或token无效");
            return response;
        }

        UserInfo user = userService.getUserById(userId);
        if (user == null) {
            response.put("success", false);
            response.put("message", "用户不存在");
            return response;
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("avatar", user.getAvatar());
        profile.put("role", user.getRole());
        profile.put("points", user.getPoints());
        profile.put("createTime", user.getCreateTime());

        int totalRecords = recordMapper.selectCount(userId, null, null);
        int approvedRecords = recordMapper.selectCount(userId, null, 1);

        Map<String, Object> stats = new HashMap<>();
        stats.put("points", user.getPoints() != null ? user.getPoints() : 0);
        stats.put("totalRecords", totalRecords);
        stats.put("completedRecords", approvedRecords);

        Map<String, Object> recordMap = recordService.getUserRecords(userId, 1, 5);
        @SuppressWarnings("unchecked")
        List<RecordInfo> recordList = (List<RecordInfo>) recordMap.getOrDefault("list", new ArrayList<>());
        List<Map<String, Object>> recentRecords = new ArrayList<>();
        for (RecordInfo record : recordList) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("taskId", record.getTaskId());
            TaskInfo taskInfo = null;
            try {
                taskInfo = taskService.getTaskById(record.getTaskId());
            } catch (Exception ignored) {
            }
            String taskTitle = taskInfo != null && taskInfo.getTitle() != null
                    ? taskInfo.getTitle()
                    : ("任务 #" + record.getTaskId());
            item.put("taskTitle", taskTitle);
            item.put("createTime", record.getCreateTime());
            item.put("auditStatus", record.getAuditStatus());
            item.put("photoUrl", record.getPhotoUrl());
            recentRecords.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("profile", profile);
        data.put("stats", stats);
        data.put("recentRecords", recentRecords);

        response.put("success", true);
        response.put("data", data);
        return response;
    }

    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody UserProfileUpdateRequest updateRequest,
                                             HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        Long userId = extractUserId(request);
        if (userId == null) {
            response.put("success", false);
            response.put("message", "未登录或token无效");
            return response;
        }

        try {
            UserInfo update = new UserInfo();
            update.setId(userId);
            if (updateRequest.getNickname() != null) {
                update.setNickname(updateRequest.getNickname().trim());
            }
            if (updateRequest.getEmail() != null) {
                update.setEmail(updateRequest.getEmail().trim());
            }
            if (updateRequest.getPhone() != null) {
                update.setPhone(updateRequest.getPhone().trim());
            }
            if (StringUtils.hasText(updateRequest.getAvatar())) {
                update.setAvatar(updateRequest.getAvatar().trim());
            }
            boolean result = userService.updateUser(update);
            response.put("success", result);
            response.put("message", result ? "个人资料更新成功" : "个人资料更新失败");
            if (result) {
                UserInfo latest = userService.getUserById(userId);
                response.put("data", latest);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 上传头像（使用七牛云）
     */
    @PostMapping(value = "/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadAvatar(@RequestParam("file") MultipartFile file,
                                            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        Long userId = extractUserId(request);
        if (userId == null) {
            response.put("success", false);
            response.put("message", "未登录或token无效");
            return response;
        }

        if (file == null || file.isEmpty()) {
            response.put("success", false);
            response.put("message", "文件不能为空");
            return response;
        }

        try {
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "只支持图片文件");
                return response;
            }

            // 验证文件大小（限制为2MB）
            if (file.getSize() > 2 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "图片大小不能超过2MB");
                return response;
            }

            // 生成文件名（使用用户ID + 时间戳，便于管理）
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String fileName = userId + "_" + System.currentTimeMillis() + extension;

            // 上传到七牛云
            String avatarUrl = qiniuService.uploadFile(file, fileName);

            // 更新用户头像URL（存储完整URL）
            UserInfo update = new UserInfo();
            update.setId(userId);
            update.setAvatar(avatarUrl);
            userService.updateUser(update);

            response.put("success", true);
            response.put("message", "头像上传成功");
            response.put("url", avatarUrl);
            response.put("path", avatarUrl); // 统一使用完整URL
            response.put("avatar", avatarUrl);
            response.put("fullAvatarUrl", avatarUrl);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "上传失败: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody ChangePasswordRequest passwordRequest,
                                              HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        Long userId = extractUserId(request);
        if (userId == null) {
            response.put("success", false);
            response.put("message", "未登录或token无效");
            return response;
        }

        if (passwordRequest.getOldPassword() == null || passwordRequest.getOldPassword().length() < 1) {
            response.put("success", false);
            response.put("message", "原密码不能为空");
            return response;
        }
        if (passwordRequest.getNewPassword() == null || passwordRequest.getNewPassword().length() < 6) {
            response.put("success", false);
            response.put("message", "新密码长度不能少于6位");
            return response;
        }
        if (passwordRequest.getOldPassword().equals(passwordRequest.getNewPassword())) {
            response.put("success", false);
            response.put("message", "新密码不能与原密码相同");
            return response;
        }

        try {
            boolean result = userService.changePassword(userId,
                    passwordRequest.getOldPassword(),
                    passwordRequest.getNewPassword());
            response.put("success", result);
            response.put("message", result ? "密码修改成功" : "密码修改失败");
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    private Long extractUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            UserInfo user = userService.getCurrentUserInfo(token);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}