package com.mobai.mopicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.mobai.mopicturebackend.config.CosClientConfig;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.exception.ThrowUtils;
import com.mobai.mopicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.io.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片到腾讯云对象存储(COS)
     *
     * @param multipartFile    上传的文件对象
     * @param uploadPathPrefix 上传路径前缀,用于区分不同类型的图片(如"avatar"、"post"等)
     * @return UploadPictureResult 包含图片URL、尺寸、格式等信息的上传结果
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片格式和大小是否符合要求
        validPicture(multipartFile);

        // 生成唯一的图片标识符,避免文件名冲突
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();

        // 使用日期+UUID重新拼接文件名,而不是直接使用原始文件名,增强安全性防止恶意文件注入
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));

        // 构造完整的上传路径: /前缀/日期_UUID.后缀
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;

        try {
            // 创建临时文件,用于将MultipartFile转换为File对象
            file = File.createTempFile(uploadPath, null);
            // 将上传的文件内容转移到临时文件中
            multipartFile.transferTo(file);

            // 调用COS Manager上传图片到腾讯云对象存储,并获取图片信息
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 从上传结果中提取图片的基本信息(宽度、高度、格式等)
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

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
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ResCodeEnum.SYSTEM_ERROR, "上传失败");
        } finally {
            // 无论成功或失败,都要删除临时文件,避免磁盘空间浪费
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验上传文件的合法性
     * 包括:非空校验、文件大小校验、文件后缀名校验
     *
     * @param multipartFile 待校验的文件对象
     * @throws BusinessException 当文件不符合要求时抛出业务异常
     */
    public void validPicture(MultipartFile multipartFile) {
        // 校验文件是否为空
        ThrowUtils.throwIf(multipartFile == null, ResCodeEnum.PARAM_ERROR, "文件不能为空");

        // 校验文件大小,限制最大为20MB
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L; // 1MB的字节数
        ThrowUtils.throwIf(fileSize > 20 * ONE_M, ResCodeEnum.PARAM_ERROR, "文件大小不能超过20M");

        // 校验文件后缀名,只允许指定的图片格式
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的图片格式列表:jpeg、jpg、png、webp
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        // 如果文件后缀不在允许的列表中,则抛出异常
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ResCodeEnum.PARAM_ERROR, "文件类型错误");
    }

    /**
     * 删除上传过程中产生的临时文件
     * 在finally块中调用,确保资源被及时释放,避免磁盘空间泄漏
     *
     * @param file 需要删除的临时文件对象
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 执行文件删除操作
        boolean deleteResult = file.delete();
        // 如果删除失败,记录错误日志便于排查问题
        if (!deleteResult) {
            log.error("file delete error,filepath = {} ", file.getAbsoluteFile());
        }
    }


}
