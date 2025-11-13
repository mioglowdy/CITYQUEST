package com.cityquest.service.impl;

import com.cityquest.service.QiniuService;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 七牛云存储服务实现类
 */
@Service
public class QiniuServiceImpl implements QiniuService {

    @Value("${qiniu.access-key}")
    private String accessKey;

    @Value("${qiniu.secret-key}")
    private String secretKey;

    @Value("${qiniu.bucket}")
    private String bucket;

    @Value("${qiniu.domain:}")
    private String domain;

    @Value("${qiniu.use-https:true}")
    private boolean useHttps;

    private UploadManager uploadManager;
    private Auth auth;

    /**
     * 初始化上传管理器
     */
    private UploadManager getUploadManager() {
        if (uploadManager == null) {
            // 配置区域，根据实际情况选择
            Configuration cfg = new Configuration(Region.autoRegion());
            uploadManager = new UploadManager(cfg);
        }
        return uploadManager;
    }

    /**
     * 获取认证对象
     */
    private Auth getAuth() {
        if (auth == null) {
            auth = Auth.create(accessKey, secretKey);
        }
        return auth;
    }

    /**
     * 生成上传token
     */
    private String getUploadToken() {
        return getAuth().uploadToken(bucket);
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return "avatars/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    /**
     * 生成文件名（指定目录）
     */
    private String generateFileName(String directory, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return directory + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    @Override
    public String uploadFile(MultipartFile file, String fileName) {
        try {
            if (StringUtils.isNullOrEmpty(fileName)) {
                fileName = generateFileName(file.getOriginalFilename());
            }
            
            // 确保文件名以 avatars/ 开头
            if (!fileName.startsWith("avatars/")) {
                fileName = "avatars/" + fileName;
            }

            String uploadToken = getUploadToken();
            Response response = getUploadManager().put(
                    file.getInputStream(),
                    fileName,
                    uploadToken,
                    null,
                    file.getContentType()
            );

            if (response.isOK()) {
                return getFileUrl(fileName);
            } else {
                throw new RuntimeException("上传失败: " + response.bodyString());
            }
        } catch (IOException e) {
            // QiniuException继承自IOException，需要先判断
            if (e instanceof QiniuException) {
                QiniuException qe = (QiniuException) e;
                Response r = qe.response;
                String errorMsg = "上传失败";
                try {
                    errorMsg = r != null ? r.bodyString() : qe.getMessage();
                } catch (QiniuException ex) {
                    errorMsg = ex.getMessage();
                }
                throw new RuntimeException("七牛云上传失败: " + errorMsg, qe);
            } else {
                throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String directory, String fileName) {
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("只支持图片文件");
            }

            // 验证文件大小（限制为10MB）
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("图片大小不能超过10MB");
            }

            // 生成文件名
            if (StringUtils.isNullOrEmpty(fileName)) {
                fileName = generateFileName(directory, file.getOriginalFilename());
            } else {
                // 确保文件名包含目录前缀
                if (!fileName.startsWith(directory + "/")) {
                    fileName = directory + "/" + fileName;
                }
            }

            String uploadToken = getUploadToken();
            Response response = getUploadManager().put(
                    file.getInputStream(),
                    fileName,
                    uploadToken,
                    null,
                    file.getContentType()
            );

            if (response.isOK()) {
                return getFileUrl(fileName);
            } else {
                throw new RuntimeException("上传失败: " + response.bodyString());
            }
        } catch (IOException e) {
            // QiniuException继承自IOException，需要先判断
            if (e instanceof QiniuException) {
                QiniuException qe = (QiniuException) e;
                Response r = qe.response;
                String errorMsg = "上传失败";
                try {
                    errorMsg = r != null ? r.bodyString() : qe.getMessage();
                } catch (QiniuException ex) {
                    errorMsg = ex.getMessage();
                }
                throw new RuntimeException("七牛云上传失败: " + errorMsg, qe);
            } else {
                throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String uploadFile(InputStream inputStream, String fileName, String contentType) {
        try {
            if (StringUtils.isNullOrEmpty(fileName)) {
                fileName = generateFileName(null);
            }
            
            // 确保文件名以 avatars/ 开头
            if (!fileName.startsWith("avatars/")) {
                fileName = "avatars/" + fileName;
            }

            String uploadToken = getUploadToken();
            Response response = getUploadManager().put(
                    inputStream,
                    fileName,
                    uploadToken,
                    null,
                    contentType
            );

            if (response.isOK()) {
                return getFileUrl(fileName);
            } else {
                throw new RuntimeException("上传失败: " + response.bodyString());
            }
        } catch (IOException e) {
            // QiniuException继承自IOException，需要先判断
            if (e instanceof QiniuException) {
                QiniuException qe = (QiniuException) e;
                Response r = qe.response;
                String errorMsg = "上传失败";
                try {
                    errorMsg = r != null ? r.bodyString() : qe.getMessage();
                } catch (QiniuException ex) {
                    errorMsg = ex.getMessage();
                }
                throw new RuntimeException("七牛云上传失败: " + errorMsg, qe);
            } else {
                throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        try {
            com.qiniu.storage.BucketManager bucketManager = new com.qiniu.storage.BucketManager(
                    getAuth(),
                    new Configuration(Region.autoRegion())
            );
            Response response = bucketManager.delete(bucket, fileName);
            return response.isOK();
        } catch (QiniuException e) {
            System.err.println("删除文件失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        if (StringUtils.isNullOrEmpty(fileName)) {
            return "";
        }

        // 如果配置了自定义域名，使用自定义域名
        if (!StringUtils.isNullOrEmpty(domain)) {
            String protocol = useHttps ? "https://" : "http://";
            String url = protocol + domain;
            if (!url.endsWith("/") && !fileName.startsWith("/")) {
                url += "/";
            }
            return url + fileName;
        }

        // 否则使用七牛云默认域名
        String protocol = useHttps ? "https://" : "http://";
        // 从bucket获取默认域名（需要配置）
        // 这里先返回一个占位符，实际使用时需要配置domain
        return protocol + bucket + ".qiniucdn.com/" + fileName;
    }
}

