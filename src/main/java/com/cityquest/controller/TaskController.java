package com.cityquest.controller;

import com.cityquest.entity.TaskInfo;
import com.cityquest.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务控制器
 */
@RestController
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /**
     * 获取任务列表
     */
    @GetMapping("/list")
    public Map<String, Object> getTaskList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String userId) {

        Integer typeValue = parseInteger(type);
        Integer statusValue = parseInteger(status);
        Long userIdValue = parseLong(userId);

        return taskService.getTaskList(typeValue, statusValue, page, pageSize, keyword, userIdValue);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数格式不正确，应为整数: " + value);
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数格式不正确，应为长整型: " + value);
        }
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/info/{id}")
    public TaskInfo getTaskInfo(@PathVariable Integer id) {
        return taskService.getTaskById(id);
    }

    /**
     * 创建任务（管理员）
     */
    @PostMapping("/create")
    public Map<String, Object> createTask(@RequestBody TaskInfo taskInfo) {
        boolean result = taskService.createTask(taskInfo);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "创建成功" : "创建失败");
        return response;
    }

    /**
     * 更新任务（管理员）
     */
    @PutMapping("/update")
    public Map<String, Object> updateTask(@RequestBody TaskInfo taskInfo) {
        boolean result = taskService.updateTask(taskInfo);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "更新成功" : "更新失败");
        return response;
    }

    /**
     * 删除任务（管理员）
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteTask(@PathVariable Integer id) {
        boolean result = taskService.deleteTask(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "删除成功" : "删除失败");
        return response;
    }

    /**
     * 获取附近任务
     */
    @GetMapping("/nearby")
    public Map<String, Object> getNearbyTasks(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(defaultValue = "5.0") Double radius) {
        List<TaskInfo> taskList = taskService.getNearbyTasks(longitude, latitude, radius);
        Map<String, Object> response = new HashMap<>();
        response.put("list", taskList);
        return response;
    }

    /**
     * 获取热门任务
     */
    @GetMapping("/hot")
    public Map<String, Object> getHotTasks(@RequestParam(defaultValue = "6") Integer limit) {
        List<TaskInfo> taskList = taskService.getHotTasks(limit);
        Map<String, Object> response = new HashMap<>();
        response.put("list", taskList);
        return response;
    }
}