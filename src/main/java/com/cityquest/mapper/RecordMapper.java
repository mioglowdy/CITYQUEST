package com.cityquest.mapper;

import com.cityquest.entity.RecordInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 打卡记录Mapper接口
 */
@Mapper
public interface RecordMapper {
    /**
     * 根据ID查询记录
     */
    RecordInfo selectById(@Param("id") Integer id);

    /**
     * 查询用户的打卡记录
     */
    List<RecordInfo> selectByUserId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    /**
     * 查询任务的打卡记录
     */
    List<RecordInfo> selectByTaskId(@Param("taskId") Integer taskId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    /**
     * 查询待审核的记录
     */
    List<RecordInfo> selectAuditList(@Param("status") Integer status, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    /**
     * 新增打卡记录
     */
    int insert(RecordInfo recordInfo);

    /**
     * 更新审核状态
     */
    int updateAuditStatus(@Param("id") Integer id, @Param("status") Integer status, @Param("remark") String remark, @Param("adminId") Long adminId);

    /**
     * 查询记录总数
     */
    int selectCount(@Param("userId") Long userId, @Param("taskId") Integer taskId, @Param("status") Integer status);

    /**
     * 检查用户是否已完成该任务
     */
    int checkUserTaskCompletion(@Param("userId") Long userId, @Param("taskId") Integer taskId);

    /**
     * 删除记录（根据ID）
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 批量删除记录
     */
    int batchDelete(@Param("ids") List<Integer> ids);
    
}