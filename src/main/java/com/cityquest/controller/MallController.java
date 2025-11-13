package com.cityquest.controller;

import com.cityquest.entity.ExchangeOrder;
import com.cityquest.entity.ProductInfo;
import com.cityquest.entity.ProductCategory;
import com.cityquest.service.ExchangeOrderService;
import com.cityquest.service.ProductService;
import com.cityquest.service.ProductCategoryService;
import com.cityquest.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商城控制器（用户端）
 */
@RestController
@RequestMapping("/mall")
public class MallController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductCategoryService categoryService;

    @Autowired
    private ExchangeOrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token != null && !token.isEmpty()) {
                String userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    return Long.parseLong(userId);
                }
            }
        } catch (Exception e) {
            // 忽略错误，返回null
        }
        return null;
    }

    /**
     * 获取商品分类列表
     */
    @GetMapping("/categories")
    public Map<String, Object> getCategories() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<ProductCategory> categories = categoryService.getAllCategories(1); // 只获取启用的分类
            response.put("success", true);
            response.put("data", categories);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取商品列表
     */
    @GetMapping("/products")
    public Map<String, Object> getProducts(@RequestParam(required = false) Integer categoryId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "12") Integer pageSize) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = productService.getProductList(categoryId, 1, keyword, page, pageSize); // 只获取上架商品
            response.put("success", true);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/products/{id}")
    public Map<String, Object> getProductDetail(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            ProductInfo product = productService.getProductById(id);
            if (product == null || product.getStatus() != 1) {
                response.put("success", false);
                response.put("message", "商品不存在或已下架");
                return response;
            }
            response.put("success", true);
            response.put("data", product);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 创建兑换订单
     */
    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) request.get("items");
            List<ExchangeOrderService.OrderItemRequest> items = itemsData.stream().map(item -> {
                ExchangeOrderService.OrderItemRequest itemRequest = new ExchangeOrderService.OrderItemRequest();
                itemRequest.setProductId(((Number) item.get("productId")).intValue());
                itemRequest.setQuantity(((Number) item.get("quantity")).intValue());
                return itemRequest;
            }).collect(Collectors.toList());

            String receiverName = (String) request.get("receiverName");
            String receiverPhone = (String) request.get("receiverPhone");
            String receiverAddress = (String) request.get("receiverAddress");
            String remark = (String) request.get("remark");

            ExchangeOrder order = orderService.createOrder(userId, items, receiverName, receiverPhone, receiverAddress, remark);
            response.put("success", true);
            response.put("data", order);
            response.put("message", "兑换成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取我的订单列表
     */
    @GetMapping("/orders")
    public Map<String, Object> getMyOrders(@RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            Map<String, Object> result = orderService.getOrderList(userId, status, null, page, pageSize);
            response.put("success", true);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/orders/{id}")
    public Map<String, Object> getOrderDetail(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            ExchangeOrder order = orderService.getOrderById(id);
            if (order == null || !order.getUserId().equals(userId)) {
                response.put("success", false);
                response.put("message", "订单不存在");
                return response;
            }

            response.put("success", true);
            response.put("data", order);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 取消订单
     */
    @PostMapping("/orders/{id}/cancel")
    public Map<String, Object> cancelOrder(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "请先登录");
                return response;
            }

            boolean result = orderService.cancelOrder(id, userId);
            response.put("success", result);
            response.put("message", result ? "订单已取消" : "取消失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}

