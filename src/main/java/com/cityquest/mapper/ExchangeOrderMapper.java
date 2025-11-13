package com.cityquest.mapper;

import com.cityquest.entity.ExchangeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 兑换订单Mapper接口
 */
@Mapper
public interface ExchangeOrderMapper {
    /**
     * 根据ID查询订单
     */
    ExchangeOrder selectById(@Param("id") Long id);

    /**
     * 根据订单号查询订单
     */
    ExchangeOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询订单列表
     */
    List<ExchangeOrder> selectList(@Param("userId") Long userId,
                                   @Param("status") Integer status,
                                   @Param("keyword") String keyword,
                                   @Param("offset") Integer offset,
                                   @Param("pageSize") Integer pageSize);

    /**
     * 查询订单总数
     */
    int selectCount(@Param("userId") Long userId,
                   @Param("status") Integer status,
                   @Param("keyword") String keyword);

    /**
     * 新增订单
     */
    int insert(ExchangeOrder order);

    /**
     * 更新订单
     */
    int update(ExchangeOrder order);

    /**
     * 删除订单
     */
    int delete(@Param("id") Long id);
}

