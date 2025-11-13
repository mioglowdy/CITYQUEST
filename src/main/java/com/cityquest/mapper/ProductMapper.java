package com.cityquest.mapper;

import com.cityquest.entity.ProductInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品Mapper接口
 */
@Mapper
public interface ProductMapper {
    /**
     * 根据ID查询商品
     */
    ProductInfo selectById(@Param("id") Integer id);

    /**
     * 查询商品列表
     */
    List<ProductInfo> selectList(@Param("categoryId") Integer categoryId,
                                 @Param("status") Integer status,
                                 @Param("keyword") String keyword,
                                 @Param("offset") Integer offset,
                                 @Param("pageSize") Integer pageSize);

    /**
     * 查询商品总数
     */
    int selectCount(@Param("categoryId") Integer categoryId,
                    @Param("status") Integer status,
                    @Param("keyword") String keyword);

    /**
     * 新增商品
     */
    int insert(ProductInfo product);

    /**
     * 更新商品
     */
    int update(ProductInfo product);

    /**
     * 删除商品
     */
    int delete(@Param("id") Integer id);

    /**
     * 更新库存
     */
    int updateStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

    /**
     * 查询用户已兑换数量（用于限购检查）
     */
    int selectUserExchangeCount(@Param("userId") Long userId, @Param("productId") Integer productId);
}

