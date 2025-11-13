package com.cityquest.service;

import com.cityquest.entity.ProductInfo;
import java.util.Map;

/**
 * 商品服务接口
 */
public interface ProductService {
    /**
     * 获取商品列表
     */
    Map<String, Object> getProductList(Integer categoryId, Integer status, String keyword, Integer page, Integer pageSize);

    /**
     * 根据ID获取商品详情
     */
    ProductInfo getProductById(Integer id);

    /**
     * 创建商品
     */
    boolean createProduct(ProductInfo product);

    /**
     * 更新商品
     */
    boolean updateProduct(ProductInfo product);

    /**
     * 删除商品
     */
    boolean deleteProduct(Integer id);

    /**
     * 上架/下架商品
     */
    boolean updateProductStatus(Integer id, Integer status);
}

