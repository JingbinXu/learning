from typing import Optional
from datetime import datetime
from fastapi import APIRouter, Depends, Query, Path
from sqlalchemy import select, func, and_
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.chat_history import ChatHistory
from app.models.user import User
from app.schemas.common import BaseResponse, PageResult
from app.schemas.chat_history import ChatHistoryQueryRequest, ChatHistoryVO
from app.services.chat_history_service import (
    list_app_chat_history_by_page,
    build_chat_history_query_filters,
)
from app.dependencies import get_current_user, require_admin
from app.exceptions import BusinessException, ErrorCode

router = APIRouter(prefix="/chatHistory", tags=["chatHistory"])


@router.get("/app/{appId}")
async def list_app_chat_history(
    appId: int = Path(...),
    pageSize: int = Query(default=10),
    lastCreateTime: Optional[datetime] = Query(default=None),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await list_app_chat_history_by_page(
        db, appId, pageSize, lastCreateTime, current_user
    )
    return BaseResponse(data=result)


@router.post("/admin/list/page/vo")
async def admin_list_chat_history(
    request: ChatHistoryQueryRequest,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    query = select(ChatHistory).where(ChatHistory.isDelete == 0)
    filters = build_chat_history_query_filters(request)
    if filters:
        query = query.where(and_(*filters))

    # 排序
    if request.sortField:
        col = getattr(ChatHistory, request.sortField, None)
        if col is not None:
            query = query.order_by(col.desc() if request.sortOrder == "descend" else col.asc())
    else:
        query = query.order_by(ChatHistory.createTime.desc())

    # 计数
    count_query = select(func.count()).select_from(ChatHistory).where(ChatHistory.isDelete == 0)
    if filters:
        count_query = count_query.where(and_(*filters))
    total = (await db.execute(count_query)).scalar()

    # 分页
    offset = (request.pageNum - 1) * request.pageSize
    query = query.offset(offset).limit(request.pageSize)
    result = await db.execute(query)
    records = list(result.scalars().all())

    page_result = PageResult(
        records=records,
        total=total,
        size=request.pageSize,
        current=request.pageNum,
    )
    return BaseResponse(data=page_result)
