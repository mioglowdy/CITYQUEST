package com.cityquest.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 兑换订单实体类
 */
@Data
public class ExchangeOrder {
    private Long id;
    private Long userId;
    private String orderNo;
    private Integer totalPoints;
    private Integer status; // 0=待发货，1=已发货，2=已完成，3=已取消
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String logisticsInfo;
    private String remark;
    private Date createTime;
    private Date updateTime;
    
    // 关联字段（用于查询）
    private String userNickname;
    private List<ExchangeOrderItem> items;
}

