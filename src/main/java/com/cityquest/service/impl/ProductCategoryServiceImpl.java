package com.cityquest.service.impl;

import com.cityquest.entity.ProductCategory;
import com.cityquest.mapper.ProductCategoryMapper;
import com.cityquest.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 商品分类服务实现类
 */
@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {

    @Autowired
    private ProductCategoryMapper categoryMapper;

    @Override
    public List<ProductCategory> getAllCategories(Integer status) {
        return categoryMapper.selectAll(status);
    }

    @Override
    public ProductCategory getCategoryById(Integer id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public boolean createCategory(ProductCategory category) {
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        category.setCreateTime(new Date());
        category.setUpdateTime(new Date());
        return categoryMapper.insert(category) > 0;
    }

    @Override
    public boolean updateCategory(ProductCategory category) {
        category.setUpdateTime(new Date());
        return categoryMapper.update(category) > 0;
    }

    @Override
    public boolean deleteCategory(Integer id) {
        return categoryMapper.delete(id) > 0;
    }
}

