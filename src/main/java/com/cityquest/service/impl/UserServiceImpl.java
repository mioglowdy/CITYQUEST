package com.cityquest.service.impl;

import com.cityquest.entity.UserInfo;
import com.cityquest.entity.dto.LoginRequest;
import com.cityquest.entity.dto.RegisterRequest;
import com.cityquest.mapper.UserMapper;
import com.cityquest.service.UserService;
import com.cityquest.service.OnlineUserService;
import com.cityquest.util.JwtUtil;
import com.cityquest.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private OnlineUserService onlineUserService;

    @Override
    public Map<String, Object> login(LoginRequest loginRequest) {
        // 查询用户
        UserInfo userInfo = userMapper.selectByUsername(loginRequest.getUsername());
        if (userInfo == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证密码
        String md5Password = DigestUtils.md5DigestAsHex(loginRequest.getPassword().getBytes());
        if (!md5Password.equals(userInfo.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        // 检查用户状态（允许status=0的用户登录，登录后会自动设置为1）
        // 注意：这里不再阻止status=0的用户登录，因为status表示在线状态，不是账号禁用状态
        
        // 生成token
        String token = jwtUtil.generateToken(userInfo.getId().toString(), userInfo.getRole());
        
        // 登录成功，更新用户状态为1（在线）
        userInfo.setStatus(1);
        userInfo.setUpdateTime(new Date());
        userMapper.update(userInfo);

        onlineUserService.markOnline(userInfo.getId(), token);
        
        // 返回登录信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return result;
    }

    @Override
    public boolean register(RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        UserInfo existingUser = userMapper.selectByUsername(registerRequest.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 创建新用户
        UserInfo userInfo = new UserInfo();
        // 使用雪花算法生成唯一ID
        userInfo.setId(snowflakeIdGenerator.nextId());
        userInfo.setUsername(registerRequest.getUsername());
        userInfo.setPassword(DigestUtils.md5DigestAsHex(registerRequest.getPassword().getBytes()));
        userInfo.setNickname(registerRequest.getNickname());
        userInfo.setEmail(registerRequest.getEmail());
        userInfo.setPhone(registerRequest.getPhone());
        userInfo.setRole("user");
        userInfo.setPoints(0);
        userInfo.setStatus(1);
        userInfo.setCreateTime(new Date());
        userInfo.setUpdateTime(new Date());
        
        return userMapper.insert(userInfo) > 0;
    }

    @Override
    public UserInfo getCurrentUserInfo(String token) {
        String userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("无效的token");
        }
        Long id = Long.parseLong(userId);
        return userMapper.selectById(id);
    }

    @Override
    public UserInfo getUserById(Long id) {
        UserInfo userInfo = userMapper.selectById(id);
        return userInfo;
    }

    @Override
    public boolean updateUser(UserInfo userInfo) {
        userInfo.setUpdateTime(new Date());
        return userMapper.update(userInfo) > 0;
    }

    @Override
    public List<UserInfo> getRankList(Integer limit) {
        return userMapper.selectRankList(limit);
    }

    @Override
    public Map<String, Object> getUserList(Integer page, Integer pageSize, String keyword, String role, Integer status) {
        int offset = (page - 1) * pageSize;
        List<UserInfo> userList = userMapper.selectList(offset, pageSize, keyword, role, status);
        int total = userMapper.selectCount(keyword, role, status);

        Map<String, Object> result = new HashMap<>();
        result.put("list", userList);
        result.put("items", userList); // 兼容前端需要的items字段
        result.put("total", total);
        return result;
    }

    @Override
    public boolean updatePoints(Long userId, Integer points) {
        return userMapper.updatePoints(userId, points) > 0;
    }

    @Override
    public boolean deleteUser(Long id) {
        // 检查是否为管理员用户
        UserInfo user = userMapper.selectById(id);
        if (user != null && "admin".equals(user.getRole())) {
            throw new RuntimeException("不能删除管理员用户");
        }
        return userMapper.delete(id) > 0;
    }

    @Override
    public boolean resetPassword(Long id, String newPassword) {
        // 检查是否为管理员用户
        UserInfo user = userMapper.selectById(id);
        if (user != null && "admin".equals(user.getRole())) {
            throw new RuntimeException("不能重置管理员用户密码");
        }
        String md5Password = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        return userMapper.updatePassword(id, md5Password) > 0;
    }

    @Override
    public boolean changePassword(Long id, String oldPassword, String newPassword) {
        UserInfo user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String oldMd5 = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        if (!oldMd5.equals(user.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }
        String newMd5 = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        return userMapper.updatePassword(id, newMd5) > 0;
    }
}