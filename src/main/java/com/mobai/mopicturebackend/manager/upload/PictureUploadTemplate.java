package com.mobai.mopicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.mobai.mopicturebackend.config.CosClientConfig;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.manager.CosManager;
import com.mobai.mopicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {

        // 1. 校验图片
        validPicture(inputSource);
        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginFilename(inputSource);
        // 自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 3. 创建临时文件，获取文件到服务器
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源
            processFile(inputSource, file);
            // 4. 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 5. 获取图片信息对象，封装返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> list = processResults.getObjectList();

            if (CollUtil.isNotEmpty(list)){

                CIObject compressedCiObject = list.get(0);

                return buildResult(originalFilename,compressedCiObject );
            }

            return buildResult(originalFilename, file, uploadPath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ResCodeEnum.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 临时文件清理
            this.deleteTempFile(file);
        }

    }

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile (Object inputSource ,File file) throws Exception;


    /**
     * 封装返回结果
     */
    private UploadPictureResult buildResult (String originalFilename,File file, String uploadPath,ImageInfo imageInfo){
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        // 计算宽高比,保留两位小数,用于后续图片展示和布局
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 设置图片名称(去除后缀)
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        // 设置图片宽度和高度
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        // 设置宽高比
        uploadPictureResult.setPicScale(picScale);
        // 设置图片格式(jpeg/png/webp等)
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        // 设置文件大小(字节)
        uploadPictureResult.setPicSize(FileUtil.size(file));
        // 拼接完整的图片访问URL: host + 路径
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     */
    private UploadPictureResult buildResult (String originalFilename,CIObject compressedCiObject ){
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        // 计算宽高比,保留两位小数,用于后续图片展示和布局
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 设置图片名称(去除后缀)
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        // 设置图片宽度和高度
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        // 设置宽高比
        uploadPictureResult.setPicScale(picScale);
        // 设置图片格式(jpeg/png/webp等)
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        // 设置文件大小(字节)
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        // 拼接完整的图片访问URL: host + 路径
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        return uploadPictureResult;
    }

    private void deleteTempFile(File file) {
        if ( file == null){
            return;
        }
        //删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("临时文件删除失败 {}", file.getPath());
        }
    }


}