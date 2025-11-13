package com.cityquest.service.impl;

import com.cityquest.service.OnlineUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 在线用户服务实现
 */
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private static final String ONLINE_USERS_KEY = "online_users";
    private static final String USER_SESSION_KEY_PREFIX = "user_session:";
    private static final String FORCE_LOGOUT_KEY_PREFIX = "force_logout:";
    private static final Duration SESSION_TTL = Duration.ofHours(4);
    private static final Duration FORCE_LOGOUT_TTL = Duration.ofMinutes(10);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void markOnline(Long userId, String token) {
        if (userId == null) {
            return;
        }
        String userIdStr = String.valueOf(userId);
        stringRedisTemplate.opsForSet().add(ONLINE_USERS_KEY, userIdStr);
        if (StringUtils.hasText(token)) {
            Map<String, String> session = new HashMap<>();
            session.put("token", token);
            session.put("loginTime", String.valueOf(System.currentTimeMillis()));
            session.put("client", "web");
            String key = USER_SESSION_KEY_PREFIX + userIdStr;
            stringRedisTemplate.opsForHash().putAll(key, session);
            stringRedisTemplate.expire(key, SESSION_TTL);
        }
        clearForceLogoutMark(userId);
    }

    @Override
    public void markOffline(Long userId) {
        if (userId == null) {
            return;
        }
        String userIdStr = String.valueOf(userId);
        stringRedisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userIdStr);
        stringRedisTemplate.delete(USER_SESSION_KEY_PREFIX + userIdStr);
        clearForceLogoutMark(userId);
    }

    @Override
    public Set<String> getOnlineUserIds() {
        Set<String> members = stringRedisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        return members != null ? members : Collections.emptySet();
    }

    @Override
    public Map<String, String> getSessionInfo(Long userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }
        String key = USER_SESSION_KEY_PREFIX + userId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
        return result;
    }

    @Override
    public void forceLogout(Long userId) {
        if (userId == null) {
            return;
        }
        markOffline(userId);
        String key = FORCE_LOGOUT_KEY_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, "1", FORCE_LOGOUT_TTL);
    }

    @Override
    public boolean isForceLoggedOut(Long userId) {
        if (userId == null) {
            return false;
        }
        String key = FORCE_LOGOUT_KEY_PREFIX + userId;
        Boolean exists = stringRedisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void clearForceLogoutMark(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(FORCE_LOGOUT_KEY_PREFIX + userId);
    }
}

