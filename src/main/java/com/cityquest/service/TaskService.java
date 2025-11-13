package com.cityquest.service;

import com.cityquest.dto.task.TaskImportResult;
import com.cityquest.entity.TaskInfo;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
/**
 * 任务服务接口
 */
public interface TaskService {
    /**
     * 获取任务列表
     */
    Map<String, Object> getTaskList(Integer type, Integer status, Integer page, Integer pageSize, String keyword, Long userId);

    /**
     * 根据ID获取任务详情
     */
    TaskInfo getTaskById(Integer id);

    /**
     * 创建任务
     */
    boolean createTask(TaskInfo taskInfo);

    /**
     * 更新任务
     */
    boolean updateTask(TaskInfo taskInfo);

    /**
     * 删除任务
     */
    boolean deleteTask(Integer id);

    /**
     * 获取附近任务
     */
    List<TaskInfo> getNearbyTasks(Double longitude, Double latitude, Double radius);

    /**
     * 获取热门任务
     */
    List<TaskInfo> getHotTasks(Integer limit);

    /**
     * 批量导入任务
     *
     * @param inputStream   上传文件输入流
     * @param filename      原始文件名
     * @param createBy      创建人ID
     * @return 导入结果
     */
    TaskImportResult importTasks(InputStream inputStream, String filename, Long createBy) throws IOException;
}