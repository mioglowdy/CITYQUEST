package com.cityquest.entity;

import lombok.Data;
import java.util.Date;

/**
 * 商品信息实体类
 */
@Data
public class ProductInfo {
    private Integer id;
    private Integer categoryId;
    private String name;
    private String description;
    private String image;
    private Integer pointsPrice;
    private Integer stock;
    private Integer limitPerUser; // 每人限购数量（0表示不限购）
    private Integer status; // 0=下架，1=上架
    private Long createBy;
    private Date createTime;
    private Date updateTime;
    
    // 关联字段（用于查询）
    private String categoryName;
}

