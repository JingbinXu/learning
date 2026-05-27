from typing import Optional
from datetime import datetime
from sqlalchemy import select, func, and_
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.chat_history import ChatHistory
from app.models.app import App
from app.schemas.chat_history import ChatHistoryQueryRequest
from app.schemas.enums import ChatHistoryMessageTypeEnum
from app.exceptions import BusinessException, ErrorCode


async def add_chat_message(
    db: AsyncSession,
    app_id: int,
    message: str,
    message_type: str,
    user_id: int,
) -> bool:
    if not app_id or app_id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "应用ID不能为空")
    if not message or not message.strip():
        raise BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空")
    if not message_type:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "消息类型不能为空")
    if not user_id or user_id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空")
    # 验证消息类型
    msg_type_enum = ChatHistoryMessageTypeEnum.get_by_value(message_type)
    if not msg_type_enum:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "不支持的消息类型")

    chat_history = ChatHistory(
        appId=app_id,
        message=message,
        messageType=message_type,
        userId=user_id,
    )
    db.add(chat_history)
    await db.flush()
    return True


async def delete_by_app_id(db: AsyncSession, app_id: int) -> bool:
    if not app_id or app_id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "应用ID不能为空")
    await db.execute(
        ChatHistory.__table__.delete().where(ChatHistory.appId == app_id)
    )
    return True


async def list_app_chat_history_by_page(
    db: AsyncSession,
    app_id: int,
    page_size: int,
    last_create_time: Optional[datetime],
    login_user,
):
    if not app_id or app_id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "应用ID不能为空")
    if page_size <= 0 or page_size > 50:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间")
    if not login_user:
        raise BusinessException(ErrorCode.NOT_LOGIN_ERROR)

    # 验证权限
    result = await db.execute(
        select(App).where(App.id == app_id, App.isDelete == 0)
    )
    app = result.scalar_one_or_none()
    if not app:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在")

    is_admin = login_user.userRole == "admin"
    is_creator = app.userId == login_user.id
    if not is_admin and not is_creator:
        raise BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史")

    # 构建查询
    query = select(ChatHistory).where(
        ChatHistory.appId == app_id,
        ChatHistory.isDelete == 0,
    )
    if last_create_time:
        query = query.where(ChatHistory.createTime < last_create_time)

    query = query.order_by(ChatHistory.createTime.desc()).limit(page_size)
    result = await db.execute(query)
    records = list(result.scalars().all())

    # 计算总数
    count_result = await db.execute(
        select(func.count())
        .select_from(ChatHistory)
        .where(ChatHistory.appId == app_id, ChatHistory.isDelete == 0)
    )
    total = count_result.scalar()

    # 返回与 Java 版相同的分页结构
    return {
        "records": records,
        "total": total,
        "size": page_size,
        "current": 1,
    }


def build_chat_history_query_filters(query_request: ChatHistoryQueryRequest):
    filters = []
    if query_request.id is not None:
        filters.append(ChatHistory.id == query_request.id)
    if query_request.message:
        filters.append(ChatHistory.message.like(f"%{query_request.message}%"))
    if query_request.messageType:
        filters.append(ChatHistory.messageType == query_request.messageType)
    if query_request.appId is not None:
        filters.append(ChatHistory.appId == query_request.appId)
    if query_request.userId is not None:
        filters.append(ChatHistory.userId == query_request.userId)
    if query_request.lastCreateTime is not None:
        filters.append(ChatHistory.createTime < query_request.lastCreateTime)
    return filters
