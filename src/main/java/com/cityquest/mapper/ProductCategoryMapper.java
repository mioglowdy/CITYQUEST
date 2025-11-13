package com.cityquest.mapper;

import com.cityquest.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品分类Mapper接口
 */
@Mapper
public interface ProductCategoryMapper {
    /**
     * 根据ID查询分类
     */
    ProductCategory selectById(@Param("id") Integer id);

    /**
     * 查询所有分类（按排序顺序）
     */
    List<ProductCategory> selectAll(@Param("status") Integer status);

    /**
     * 新增分类
     */
    int insert(ProductCategory category);

    /**
     * 更新分类
     */
    int update(ProductCategory category);

    /**
     * 删除分类
     */
    int delete(@Param("id") Integer id);
}

