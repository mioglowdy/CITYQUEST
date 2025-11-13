package com.cityquest.entity;

import lombok.Data;
import java.util.Date;

/**
 * 用户信息实体类
 */
@Data
public class UserInfo {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private Integer points;
    private String role;  // user, admin
    private String email;
    private String phone;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}