package com.cityquest.mapper;

import com.cityquest.entity.TaskInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务Mapper接口
 */
@Mapper
public interface TaskMapper {
    /**
     * 根据ID查询任务
     */
    TaskInfo selectById(@Param("id") Integer id);

    /**
     * 查询任务列表
     */
    List<TaskInfo> selectList(@Param("type") Integer type, @Param("status") Integer status, @Param("keyword") String keyword, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    /**
     * 查询任务总数
     */
    int selectCount(@Param("type") Integer type, @Param("status") Integer status, @Param("keyword") String keyword);

    /**
     * 新增任务
     */
    int insert(TaskInfo taskInfo);

    /**
     * 更新任务
     */
    int update(TaskInfo taskInfo);

    /**
     * 删除任务
     */
    int delete(@Param("id") Integer id);

    /**
     * 更新任务完成数
     */
    int updateCompletionCount(@Param("id") Integer id);

    /**
     * 根据地理位置查询附近任务
     */
    List<TaskInfo> selectNearbyTasks(@Param("longitude") Double longitude, @Param("latitude") Double latitude, @Param("radius") Double radius);
}