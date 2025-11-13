package com.cityquest.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

/**
 * 七牛云存储服务接口
 */
public interface QiniuService {
    /**
     * 上传文件到七牛云
     * @param file 文件
     * @param fileName 文件名（可选，不传则自动生成）
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String fileName);

    /**
     * 上传文件到七牛云（指定目录前缀）
     * @param file 文件
     * @param directory 目录前缀（如：records, tasks, feeds）
     * @param fileName 文件名（可选，不传则自动生成）
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String directory, String fileName);

    /**
     * 上传文件流到七牛云
     * @param inputStream 文件输入流
     * @param fileName 文件名
     * @param contentType 文件类型
     * @return 文件访问URL
     */
    String uploadFile(InputStream inputStream, String fileName, String contentType);

    /**
     * 删除七牛云文件
     * @param fileName 文件名
     * @return 是否删除成功
     */
    boolean deleteFile(String fileName);

    /**
     * 获取文件访问URL
     * @param fileName 文件名
     * @return 文件访问URL
     */
    String getFileUrl(String fileName);
}

