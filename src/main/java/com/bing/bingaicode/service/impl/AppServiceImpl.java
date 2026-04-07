package com.bing.bingaicode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bing.bingaicode.core.AiCodeGeneratorFacade;
import com.bing.bingaicode.exception.BusinessException;
import com.bing.bingaicode.exception.ErrorCode;
import com.bing.bingaicode.exception.ThrowUtils;
import com.bing.bingaicode.model.dto.app.AppQueryRequest;
import com.bing.bingaicode.model.entity.User;
import com.bing.bingaicode.model.enums.CodeGenTypeEnum;
import com.bing.bingaicode.model.vo.AppVO;
import com.bing.bingaicode.model.vo.UserVO;
import com.bing.bingaicode.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.bing.bingaicode.model.entity.App;
import com.bing.bingaicode.mapper.AppMapper;
import com.bing.bingaicode.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author bing
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {


    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        // 构建查询条件
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 只对非空字段添加查询条件，避免 SQL 错误
        if (id != null) {
            queryWrapper.eq("id", id);
        }
        if (appName != null && !appName.isEmpty()) {
            queryWrapper.like("appName", appName);
        }
        if (cover != null && !cover.isEmpty()) {
            queryWrapper.like("cover", cover);
        }
        if (initPrompt != null && !initPrompt.isEmpty()) {
            queryWrapper.like("initPrompt", initPrompt);
        }
        if (codeGenType != null && !codeGenType.isEmpty()) {
            queryWrapper.eq("codeGenType", codeGenType);
        }
        if (deployKey != null && !deployKey.isEmpty()) {
            queryWrapper.eq("deployKey", deployKey);
        }
        if (priority != null) {
            queryWrapper.eq("priority", priority);
        }
        if (userId != null) {
            queryWrapper.eq("userId", userId);
        }
        if (sortField != null && !sortField.isEmpty()) {
            boolean isAsc = "ascend".equals(sortOrder);
            queryWrapper.orderBy(sortField, isAsc);
        }
        return queryWrapper;
    }


    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            // 如果用户不存在，不设置用户信息，避免空指针
            if (userVO != null) {
                appVO.setUser(userVO);
            }
            return appVO;
        }).collect(Collectors.toList());
    }


    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        //查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        //校验信息，仅本人能和自己应用对话
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }

        //获取应用生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型错误");
        }
//        调用AI生成代码
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);

    }

}
