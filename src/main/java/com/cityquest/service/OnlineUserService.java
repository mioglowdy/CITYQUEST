package com.cityquest.service;

import java.util.Map;
import java.util.Set;

/**
 * 在线用户服务（基于 Redis）
 */
public interface OnlineUserService {

    /**
     * 标记用户上线
     *
     * @param userId 用户ID
     * @param token  登录token
     */
    void markOnline(Long userId, String token);

    /**
     * 标记用户下线
     *
     * @param userId 用户ID
     */
    void markOffline(Long userId);

    /**
     * 获取在线用户ID集合
     */
    Set<String> getOnlineUserIds();

    /**
     * 获取用户会话信息
     *
     * @param userId 用户ID
     */
    Map<String, String> getSessionInfo(Long userId);

    /**
     * 强制下线用户
     *
     * @param userId 用户ID
     */
    void forceLogout(Long userId);

    /**
     * 检查是否被强制下线
     *
     * @param userId 用户ID
     */
    boolean isForceLoggedOut(Long userId);

    /**
     * 清除强制下线标记
     *
     * @param userId 用户ID
     */
    void clearForceLogoutMark(Long userId);
}

