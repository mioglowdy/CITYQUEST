package com.cityquest.mapper;

import com.cityquest.entity.ExchangeOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细Mapper接口
 */
@Mapper
public interface ExchangeOrderItemMapper {
    /**
     * 根据订单ID查询订单明细
     */
    List<ExchangeOrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 新增订单明细
     */
    int insert(ExchangeOrderItem item);

    /**
     * 批量新增订单明细
     */
    int insertBatch(@Param("items") List<ExchangeOrderItem> items);

    /**
     * 删除订单明细
     */
    int deleteByOrderId(@Param("orderId") Long orderId);
}

