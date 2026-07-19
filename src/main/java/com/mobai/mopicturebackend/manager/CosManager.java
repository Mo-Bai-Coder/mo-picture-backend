package com.mobai.mopicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.mobai.mopicturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传文件
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putObject (String key, File file){
        PutObjectRequest putObjectResult =  new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectResult);
    }

    /**
     * 获取文件
     * @param key
     * @return
     */
    public COSObject getObject(String key){
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传图片
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file){
        PutObjectRequest putObjectRequest =  new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        //对图片进行处理(获取基本信息)
        PicOperations picOperations = new PicOperations();
        //1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules =new ArrayList<>();

        //2.图片压缩成 转成webp格式
        String webpKey = FileUtil.mainName(key)+".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);

        // 由于原图比较小，缩略图可能会比原图大，索引要加大于20kb 的限制
        if (file.length() > 2 * 1024){
            //3.缩略图处理
            String thumbnaiKey = FileUtil.mainName(key)+"_thumbnail."+FileUtil.getSuffix(key);
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%x>",128,128));
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setFileId(thumbnaiKey);
            rules.add(thumbnailRule);
        }


        picOperations.setRules(rules);

        //构造处理参数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }



}
