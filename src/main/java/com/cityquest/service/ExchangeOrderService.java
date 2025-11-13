package com.cityquest.service;

import com.cityquest.entity.ExchangeOrder;
import java.util.List;
import java.util.Map;

/**
 * 兑换订单服务接口
 */
public interface ExchangeOrderService {
    /**
     * 创建兑换订单
     */
    ExchangeOrder createOrder(Long userId, List<OrderItemRequest> items, String receiverName, String receiverPhone, String receiverAddress, String remark);

    /**
     * 获取订单列表
     */
    Map<String, Object> getOrderList(Long userId, Integer status, String keyword, Integer page, Integer pageSize);

    /**
     * 根据ID获取订单详情
     */
    ExchangeOrder getOrderById(Long id);

    /**
     * 根据订单号获取订单
     */
    ExchangeOrder getOrderByOrderNo(String orderNo);

    /**
     * 更新订单状态
     */
    boolean updateOrderStatus(Long id, Integer status);

    /**
     * 更新物流信息
     */
    boolean updateLogisticsInfo(Long id, String logisticsInfo);

    /**
     * 取消订单
     */
    boolean cancelOrder(Long id, Long userId);

    /**
     * 订单项请求
     */
    class OrderItemRequest {
        private Integer productId;
        private Integer quantity;

        public Integer getProductId() {
            return productId;
        }

        public void setProductId(Integer productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}

