package com.cityquest.service;

import com.cityquest.entity.ProductCategory;
import java.util.List;

/**
 * 商品分类服务接口
 */
public interface ProductCategoryService {
    /**
     * 获取所有分类
     */
    List<ProductCategory> getAllCategories(Integer status);

    /**
     * 根据ID获取分类
     */
    ProductCategory getCategoryById(Integer id);

    /**
     * 创建分类
     */
    boolean createCategory(ProductCategory category);

    /**
     * 更新分类
     */
    boolean updateCategory(ProductCategory category);

    /**
     * 删除分类
     */
    boolean deleteCategory(Integer id);
}

