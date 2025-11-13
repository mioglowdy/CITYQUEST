package com.cityquest.util;

/**
 * 距离计算工具类
 * 使用Haversine公式计算两点间的距离
 */
public class DistanceUtil {
    
    // 地球半径（米）
    private static final double EARTH_RADIUS = 6371000.0;
    
    /**
     * 计算两个经纬度点之间的距离（米）
     * 
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 距离（米）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 将角度转换为弧度
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);
        
        // Haversine公式
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        // 计算距离
        double distance = EARTH_RADIUS * c;
        
        return distance;
    }
    
    /**
     * 检查两个位置是否在指定范围内
     * 
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @param maxDistance 最大允许距离（米）
     * @return 如果在范围内返回true，否则返回false
     */
    public static boolean isWithinRange(double lat1, double lon1, double lat2, double lon2, double maxDistance) {
        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance <= maxDistance;
    }
    
    /**
     * 格式化距离显示
     * 
     * @param distanceInMeters 距离（米）
     * @return 格式化后的距离字符串
     */
    public static String formatDistance(double distanceInMeters) {
        if (distanceInMeters < 1000) {
            return String.format("%.0f米", distanceInMeters);
        } else {
            return String.format("%.2f公里", distanceInMeters / 1000.0);
        }
    }
}

