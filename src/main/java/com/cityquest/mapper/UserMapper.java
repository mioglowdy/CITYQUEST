package com.cityquest.mapper;

import com.cityquest.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {
    /**
     * 根据用户名查询用户
     */
    UserInfo selectByUsername(@Param("username") String username);

    /**
     * 根据ID查询用户
     */
    UserInfo selectById(@Param("id") Long id);

    /**
     * 新增用户
     */
    int insert(UserInfo userInfo);

    /**
     * 更新用户信息
     */
    int update(UserInfo userInfo);

    /**
     * 更新用户积分
     */
    int updatePoints(@Param("userId") Long userId, @Param("points") Integer points);

    /**
     * 查询积分排行榜
     */
    List<UserInfo> selectRankList(@Param("limit") Integer limit);

    /**
     * 查询用户列表
     */
    List<UserInfo> selectList(@Param("offset") Integer offset,
                              @Param("pageSize") Integer pageSize,
                              @Param("keyword") String keyword,
                              @Param("role") String role,
                              @Param("status") Integer status);

    /**
     * 查询用户总数
     */
    int selectCount(@Param("keyword") String keyword,
                    @Param("role") String role,
                    @Param("status") Integer status);

    /**
     * 删除用户
     */
    int delete(@Param("id") Long id);

    /**
     * 更新用户密码
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    
}