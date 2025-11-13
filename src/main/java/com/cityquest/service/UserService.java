package com.cityquest.service;

import com.cityquest.entity.UserInfo;
import com.cityquest.entity.dto.LoginRequest;
import com.cityquest.entity.dto.RegisterRequest;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {
    /**
     * 用户登录
     */
    Map<String, Object> login(LoginRequest loginRequest);

    /**
     * 用户注册
     */
    boolean register(RegisterRequest registerRequest);

    /**
     * 根据token获取当前用户信息
     */
    UserInfo getCurrentUserInfo(String token);

    /**
     * 根据ID获取用户信息
     */
    UserInfo getUserById(Long id);

    /**
     * 更新用户信息
     */
    boolean updateUser(UserInfo userInfo);

    /**
     * 获取积分排行榜
     */
    List<UserInfo> getRankList(Integer limit);

    /**
     * 获取用户列表
     */
    Map<String, Object> getUserList(Integer page, Integer pageSize, String keyword, String role, Integer status);

    /**
     * 更新用户积分
     */
    boolean updatePoints(Long userId, Integer points);

    /**
     * 删除用户
     */
    boolean deleteUser(Long id);

    /**
     * 重置用户密码
     */
    boolean resetPassword(Long id, String newPassword);

    /**
     * 修改密码（需验证旧密码）
     */
    boolean changePassword(Long id, String oldPassword, String newPassword);
}