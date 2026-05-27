import os
import random
import string
import shutil
from datetime import datetime
from typing import Optional
from sqlalchemy import select, func, and_
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.app import App
from app.models.user import User
from app.models.chat_history import ChatHistory
from app.schemas.app import AppVO, AppQueryRequest, AppAddRequest
from app.schemas.user import UserVO
from app.schemas.enums import CodeGenTypeEnum
from app.services.user_service import get_user_vo
from app.exceptions import BusinessException, ErrorCode
from app.config import get_settings
from app.ai.code_gen_service import route_code_gen_type as ai_route_code_gen_type

settings = get_settings()

GOOD_APP_PRIORITY = 99


def get_app_vo(app: App, user: Optional[User] = None) -> AppVO:
    user_vo = get_user_vo(user) if user else None
    return AppVO(
        id=app.id,
        appName=app.appName,
        cover=app.cover,
        initPrompt=app.initPrompt,
        codeGenType=app.codeGenType,
        deployKey=app.deployKey,
        deployedTime=app.deployedTime,
        priority=app.priority or 0,
        userId=app.userId,
        createTime=app.createTime,
        updateTime=app.updateTime,
        user=user_vo,
    )


def get_app_vo_list(apps: list[App], user_map: dict[int, User]) -> list[AppVO]:
    result = []
    for app in apps:
        user = user_map.get(app.userId)
        result.append(get_app_vo(app, user))
    return result


def build_app_query_filters(query_request: AppQueryRequest):
    filters = []
    if query_request.id is not None:
        filters.append(App.id == query_request.id)
    if query_request.appName:
        filters.append(App.appName.like(f"%{query_request.appName}%"))
    if query_request.cover:
        filters.append(App.cover.like(f"%{query_request.cover}%"))
    if query_request.initPrompt:
        filters.append(App.initPrompt.like(f"%{query_request.initPrompt}%"))
    if query_request.codeGenType:
        filters.append(App.codeGenType == query_request.codeGenType)
    if query_request.deployKey:
        filters.append(App.deployKey == query_request.deployKey)
    if query_request.priority is not None:
        filters.append(App.priority == query_request.priority)
    if query_request.userId is not None:
        filters.append(App.userId == query_request.userId)
    return filters


async def create_app(
    db: AsyncSession,
    app_add_request: AppAddRequest,
    login_user: User,
) -> int:
    """创建应用"""
    init_prompt = app_add_request.initPrompt
    if not init_prompt or not init_prompt.strip():
        raise BusinessException(ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空")

    # 构造应用对象
    app = App(
        initPrompt=init_prompt,
        userId=login_user.id,
        appName=init_prompt[:12],
    )

    # AI 智能选择代码生成类型
    try:
        selected_type = await ai_route_code_gen_type(init_prompt)
        app.codeGenType = selected_type.value
    except Exception as e:
        # 路由失败时默认 HTML
        app.codeGenType = "html"

    db.add(app)
    await db.flush()
    await db.refresh(app)
    return app.id


async def deploy_app(
    db: AsyncSession,
    app_id: int,
    login_user: User,
) -> str:
    """部署应用"""
    if not app_id or app_id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 错误")
    if not login_user:
        raise BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录")

    # 查询应用
    result = await db.execute(
        select(App).where(App.id == app_id, App.isDelete == 0)
    )
    app = result.scalar_one_or_none()
    if not app:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在")

    # 权限校验
    if app.userId != login_user.id:
        raise BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用")

    # 生成 deployKey
    deploy_key = app.deployKey
    if not deploy_key:
        deploy_key = "".join(random.choices(string.ascii_letters + string.digits, k=6))

    # 构建路径
    code_gen_type = app.codeGenType
    source_dir_name = f"{code_gen_type}_{app_id}"
    source_dir_path = os.path.join(settings.code_output_root_dir, source_dir_name)

    if not os.path.isdir(source_dir_path):
        raise BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码路径不存在，请先生成应用")

    # Vue 项目特殊处理 (TODO: Phase 7 实现构建器)
    actual_source = source_dir_path
    if code_gen_type == CodeGenTypeEnum.VUE_PROJECT.value:
        dist_dir = os.path.join(source_dir_path, "dist")
        if os.path.isdir(dist_dir):
            actual_source = dist_dir

    # 复制文件到部署目录
    deploy_dir_path = os.path.join(settings.code_deploy_root_dir, deploy_key)
    try:
        if os.path.exists(deploy_dir_path):
            shutil.rmtree(deploy_dir_path)
        shutil.copytree(actual_source, deploy_dir_path)
    except Exception as e:
        raise BusinessException(ErrorCode.SYSTEM_ERROR, f"应用部署失败：{e}")

    # 更新数据库
    app.deployKey = deploy_key
    app.deployedTime = datetime.now()
    await db.flush()

    # 构建访问 URL
    app_deploy_url = f"{settings.code_deploy_host}/{deploy_key}/"

    # TODO: 异步截图 (Phase 9 实现)
    # import asyncio
    # asyncio.create_task(generate_app_screenshot(db, app_id, app_deploy_url))

    return app_deploy_url


async def chat_to_gen_code(
    db: AsyncSession,
    app_id: int,
    message: str,
    login_user: User,
):
    """代码生成对话 (SSE 流式)"""
    if not app_id or app_id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "应用 id 错误")
    if not message or not message.strip():
        raise BusinessException(ErrorCode.PARAMS_ERROR, "提示词不能为空")

    # 查询应用
    result = await db.execute(
        select(App).where(App.id == app_id, App.isDelete == 0)
    )
    app = result.scalar_one_or_none()
    if not app:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在")

    # 权限校验
    if app.userId != login_user.id:
        raise BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限")

    # 校验生成类型
    code_gen_type = CodeGenTypeEnum.get_by_value(app.codeGenType)
    if not code_gen_type:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "生成类型错误")

    # 保存用户消息
    chat_msg = ChatHistory(
        message=message,
        messageType="user",
        appId=app_id,
        userId=login_user.id,
    )
    db.add(chat_msg)
    await db.flush()

    # 调用 AI 生成代码（流式）
    from app.core.code_gen_facade import generate_and_save_code_stream

    return generate_and_save_code_stream(message, code_gen_type, app_id)
