package com.cityquest.entity;

import lombok.Data;
import java.util.Date;

/**
 * 订单明细实体类
 */
@Data
public class ExchangeOrderItem {
    private Long id;
    private Long orderId;
    private Integer productId;
    private String productName;
    private String productImage;
    private Integer pointsPrice;
    private Integer quantity;
    private Integer subtotalPoints;
    private Date createTime;
}

