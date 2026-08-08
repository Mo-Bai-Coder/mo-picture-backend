package com.mobai.mopicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.exception.ThrowUtils;
import com.mobai.mopicturebackend.model.dto.space.SpaceAddRequest;
import com.mobai.mopicturebackend.model.dto.space.SpaceQueryRequest;
import com.mobai.mopicturebackend.model.entity.SpaceEntity;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.enums.SpaceLevelEnum;
import com.mobai.mopicturebackend.model.vo.LoginUserVO;
import com.mobai.mopicturebackend.model.vo.SpaceVO;
import com.mobai.mopicturebackend.model.vo.UserVO;
import com.mobai.mopicturebackend.service.PictureSpaceService;
import com.mobai.mopicturebackend.mapper.PictureSpaceMapper;
import com.mobai.mopicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author MoBai
* @description 针对表【picture_space(空间)】的数据库操作Service实现
* @createDate 2026-07-25 22:45:39
*/
@Service
public class PictureSpaceServiceImpl extends ServiceImpl<PictureSpaceMapper, SpaceEntity>
    implements PictureSpaceService{

    private final UserService userService;
    private final TransactionTemplate transactionTemplate;

    public PictureSpaceServiceImpl(UserService userService, TransactionTemplate transactionTemplate) {
        this.userService = userService;
        this.transactionTemplate = transactionTemplate;
    }

    //先开发一个校验参数的方法
    @Override
    public void validSpace(SpaceEntity space, boolean add) {
        ThrowUtils.throwIf(space == null, ResCodeEnum.PARAMS_ERROR);
        // 1.校验参数， 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);

        // 2.判断是否新增
        if (add) {
//            新增时
//            空间名称，空间等级不能为空
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "空间级别不能为空");
            }
        }

//        不是新增编辑时
//        空间级别不能为空
//        空间名称字数不能超过30

        // 修改数据时，如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "空间名称过长");
        }
    }


    @Override
    public void fillSpaceBySpaceLevel(SpaceEntity space) {

//        拿到空间级别SpaceLenvelEnum
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());

//        填充入参SpaceEntity
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }





    }

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, UserEntity loginUser) {

        // 1. 填充参数默认值

        // 转换实体类和 DTO
        SpaceEntity space = new SpaceEntity();
        BeanUtils.copyProperties(spaceAddRequest, space);

//        填充参数默认值
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }

        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }

        //填充容量和大小
        this.fillSpaceBySpaceLevel(space);

        //校验参数
        this.validSpace(space,true);

        //校验参数，非管理员只能创建普通级别的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);

        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)){
            throw new BusinessException(ResCodeEnum.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }

        //控制同一用户只能创建一个私有空间
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long  newSpaceId = transactionTemplate.execute(status -> {
                //判断是否已有空间
                boolean exists = this.lambdaQuery().eq(SpaceEntity::getUserId,userId).exists();

                // 如果已有空间，就不能再创建
                ThrowUtils.throwIf(exists, ResCodeEnum.OPERATION_ERROR, "每个用户仅能有一个私有空间");

                //创建
                boolean create = this.save(space);
                ThrowUtils.throwIf(!create, ResCodeEnum.OPERATION_ERROR, "保存空间到数据库失败");

                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);

        }

    }

    @Override
    public QueryWrapper<SpaceEntity> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<SpaceEntity> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<SpaceEntity> spacePage, HttpServletRequest request) {
        List<SpaceEntity> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = spaceList.stream().map(SpaceEntity::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, List<UserEntity>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(UserEntity::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            UserEntity user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public SpaceVO getSpaceVO(SpaceEntity space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            UserEntity user = userService.getById(userId);
            LoginUserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }


}




