package com.mobai.mopicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.exception.ThrowUtils;
import com.mobai.mopicturebackend.manager.CosManager;
import com.mobai.mopicturebackend.manager.FileManager;
import com.mobai.mopicturebackend.manager.upload.FilePictureUpload;
import com.mobai.mopicturebackend.manager.upload.PictureUploadTemplate;
import com.mobai.mopicturebackend.manager.upload.UrlPictureUpload;
import com.mobai.mopicturebackend.model.dto.file.UploadPictureResult;
import com.mobai.mopicturebackend.model.dto.picture.*;
import com.mobai.mopicturebackend.model.entity.PictureEntity;
import com.mobai.mopicturebackend.model.entity.SpaceEntity;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.enums.PictureReviewStatusEnum;
import com.mobai.mopicturebackend.model.vo.LoginUserVO;
import com.mobai.mopicturebackend.model.vo.PictureVO;
import com.mobai.mopicturebackend.service.PicturePictureService;
import com.mobai.mopicturebackend.mapper.PicturePictureMapper;
import com.mobai.mopicturebackend.service.PictureSpaceService;
import com.mobai.mopicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图片服务实现类
 * 负责图片的上传、查询、分页、数据校验等核心业务逻辑
 *
 * @author MoBai
 * @description 针对表【picture_picture(图片)】的数据库操作Service实现
 * @createDate 2026-06-21 15:48:03
 */
