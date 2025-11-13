package com.cityquest.config;

import com.cityquest.entity.UserInfo;
import com.cityquest.service.UserService;
import com.cityquest.service.OnlineUserService;
import com.cityquest.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * JWT拦截器
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    @SuppressWarnings("unused")
    private JwtUtil jwtUtil;

    @Autowired
    @SuppressWarnings("unused")
    private UserService userService;

    @Autowired
    private OnlineUserService onlineUserService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // 处理OPTIONS预检请求
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        
        // 获取请求路径
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        // 移除context-path前缀，获取实际路径
        if (contextPath != null && !contextPath.isEmpty() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }
        
        // 排除静态资源路径，允许公开访问
        // 检查多种可能的路径格式
        if (requestPath != null) {
            // 检查 /uploads/ 路径（移除context-path后）
            if (requestPath.startsWith("/uploads/") || requestPath.equals("/uploads")) {
                return true;
            }
            // 检查完整路径（包含context-path的情况）
            String fullPath = request.getRequestURI();
            if (fullPath.contains("/uploads/") || fullPath.endsWith("/uploads")) {
                return true;
            }
            // 尝试解码URL编码的路径（处理中文文件名等情况）
            try {
                String decodedPath = java.net.URLDecoder.decode(requestPath, "UTF-8");
                if (decodedPath.startsWith("/uploads/") || decodedPath.equals("/uploads")) {
                    return true;
                }
                String decodedFullPath = java.net.URLDecoder.decode(fullPath, "UTF-8");
                if (decodedFullPath.contains("/uploads/") || decodedFullPath.endsWith("/uploads")) {
                    return true;
                }
            } catch (Exception e) {
                // 解码失败，继续使用原始路径检查
            }
        }
        
        // 排除登录、注册和退出登录接口
        if (requestPath != null && (requestPath.equals("/user/login") || requestPath.equals("/user/register") || requestPath.equals("/user/logout"))) {
            return true;
        }
        
        // 从请求头中获取token
        String authHeader = request.getHeader("Authorization");
        String token = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        // 如果没有token，返回401未授权
        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"未登录或token无效\"}");
            return false;
        }
        
        // 验证token
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"token无效或已过期\"}");
            return false;
        }
        
        // 检查用户状态：如果status=0（离线），强制下线
        try {
            String userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                Long userIdLong = Long.parseLong(userId);

                if (onlineUserService.isForceLoggedOut(userIdLong)) {
                    onlineUserService.markOffline(userIdLong);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"账号已被管理员强制下线，请重新登录\"}");
                    return false;
                }

                Map<String, String> sessionInfo = onlineUserService.getSessionInfo(userIdLong);
                String storedToken = sessionInfo.get("token");
                if (storedToken != null && !storedToken.isEmpty() && !storedToken.equals(token)) {
                    onlineUserService.markOffline(userIdLong);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"账号登录状态已更新，请重新登录\"}");
                    return false;
                }

                UserInfo user = userService.getUserById(userIdLong);
                if (user != null && user.getStatus() != null && user.getStatus() == 0) {
                    // 用户状态为0（离线），强制下线
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"账号已被强制下线，请重新登录\"}");
                    return false;
                }
            }
        } catch (Exception e) {
            // 检查失败，继续验证token（避免因检查错误导致正常用户无法访问）
        }
        
        // token有效且用户在线，允许请求通过
        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) throws Exception {
    }
}