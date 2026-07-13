package com.mobai.mopicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 通过本地文件上传图片的实现类
 * <p>
 * 接收前端上传的 {@link MultipartFile}，对其进行校验后写入本地临时文件，
 * 再由模板方法上传到对象存储。
 * 继承自 {@link PictureUploadTemplate}，实现模板方法中的三个抽象步骤：
 * 校验、获取文件名、处理文件。
 * </p>
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {

    /** 允许上传的图片格式列表 */
    private static final List<String> ALLOW_FORMAT_LIST = Arrays.asList(
            "jpeg", "jpg", "png", "webp"
    );

    /** 文件最大大小：20 MB */
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L;

    /**
     * 校验本地上传文件的合法性
     * <p>
     * 依次校验：文件非空、文件大小（不超过 20MB）、文件后缀（仅允许 jpeg/jpg/png/webp）
     * </p>
     *
     * @param inputSource 输入源，此处为前端上传的 {@link MultipartFile}
     */
    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;

        // 1. 校验文件不能为空
        ThrowUtils.throwIf(multipartFile == null, ResCodeEnum.PARAMS_ERROR, "文件不能为空");

        // 2. 校验文件大小，限制最大为 20MB
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > MAX_FILE_SIZE, ResCodeEnum.PARAMS_ERROR, "文件大小不能超过20M");

        // 3. 校验文件后缀，只允许指定的图片格式
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ResCodeEnum.PARAMS_ERROR, "文件类型错误");
    }

    /**
     * 获取上传文件的原始文件名
     *
     * @param inputSource 输入源，此处为前端上传的 {@link MultipartFile}
     * @return 原始文件名（包含后缀）
     */
    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    /**
     * 将上传的文件内容写入本地临时文件
     *
     * @param inputSource 输入源，此处为前端上传的 {@link MultipartFile}
     * @param file        本地临时文件，由模板方法创建
     * @throws Exception 写入过程中发生异常时抛出
     */
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        // 将 MultipartFile 的内容传输到临时文件
        multipartFile.transferTo(file);
    }
}
