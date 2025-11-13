package com.cityquest.service.impl;

import com.cityquest.entity.ExchangeOrder;
import com.cityquest.entity.ExchangeOrderItem;
import com.cityquest.entity.ProductInfo;
import com.cityquest.entity.UserInfo;
import com.cityquest.mapper.ExchangeOrderItemMapper;
import com.cityquest.mapper.ExchangeOrderMapper;
import com.cityquest.mapper.ProductMapper;
import com.cityquest.mapper.UserMapper;
import com.cityquest.service.ExchangeOrderService;
import com.cityquest.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 兑换订单服务实现类
 */
@Service
public class ExchangeOrderServiceImpl implements ExchangeOrderService {

    @Autowired
    private ExchangeOrderMapper orderMapper;

    @Autowired
    private ExchangeOrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    @Transactional
    public ExchangeOrder createOrder(Long userId, List<OrderItemRequest> items, String receiverName, String receiverPhone, String receiverAddress, String remark) {
        // 1. 验证用户积分是否足够
        UserInfo user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 计算总积分并验证商品
        int totalPoints = 0;
        List<ExchangeOrderItem> orderItems = new ArrayList<>();
        
        for (OrderItemRequest itemRequest : items) {
            ProductInfo product = productMapper.selectById(itemRequest.getProductId());
            if (product == null) {
                throw new RuntimeException("商品不存在: " + itemRequest.getProductId());
            }
            if (product.getStatus() != 1) {
                throw new RuntimeException("商品已下架: " + product.getName());
            }
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new RuntimeException("商品库存不足: " + product.getName());
            }
            
            // 检查限购
            if (product.getLimitPerUser() > 0) {
                int userExchangeCount = productMapper.selectUserExchangeCount(userId, product.getId());
                if (userExchangeCount + itemRequest.getQuantity() > product.getLimitPerUser()) {
                    throw new RuntimeException("超过限购数量: " + product.getName() + "，限购" + product.getLimitPerUser() + "件");
                }
            }
            
            int subtotal = product.getPointsPrice() * itemRequest.getQuantity();
            totalPoints += subtotal;
            
            // 创建订单项
            ExchangeOrderItem item = new ExchangeOrderItem();
            item.setId(snowflakeIdGenerator.nextId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImage(product.getImage());
            item.setPointsPrice(product.getPointsPrice());
            item.setQuantity(itemRequest.getQuantity());
            item.setSubtotalPoints(subtotal);
            item.setCreateTime(new Date());
            orderItems.add(item);
        }

        // 3. 检查用户积分
        if (user.getPoints() < totalPoints) {
            throw new RuntimeException("积分不足，需要" + totalPoints + "积分，当前拥有" + user.getPoints() + "积分");
        }

        // 4. 创建订单
        Long orderId = snowflakeIdGenerator.nextId();
        String orderNo = "EX" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
        
        ExchangeOrder order = new ExchangeOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setTotalPoints(totalPoints);
        order.setStatus(0); // 待发货
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setRemark(remark);
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        
        orderMapper.insert(order);

        // 5. 创建订单明细
        for (ExchangeOrderItem item : orderItems) {
            item.setOrderId(orderId);
            orderItemMapper.insert(item);
            
            // 扣减库存
            productMapper.updateStock(item.getProductId(), item.getQuantity());
        }

        // 6. 扣减用户积分
        userMapper.updatePoints(userId, -totalPoints);

        // 7. 返回完整订单信息
        return getOrderById(orderId);
    }

    @Override
    public Map<String, Object> getOrderList(Long userId, Integer status, String keyword, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<ExchangeOrder> orderList = orderMapper.selectList(userId, status, keyword, offset, pageSize);
        int total = orderMapper.selectCount(userId, status, keyword);

        // 加载订单明细
        for (ExchangeOrder order : orderList) {
            List<ExchangeOrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            order.setItems(items);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", orderList);
        result.put("items", orderList);
        result.put("total", total);
        return result;
    }

    @Override
    public ExchangeOrder getOrderById(Long id) {
        ExchangeOrder order = orderMapper.selectById(id);
        if (order != null) {
            List<ExchangeOrderItem> items = orderItemMapper.selectByOrderId(id);
            order.setItems(items);
        }
        return order;
    }

    @Override
    public ExchangeOrder getOrderByOrderNo(String orderNo) {
        ExchangeOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order != null) {
            List<ExchangeOrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            order.setItems(items);
        }
        return order;
    }

    @Override
    @Transactional
    public boolean updateOrderStatus(Long id, Integer status) {
        ExchangeOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        order.setStatus(status);
        order.setUpdateTime(new Date());
        return orderMapper.update(order) > 0;
    }

    @Override
    public boolean updateLogisticsInfo(Long id, String logisticsInfo) {
        ExchangeOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        order.setLogisticsInfo(logisticsInfo);
        order.setUpdateTime(new Date());
        return orderMapper.update(order) > 0;
    }

    @Override
    @Transactional
    public boolean cancelOrder(Long id, Long userId) {
        ExchangeOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权取消此订单");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("只能取消待发货的订单");
        }

        // 恢复库存
        List<ExchangeOrderItem> items = orderItemMapper.selectByOrderId(id);
        for (ExchangeOrderItem item : items) {
            ProductInfo product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                product.setUpdateTime(new Date());
                productMapper.update(product);
            }
        }

        // 退还积分
        userMapper.updatePoints(userId, order.getTotalPoints());

        // 更新订单状态
        order.setStatus(3); // 已取消
        order.setUpdateTime(new Date());
        return orderMapper.update(order) > 0;
    }
}