@Slf4j
@Service
public class PicturePictureServiceImpl extends ServiceImpl<PicturePictureMapper, PictureEntity>
        implements PicturePictureService {

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    /**
     * 文件管理器，负责图片文件的上传和存储
     */
    private final FileManager fileManager;

    /**
     * 用户服务，用于获取用户信息
     */
    private final UserService userService;

    @Autowired
    private CosManager cosManager;

    @Resource
    private PictureSpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 构造器注入依赖
     *
     * @param fileManager 文件管理器
     * @param userService 用户服务
     */
    public PicturePictureServiceImpl(FileManager fileManager, UserService userService) {
        this.fileManager = fileManager;
        this.userService = userService;
    }

    /**
     * 上传图片（支持新增和更新）
     * 将图片文件上传到云存储，并根据是否有图片ID判断是新增还是更新操作
     *
     * @param inputSource         上传的图片文件
     * @param pictureUploadRequest  图片上传请求参数，包含图片ID（用于更新）
     * @param loginUser             当前登录用户信息
     * @return 上传成功后的图片视图对象
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, UserEntity loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ResCodeEnum.NO_AUTH_ERROR);
        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            SpaceEntity space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ResCodeEnum.NOT_FOUND_ERROR, "空间不存在");
            // 校验是否有空间的权限，仅空间管理员才能上传
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ResCodeEnum.NO_AUTH_ERROR, "没有空间权限");
            }
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ResCodeEnum.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ResCodeEnum.OPERATION_ERROR, "空间大小不足");
            }
        }
        // 判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新，判断图片是否存在
        if (pictureId != null) {
            PictureEntity oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ResCodeEnum.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑图片
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ResCodeEnum.NO_AUTH_ERROR);
            }
            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId（这样也兼容了公共图库）
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原图片的空间 id 一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }
        // 上传图片，得到图片信息
        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            // 公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            // 空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据 inputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        PictureEntity picture = new PictureEntity();
        picture.setSpaceId(spaceId); // 指定空间 id
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ResCodeEnum.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            if (finalSpaceId != null) {
                // 更新空间的使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(SpaceEntity::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ResCodeEnum.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        // 可自行实现，如果是更新，可以清理图片资源
        // this.clearPictureFile(oldPicture);
        return PictureVO.objToVo(picture);
    }

    /**
     * 构建图片查询条件
     * 根据查询请求中的各项参数动态构建 MyBatis-Plus 的 QueryWrapper，支持多条件组合查询和排序
     *
     * @param pictureQueryRequest 图片查询请求，包含各类筛选条件
     * @return 构建好的查询条件包装器
     */
    @Override
    public QueryWrapper<PictureEntity> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {

        QueryWrapper<PictureEntity> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从查询请求中提取所有查询参数
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Long reviewerId = pictureQueryRequest.getReviewerId();

        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();

        String reviewMessage = pictureQueryRequest.getReviewMessage();
        // searchText 和 tags 合并为一个 OR 条件组
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            // and (name like "%xxx%" or introduction like "%xxx%")
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }
        // 精确匹配条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        // 模糊匹配条件
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);

        queryWrapper.like(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.like(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.like(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.like(ObjUtil.isNotEmpty(picScale), "picScale", picScale);

        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);

        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");

        // >= startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // < endEditTime
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);


        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            /* and (tag like "%\"Java\"%" and like "%\"Python\"%") */
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }        // 排序处理
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        // 注意：isNotEmpty 只判断字符串是否为 null 或空串，不关心是否全是空格；
        // isNotBlank 会先 trim 再判断是否有实际内容。
        // 在业务参数校验中，一般优先使用 isNotBlank，因为它能过滤掉无效的空白输入。
        return queryWrapper;
    }

    /**
     * 获取单个图片的视图对象（含关联用户信息）
     * 将图片实体转换为视图对象，并填充上传者用户信息
     *
     * @param picture 图片实体对象
     * @param request HTTP请求对象
     * @return 包含用户信息的图片视图对象
     */
    @Override
    public PictureVO getPictureVO(PictureEntity picture, HttpServletRequest request) {
        // 将实体对象转换为视图对象
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询并填充上传者用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            UserEntity user = userService.getById(userId);
            LoginUserVO userVO = userService.getLoginUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 获取图片视图对象的分页结果（含关联用户信息）
     * 将图片实体分页转换为视图对象分页，并通过批量查询关联用户信息来优化性能
     *
     * @param picturePage 图片实体的分页结果
     * @param request     HTTP请求对象
     * @return 图片视图对象的分页结果，包含用户信息
     */
    @Override
    public Page<PictureVO> getPictureVoPage(Page<PictureEntity> picturePage, HttpServletRequest request) {

        // 获取当前页的图片实体列表
        List<PictureEntity> pictureEntities = picturePage.getRecords();
        // 构建空的VO分页结果，保留分页信息
        Page<PictureVO> resultPicturePage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        // 如果当前页无数据，直接返回空分页结果
        if (CollUtil.isEmpty(pictureEntities)) {
            return resultPicturePage;
        }
        // 将实体对象列表转换为视图对象列表
        List<PictureVO> pictureVOList = pictureEntities.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 批量查询关联的用户信息（一次查询代替N次查询，优化性能）
        Set<Long> userIdSet = pictureEntities.stream().map(PictureEntity::getUserId).collect(Collectors.toSet());
        // 将用户列表按ID分组，形成 Map<用户ID, 用户列表>
        Map<Long, List<UserEntity>> userIdUserMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(UserEntity::getId));
        // 为每个图片视图对象填充用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            UserEntity user = null;
            if (userIdUserMap.containsKey(userId)) {
                user = userIdUserMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getLoginUserVO(user));
        });
        resultPicturePage.setRecords(pictureVOList);
        return resultPicturePage;
    }

    /**
     * 图片数据校验方法
     * 用于更新和编辑图片时对关键字段进行校验，确保数据合法
     * 校验规则：图片ID不能为空、URL长度不超过1024字符、简介长度不超过800字符
     *
     * @param picture 待校验的图片实体对象
     */
    @Override
    public void validPicture(PictureEntity picture) {
        ThrowUtils.throwIf(picture == null, ResCodeEnum.PARAMS_ERROR);
        // 从对象中提取需要校验的字段
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 校验图片ID是否为空
        ThrowUtils.throwIf(ObjUtil.isNull(id), ResCodeEnum.PARAMS_ERROR);
        // 校验URL长度（有值时才校验）
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ResCodeEnum.PARAMS_ERROR, "url过长");
        }
        // 校验简介长度（有值时才校验）
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ResCodeEnum.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, UserEntity loginUser) {

        //1.获取参数并校验参数是否存在
        Long id = pictureReviewRequest.getId();
        int reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id ==null ||pictureReviewStatusEnum ==null || PictureReviewStatusEnum.REVIEWING.equals(pictureReviewStatusEnum)){
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR);
        }
        //2.判断是否存在
        PictureEntity picture = getById(id);
        ThrowUtils.throwIf(picture == null, ResCodeEnum.NOT_FOUND_ERROR);
        //2.1校验图片状态是否已经审核过了
        if (PictureReviewStatusEnum.PASS.equals(pictureReviewStatusEnum)){
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "图片已经审核通过");
        }
        //3.更新审核状态
        PictureEntity updatePicture = new PictureEntity();
        BeanUtils.copyProperties(pictureReviewRequest,updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        //4.修改数据库
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result ,new BusinessException(ResCodeEnum.OPERATION_ERROR));
    }

    @Override
    public void fillReviewParams(PictureEntity picture, UserEntity loginUser) {
        //1.判断是否是管理员
        if (userService.isAdmin(loginUser)){
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
            picture.setReviewerId(loginUser.getId());
        }else {
            //2.非管理员自动填充为待审核状态
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, UserEntity loginUser) {
// 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ResCodeEnum.PARAMS_ERROR, "最多 30 条");
        // 名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ResCodeEnum.OPERATION_ERROR, "获取页面失败");
        }
        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ResCodeEnum.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        // 遍历元素，依次处理上传图片
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            // codefather.cn?yupi=dog，应该只保留 codefather.cn
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(PictureEntity oldPicture) {

        String pictureUrl = oldPicture.getUrl();
        // 判断该图片是否被多个记录使用
        long count = this.lambdaQuery().eq(PictureEntity::getUrl,pictureUrl).count();

        if (count > 1 ){
            return ;
        }

        String picturePath = null;
        try {
            picturePath = new URL(pictureUrl).getPath();
            cosManager.deleteObject(picturePath);

            // 清理缩略图
            String thumbnailUrl = oldPicture.getThumbnailUrl();

            if (StrUtil.isNotBlank(thumbnailUrl)){
                String thumbnaiPath = new URL(thumbnailUrl).getPath();
                cosManager.deleteObject(thumbnaiPath);
            }

        } catch (MalformedURLException e) {
            log.info("处理图片删除时遇到格式错误的URL url is :{}",pictureUrl,e);
            throw new BusinessException(ResCodeEnum.SYSTEM_ERROR,"格式错误的URL");

        }

    }


    @Override
    public void deletePicture(long pictureId, UserEntity loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ResCodeEnum.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ResCodeEnum.NO_AUTH_ERROR);
        // 判断是否存在
        PictureEntity oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ResCodeEnum.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ResCodeEnum.OPERATION_ERROR);
            // 更新空间的使用额度，释放额度
            boolean update = spaceService.lambdaUpdate()
                    .eq(SpaceEntity::getId, oldPicture.getSpaceId())
                    .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                    .setSql("totalCount = totalCount - 1")
                    .update();
            ThrowUtils.throwIf(!update, ResCodeEnum.OPERATION_ERROR, "额度更新失败");
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, UserEntity loginUser) {
        // 在此处将实体类和 DTO 进行转换
        PictureEntity picture = new PictureEntity();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        PictureEntity oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ResCodeEnum.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ResCodeEnum.OPERATION_ERROR);
    }

    @Override
    public void checkPictureAuth(UserEntity loginUser, PictureEntity picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUserId) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ResCodeEnum.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUserId)) {
                throw new BusinessException(ResCodeEnum.NO_AUTH_ERROR);
            }
        }
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<PictureEntity> picturePage, HttpServletRequest request) {
        List<PictureEntity> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = pictureList.stream().map(PictureEntity::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, List<UserEntity>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(UserEntity::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            UserEntity user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

}




