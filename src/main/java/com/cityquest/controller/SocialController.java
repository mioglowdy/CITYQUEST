package com.cityquest.controller;

import com.cityquest.service.SocialService;
import com.cityquest.service.QiniuService;
import com.cityquest.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 社交功能控制器
 */
@RestController
@RequestMapping("/social")
public class SocialController {

    @Autowired
    private SocialService socialService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private QiniuService qiniuService;

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);
            return userId != null ? Long.parseLong(userId) : null;
        }
        return null;
    }

    // ==================== 好友关注功能 ====================

    /**
     * 关注用户
     */
    @PostMapping("/follow")
    public Map<String, Object> followUser(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long followerId = getCurrentUserId(request);
            if (followerId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            // 处理followeeId（可能是Integer、Long或String类型）
            Object followeeIdObj = body.get("followeeId");
            Long followeeId = null;
            if (followeeIdObj != null) {
                if (followeeIdObj instanceof Number) {
                    followeeId = ((Number) followeeIdObj).longValue();
                } else if (followeeIdObj instanceof String) {
                    try {
                        followeeId = Long.parseLong((String) followeeIdObj);
                    } catch (NumberFormatException e) {
                        response.put("success", false);
                        response.put("message", "无效的用户ID格式");
                        return response;
                    }
                } else {
                    followeeId = ((Number) followeeIdObj).longValue();
                }
            }
            if (followeeId == null) {
                response.put("success", false);
                response.put("message", "被关注者ID不能为空");
                return response;
            }

            // 直接使用Long类型的ID（SocialService接口已支持Long类型）
            boolean result = socialService.followUser(followerId, followeeId);
            response.put("success", result);
            response.put("message", result ? "关注成功" : "关注失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 取消关注
     */
    @PostMapping("/unfollow")
    public Map<String, Object> unfollowUser(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long followerId = getCurrentUserId(request);
            if (followerId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            // 处理followeeId（可能是Integer、Long或String类型）
            Object followeeIdObj = body.get("followeeId");
            Long followeeId = null;
            if (followeeIdObj != null) {
                if (followeeIdObj instanceof Number) {
                    followeeId = ((Number) followeeIdObj).longValue();
                } else if (followeeIdObj instanceof String) {
                    try {
                        followeeId = Long.parseLong((String) followeeIdObj);
                    } catch (NumberFormatException e) {
                        response.put("success", false);
                        response.put("message", "无效的用户ID格式");
                        return response;
                    }
                } else {
                    followeeId = ((Number) followeeIdObj).longValue();
                }
            }
            if (followeeId == null) {
                response.put("success", false);
                response.put("message", "被关注者ID不能为空");
                return response;
            }

            // 直接使用Long类型的ID
            boolean result = socialService.unfollowUser(followerId, followeeId);
            response.put("success", result);
            response.put("message", result ? "取消关注成功" : "取消关注失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 检查是否已关注
     */
    @GetMapping("/follow/check")
    public Map<String, Object> checkFollow(@RequestParam Long followeeId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long followerId = getCurrentUserId(request);
            if (followerId == null) {
                response.put("success", false);
                response.put("isFollowing", false);
                return response;
            }

            // 直接使用Long类型的ID
            boolean isFollowing = socialService.isFollowing(followerId, followeeId);
            response.put("success", true);
            response.put("isFollowing", isFollowing);
        } catch (Exception e) {
            response.put("success", false);
            response.put("isFollowing", false);
        }
        return response;
    }

    /**
     * 获取关注列表
     */
    @GetMapping("/following")
    public Map<String, Object> getFollowingList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long currentUserId = userId != null ? userId : getCurrentUserId(request);
            if (currentUserId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            // 直接使用Long类型的ID
            Map<String, Object> result = socialService.getFollowingList(currentUserId, page, pageSize);
            response.put("success", true);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("items", java.util.List.of());
            response.put("total", 0);
        }
        return response;
    }

    /**
     * 获取粉丝列表
     */
    @GetMapping("/followers")
    public Map<String, Object> getFollowerList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long currentUserId = userId != null ? userId : getCurrentUserId(request);
            if (currentUserId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            // 直接使用Long类型的ID
            Map<String, Object> result = socialService.getFollowerList(currentUserId, page, pageSize);
            response.put("success", true);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("items", java.util.List.of());
            response.put("total", 0);
        }
        return response;
    }

    // ==================== 动态功能 ====================

    /**
     * 上传动态图片（使用七牛云）
     */
    @PostMapping(value = "/feed/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadFeedImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            if (file == null || file.isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择要上传的图片");
                return response;
            }

            // 上传到七牛云
            String url = qiniuService.uploadFile(file, "feeds", null);

            response.put("success", true);
            response.put("message", "上传成功");
            response.put("url", url);
            response.put("path", url); // 统一使用完整URL
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "图片上传失败: " + e.getMessage());
            return response;
        }
    }

    /**
     * 发布动态
     */
    @PostMapping("/feed")
    public Map<String, Object> publishFeed(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            String content = (String) body.get("content");
            String imageUrl = (String) body.get("imageUrl");
            if (imageUrl != null && !imageUrl.isBlank()) {
                if (imageUrl.startsWith("data:")) {
                    response.put("success", false);
                    response.put("message", "图片上传功能暂未开放，请先移除图片或稍后再试");
                    return response;
                }
                if (imageUrl.length() > 512) {
                    response.put("success", false);
                    response.put("message", "图片地址长度超出限制，请确认图片已成功上传");
                    return response;
                }
            }
            Integer taskId = body.get("taskId") != null ? ((Number) body.get("taskId")).intValue() : null;
            Integer recordId = body.get("recordId") != null ? ((Number) body.get("recordId")).intValue() : null;
            Boolean isPublic = body.get("isPublic") != null ? (Boolean) body.get("isPublic") : true;

            boolean result = socialService.publishFeed(userId, content, imageUrl, taskId, recordId, isPublic);
            response.put("success", result);
            response.put("message", result ? "发布成功" : "发布失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取动态列表
     */
    @GetMapping("/feed")
    public Map<String, Object> getFeedList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null && !"public".equals(type)) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            Map<String, Object> result = socialService.getFeedList(userId, page, pageSize, type);
            response.put("success", true);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("items", java.util.List.of());
            response.put("total", 0);
        }
        return response;
    }

    /**
     * 点赞动态
     */
    @PostMapping("/feed/{feedId}/like")
    public Map<String, Object> likeFeed(@PathVariable Integer feedId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            boolean result = socialService.likeFeed(feedId, userId);
            response.put("success", result);
            response.put("message", result ? "点赞成功" : "点赞失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 取消点赞
     */
    @PostMapping("/feed/{feedId}/unlike")
    public Map<String, Object> unlikeFeed(@PathVariable Integer feedId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            boolean result = socialService.unlikeFeed(feedId, userId);
            response.put("success", result);
            response.put("message", result ? "取消点赞成功" : "取消点赞失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 评论动态
     */
    @PostMapping("/feed/{feedId}/comment")
    public Map<String, Object> commentFeed(
            @PathVariable Integer feedId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            String content = (String) body.get("content");
            if (content == null || content.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "评论内容不能为空");
                return response;
            }

            Integer parentId = body.get("parentId") != null ? ((Number) body.get("parentId")).intValue() : null;

            boolean result = socialService.commentFeed(feedId, userId, content, parentId);
            response.put("success", result);
            response.put("message", result ? "评论成功" : "评论失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取评论列表
     */
    @GetMapping("/feed/{feedId}/comments")
    public Map<String, Object> getCommentList(
            @PathVariable Integer feedId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = socialService.getCommentList(feedId, page, pageSize);
            response.put("success", true);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("items", java.util.List.of());
            response.put("total", 0);
        }
        return response;
    }

    /**
     * 删除动态
     */
    @DeleteMapping("/feed/{feedId}")
    public Map<String, Object> deleteFeed(@PathVariable Integer feedId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            boolean result = socialService.deleteFeed(feedId, userId);
            response.put("success", result);
            response.put("message", result ? "删除成功" : "删除失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    // ==================== 通知功能 ====================

    /**
     * 获取通知列表
     */
    @GetMapping("/notifications")
    public Map<String, Object> getNotificationList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            Map<String, Object> result = socialService.getNotificationList(userId, page, pageSize);
            response.put("success", true);
            response.putAll(result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("items", java.util.List.of());
            response.put("total", 0);
            response.put("unreadCount", 0);
        }
        return response;
    }

    /**
     * 标记通知为已读
     */
    @PostMapping("/notifications/{id}/read")
    public Map<String, Object> markNotificationAsRead(@PathVariable Integer id, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userIdLong = getCurrentUserId(request);
            if (userIdLong == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }
            Integer userId = userIdLong > Integer.MAX_VALUE ? null : userIdLong.intValue();
            if (userId == null) {
                response.put("success", false);
                response.put("message", "用户ID超出范围");
                return response;
            }

            boolean result = socialService.markNotificationAsRead(id);
            response.put("success", result);
            response.put("message", result ? "标记成功" : "标记失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 标记所有通知为已读
     */
    @PostMapping("/notifications/read-all")
    public Map<String, Object> markAllNotificationsAsRead(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "未登录");
                return response;
            }

            boolean result = socialService.markAllNotificationsAsRead(userId);
            response.put("success", result);
            response.put("message", result ? "全部标记为已读" : "标记失败");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取未读通知数
     */
    @GetMapping("/notifications/unread-count")
    public Map<String, Object> getUnreadNotificationCount(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                response.put("success", false);
                response.put("unreadCount", 0);
                return response;
            }

            int count = socialService.getUnreadNotificationCount(userId);
            response.put("success", true);
            response.put("unreadCount", count);
        } catch (Exception e) {
            response.put("success", false);
            response.put("unreadCount", 0);
        }
        return response;
    }
}

