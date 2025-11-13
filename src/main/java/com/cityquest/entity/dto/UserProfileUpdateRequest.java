package com.cityquest.entity.dto;

import lombok.Data;

/**
 * 用户资料更新请求
 */
@Data
public class UserProfileUpdateRequest {
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
}

