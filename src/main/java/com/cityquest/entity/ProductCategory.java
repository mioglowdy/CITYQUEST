package com.cityquest.entity;

import lombok.Data;
import java.util.Date;

/**
 * 商品分类实体类
 */
@Data
public class ProductCategory {
    private Integer id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Integer status; // 0=禁用，1=启用
    private Date createTime;
    private Date updateTime;
}

