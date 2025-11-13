package com.cityquest.service.impl;

import com.cityquest.dto.task.TaskImportResult;
import com.cityquest.entity.TaskInfo;
import com.cityquest.mapper.RecordMapper;
import com.cityquest.mapper.TaskMapper;
import com.cityquest.service.TaskService;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/**
 * 任务服务实现类
 */
@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private RecordMapper recordMapper;

    @Override
    public Map<String, Object> getTaskList(Integer type, Integer status, Integer page, Integer pageSize, String keyword, Long userId) {
        int offset = (page - 1) * pageSize;
        List<TaskInfo> taskList = taskMapper.selectList(type, status, keyword, offset, pageSize);
        int total = taskMapper.selectCount(type, status, keyword);
        
        // 如果提供了userId，检查每个任务的完成状态（在下面的代码中处理）
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", taskList);
        result.put("items", taskList); // 兼容前端需要的items字段
        result.put("total", total);
        
        // 如果提供了userId，同时返回每个任务的完成状态
        if (userId != null) {
            Map<Integer, Boolean> completionMap = new HashMap<>();
            for (TaskInfo task : taskList) {
                boolean completed = recordMapper.checkUserTaskCompletion(userId, task.getId()) > 0;
                completionMap.put(task.getId(), completed);
            }
            result.put("completionStatus", completionMap);
        }
        
        return result;
    }

    @Override
    public TaskInfo getTaskById(Integer id) {
        return taskMapper.selectById(id);
    }

    @Override
    public boolean createTask(TaskInfo taskInfo) {
        if (taskInfo.getStatus() == null) {
            taskInfo.setStatus(0);
        }
        if (taskInfo.getType() == null) {
            taskInfo.setType(0);
        }
        taskInfo.setCreateTime(new Date());
        taskInfo.setUpdateTime(new Date());
        taskInfo.setCompletionCount(0);
        return taskMapper.insert(taskInfo) > 0;
    }

    @Override
    public boolean updateTask(TaskInfo taskInfo) {
        taskInfo.setUpdateTime(new Date());
        return taskMapper.update(taskInfo) > 0;
    }

    @Override
    public boolean deleteTask(Integer id) {
        return taskMapper.delete(id) > 0;
    }

    @Override
    public List<TaskInfo> getNearbyTasks(Double longitude, Double latitude, Double radius) {
        return taskMapper.selectNearbyTasks(longitude, latitude, radius);
    }

    @Override
    public List<TaskInfo> getHotTasks(Integer limit) {
        // 使用实时统计完成人数的查询
        int offset = 0;
        return taskMapper.selectList(null, 1, null, offset, limit);
    }

    @Override
    public TaskImportResult importTasks(InputStream inputStream, String filename, Long createBy) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("文件数据不能为空");
        }
        String safeFilename = filename != null ? filename : "unknown";
        TaskImportResult result = new TaskImportResult();
        result.setFileName(safeFilename);

        String lowerName = safeFilename.toLowerCase(Locale.ROOT);

        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
            importFromWorkbook(inputStream, result, createBy);
        } else if (lowerName.endsWith(".csv")) {
            importFromDelimited(inputStream, result, createBy, ",");
        } else if (lowerName.endsWith(".txt")) {
            importFromDelimited(inputStream, result, createBy, "\t");
        } else {
            throw new IllegalArgumentException("暂不支持的文件类型：" + safeFilename);
        }

        return result;
    }

    private void importFromWorkbook(InputStream inputStream, TaskImportResult result, Long createBy) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return;
            }
            DataFormatter formatter = new DataFormatter();
            int lastRowNum = sheet.getLastRowNum();
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowBlank(row, formatter)) {
                    continue;
                }
                int displayRow = i + 1;
                result.increaseTotalRows();
                String title = getCellValue(row, 0, formatter);
                String description = getCellValue(row, 1, formatter);
                String longitudeStr = getCellValue(row, 2, formatter);
                String latitudeStr = getCellValue(row, 3, formatter);
                String address = getCellValue(row, 4, formatter);
                String rewardStr = getCellValue(row, 5, formatter);
                String typeStr = getCellValue(row, 6, formatter);
                String statusStr = getCellValue(row, 7, formatter);
                String coverImage = getCellValue(row, 8, formatter);

                processRow(displayRow, title, description, longitudeStr, latitudeStr, address,
                        rewardStr, typeStr, statusStr, coverImage, result, createBy);
            }
        } catch (EncryptedDocumentException e) {
            throw new IOException("无法解析Excel文件，请检查格式是否正确", e);
        }
    }

    private void importFromDelimited(InputStream inputStream, TaskImportResult result, Long createBy, String delimiter) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int rowIndex = 0;
            while ((line = reader.readLine()) != null) {
                rowIndex++;
                if (rowIndex == 1) {
                    continue;
                }
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                result.increaseTotalRows();
                String[] parts = line.split(delimiter, -1);
                if (parts.length == 0) {
                    continue;
                }
                String title = parts.length > 0 ? parts[0] : "";
                String description = parts.length > 1 ? parts[1] : "";
                String longitudeStr = parts.length > 2 ? parts[2] : "";
                String latitudeStr = parts.length > 3 ? parts[3] : "";
                String address = parts.length > 4 ? parts[4] : "";
                String rewardStr = parts.length > 5 ? parts[5] : "";
                String typeStr = parts.length > 6 ? parts[6] : "";
                String statusStr = parts.length > 7 ? parts[7] : "";
                String coverImage = parts.length > 8 ? parts[8] : "";

                processRow(rowIndex, title, description, longitudeStr, latitudeStr, address,
                        rewardStr, typeStr, statusStr, coverImage, result, createBy);
            }
        }
    }

    private void processRow(int rowIndex,
                            String title,
                            String description,
                            String longitudeStr,
                            String latitudeStr,
                            String address,
                            String rewardStr,
                            String typeStr,
                            String statusStr,
                            String coverImage,
                            TaskImportResult result,
                            Long createBy) {
        if (!StringUtils.hasText(title)) {
            result.addFailure(rowIndex, "任务名称不能为空");
            return;
        }

        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTitle(title.trim());
        if (StringUtils.hasText(description)) {
            taskInfo.setDescription(description.trim());
        }
        if (StringUtils.hasText(address)) {
            taskInfo.setAddress(address.trim());
        }
        if (StringUtils.hasText(coverImage)) {
            taskInfo.setCoverImage(coverImage.trim());
        }
        if (createBy != null) {
            taskInfo.setCreateBy(createBy);
        }

        try {
            if (StringUtils.hasText(longitudeStr)) {
                taskInfo.setLongitude(Double.parseDouble(longitudeStr.trim()));
            }
            if (StringUtils.hasText(latitudeStr)) {
                taskInfo.setLatitude(Double.parseDouble(latitudeStr.trim()));
            }
        } catch (NumberFormatException e) {
            result.addFailure(rowIndex, "经纬度格式不正确");
            return;
        }

        Integer reward = parseInteger(rewardStr, 0, "积分奖励", rowIndex, result);
        if (reward == null) {
            return;
        }
        taskInfo.setReward(reward);

        Integer type = parseInteger(typeStr, 0, "任务类型", rowIndex, result);
        if (type == null) {
            return;
        }
        taskInfo.setType(type);

        Integer status = parseStatus(statusStr, rowIndex, result);
        if (status == null) {
            return;
        }
        taskInfo.setStatus(status);

        boolean inserted = this.createTask(taskInfo);
        if (inserted) {
            result.addSuccess(taskInfo);
        } else {
            result.addFailure(rowIndex, "数据库写入失败");
        }
    }

    private Integer parseInteger(String value, int defaultValue, String fieldName, int rowIndex, TaskImportResult result) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            result.addFailure(rowIndex, fieldName + "格式不正确");
            return null;
        }
    }

    private Integer parseStatus(String value, int rowIndex, TaskImportResult result) {
        if (!StringUtils.hasText(value)) {
            return 1;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        if ("1".equals(trimmed) || "active".equals(trimmed) || "进行中".equals(trimmed)) {
            return 1;
        }
        if ("0".equals(trimmed) || "inactive".equals(trimmed) || "停用".equals(trimmed)) {
            return 0;
        }
        result.addFailure(rowIndex, "状态值无效，仅支持 1/0 或 active/inactive");
        return null;
    }

    private boolean isRowBlank(Row row, DataFormatter formatter) {
        for (int i = 0; i < 9; i++) {
            if (StringUtils.hasText(getCellValue(row, i, formatter))) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        String value = formatter.formatCellValue(cell);
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}