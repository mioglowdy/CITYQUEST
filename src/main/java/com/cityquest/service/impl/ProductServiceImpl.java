package com.cityquest.service.impl;

import com.cityquest.entity.ProductInfo;
import com.cityquest.mapper.ProductMapper;
import com.cityquest.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品服务实现类
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public Map<String, Object> getProductList(Integer categoryId, Integer status, String keyword, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<ProductInfo> productList = productMapper.selectList(categoryId, status, keyword, offset, pageSize);
        int total = productMapper.selectCount(categoryId, status, keyword);

        Map<String, Object> result = new HashMap<>();
        result.put("list", productList);
        result.put("items", productList);
        result.put("total", total);
        return result;
    }

    @Override
    public ProductInfo getProductById(Integer id) {
        return productMapper.selectById(id);
    }

    @Override
    public boolean createProduct(ProductInfo product) {
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getStock() == null) {
            product.setStock(0);
        }
        if (product.getLimitPerUser() == null) {
            product.setLimitPerUser(0);
        }
        product.setCreateTime(new Date());
        product.setUpdateTime(new Date());
        return productMapper.insert(product) > 0;
    }

    @Override
    public boolean updateProduct(ProductInfo product) {
        product.setUpdateTime(new Date());
        return productMapper.update(product) > 0;
    }

    @Override
    public boolean deleteProduct(Integer id) {
        return productMapper.delete(id) > 0;
    }

    @Override
    public boolean updateProductStatus(Integer id, Integer status) {
        ProductInfo product = new ProductInfo();
        product.setId(id);
        product.setStatus(status);
        product.setUpdateTime(new Date());
        return productMapper.update(product) > 0;
    }
}

