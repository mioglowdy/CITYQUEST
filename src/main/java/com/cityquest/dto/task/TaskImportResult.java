package com.cityquest.dto.task;

import com.cityquest.entity.TaskInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 批量导入任务结果
 */
@Data
@NoArgsConstructor
public class TaskImportResult {

    private String fileName;
    private int totalRows;
    private int successCount;
    private int failedCount;
    private final List<FailureDetail> failures = new ArrayList<>();
    private final List<TaskInfo> importedTasks = new ArrayList<>();

    public void increaseTotalRows() {
        this.totalRows++;
    }

    public void addSuccess(TaskInfo taskInfo) {
        this.successCount++;
        if (taskInfo != null) {
            this.importedTasks.add(taskInfo);
        }
    }

    public void addFailure(int rowIndex, String reason) {
        this.failedCount++;
        this.failures.add(new FailureDetail(rowIndex, reason));
    }

    public List<FailureDetail> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    public List<TaskInfo> getImportedTasks() {
        return Collections.unmodifiableList(importedTasks);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FailureDetail {
        /**
         * Excel/文本中的行号（从1开始）
         */
        private int rowIndex;
        /**
         * 失败原因
         */
        private String reason;
    }
}

