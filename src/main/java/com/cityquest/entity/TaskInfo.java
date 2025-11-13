package com.cityquest.entity;

import lombok.Data;
import java.util.Date;

/**
 * 任务信息实体类
 */
@Data
public class TaskInfo {
    private Integer id;
    private String title;
    private String description;
    private Double longitude;
    private Double latitude;
    private String address;
    private Integer reward;
    private Integer type;
    private Integer status;
    private Long createBy;
    private Date createTime;
    private Date updateTime;
    private String coverImage;
    private Integer completionCount;
}