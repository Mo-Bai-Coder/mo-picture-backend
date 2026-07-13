package com.mobai.mopicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 通过 URL 上传图片的实现类
 * <p>
 * 支持从远程 URL 地址下载图片，并上传到对象存储。
 * 继承自 {@link PictureUploadTemplate}，实现模板方法中的三个抽象步骤：
 * 校验、获取文件名、处理文件。
 * </p>
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate {

    /**
     * 校验远程图片 URL 的合法性
     * <p>
     * 依次校验：非空、URL 格式、协议类型（仅允许 HTTP / HTTPS）
     * </p>
     *
     * @param inputSource 输入源，此处为远程图片的 URL 字符串
     */
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;

        // 1. 校验 URL 非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ResCodeEnum.PARAMS_ERROR, "文件地址为空");

        // 2. 校验 URL 格式是否合法
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "文件地址格式不正确");
        }

        // 3. 校验 URL 协议，仅支持 HTTP 或 HTTPS
        ThrowUtils.throwIf(
                !fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),
                ResCodeEnum.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址"
        );
    }

    /**
     * 从 URL 中提取原始文件名（不含后缀）
     *
     * @param inputSource 输入源，此处为远程图片的 URL 字符串
     * @return 文件名（不含后缀部分）
     */
    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 使用 Hutool 的 FileUtil.mainName 提取 URL 路径中的主文件名
        return FileUtil.mainName(fileUrl);
    }

    /**
     * 将远程 URL 对应的文件下载到本地临时文件
     *
     * @param inputSource 输入源，此处为远程图片的 URL 字符串
     * @param file        本地临时文件，由模板方法创建
     * @throws Exception 下载过程中发生异常时抛出
     */
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        // 使用 Hutool 的 HttpUtil 将远程文件下载到临时文件
        HttpUtil.downloadFile(fileUrl, file);
    }

}
