package com.cityquest.util;

import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 * 生成64位唯一ID，包含时间戳、机器ID、序列号
 * 
 * ID结构（64位）：
 * 0 - 41位时间戳（毫秒级，可用约69年）
 * 42 - 52位机器ID（10位，支持1024台机器）
 * 53 - 63位序列号（12位，每毫秒可生成4096个ID）
 */
@Component
public class SnowflakeIdGenerator {
    
    // 起始时间戳（2024-01-01 00:00:00）
    private static final long START_TIMESTAMP = 1704067200000L;
    
    // 机器ID占用的位数
    private static final long MACHINE_ID_BITS = 10L;
    
    // 序列号占用的位数
    private static final long SEQUENCE_BITS = 12L;
    
    // 机器ID的最大值（1023）
    private static final long MAX_MACHINE_ID = (1L << MACHINE_ID_BITS) - 1;
    
    // 序列号的最大值（4095）
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    
    // 机器ID向左移12位
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    
    // 时间戳向左移22位（10+12）
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    
    // 机器ID（可以通过配置文件或环境变量设置，这里使用默认值）
    private final long machineId;
    
    // 序列号
    private long sequence = 0L;
    
    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;
    
    /**
     * 构造函数，使用默认机器ID（从系统属性或环境变量获取，默认为1）
     */
    public SnowflakeIdGenerator() {
        // 尝试从系统属性获取机器ID
        String machineIdStr = System.getProperty("snowflake.machine.id");
        if (machineIdStr == null || machineIdStr.isEmpty()) {
            // 尝试从环境变量获取
            machineIdStr = System.getenv("SNOWFLAKE_MACHINE_ID");
        }
        
        long id = 1L; // 默认机器ID
        if (machineIdStr != null && !machineIdStr.isEmpty()) {
            try {
                id = Long.parseLong(machineIdStr);
            } catch (NumberFormatException e) {
                System.err.println("无效的机器ID配置，使用默认值1: " + machineIdStr);
            }
        }
        
        if (id < 0 || id > MAX_MACHINE_ID) {
            throw new IllegalArgumentException("机器ID必须在0到" + MAX_MACHINE_ID + "之间");
        }
        
        this.machineId = id;
        System.out.println("雪花算法ID生成器初始化，机器ID: " + this.machineId);
    }
    
    /**
     * 生成下一个ID
     * 
     * @return 64位唯一ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("系统时钟回退，拒绝生成ID。回退时间: " + (lastTimestamp - timestamp) + "毫秒");
        }
        
        // 如果是同一毫秒内生成的，则进行毫秒内序列
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒，获得新的时间戳
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，毫秒内序列重置
            sequence = 0L;
        }
        
        // 上次生成ID的时间戳
        lastTimestamp = timestamp;
        
        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence;
    }
    
    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * 
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 当前时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
    
    /**
     * 从ID中解析出时间戳
     * 
     * @param id 雪花算法生成的ID
     * @return 时间戳（毫秒）
     */
    public static long getTimestamp(long id) {
        return ((id >> TIMESTAMP_SHIFT) + START_TIMESTAMP);
    }
    
    /**
     * 从ID中解析出机器ID
     * 
     * @param id 雪花算法生成的ID
     * @return 机器ID
     */
    public static long getMachineId(long id) {
        return (id >> MACHINE_ID_SHIFT) & MAX_MACHINE_ID;
    }
    
    /**
     * 从ID中解析出序列号
     * 
     * @param id 雪花算法生成的ID
     * @return 序列号
     */
    public static long getSequence(long id) {
        return id & MAX_SEQUENCE;
    }
}

