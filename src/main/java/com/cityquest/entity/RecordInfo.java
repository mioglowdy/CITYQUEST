package com.cityquest.entity;

import lombok.Data;
import java.util.Date;

/**
 * 打卡记录实体类
 */
@Data
public class RecordInfo {
    private Integer id;
    private Long userId;
    private Integer taskId;
    private String photoUrl;
    private Double longitude;
    private Double latitude;
    private String description;
    private Integer auditStatus;  // 0: 待审核, 1: 审核通过, 2: 审核拒绝
    private Date createTime;
    private Date updateTime;
    private String auditRemark;
    private Long adminId;
    
    // 关联信息
    private UserInfo userInfo;
    private TaskInfo taskInfo;
}