import json
from typing import Optional
from fastapi import Request, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.user import User
from app.exceptions import BusinessException, ErrorCode


async def get_current_user(
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> User:
    """从 session 获取当前登录用户，重新查库获取最新数据"""
    user_data = request.session.get("user")
    if not user_data:
        raise BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录")

    # 支持 dict 格式的 session 数据
    if isinstance(user_data, str):
        user_data = json.loads(user_data)

    user_id = user_data.get("id") if isinstance(user_data, dict) else None
    if not user_id:
        raise BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录")

    result = await db.execute(
        select(User).where(User.id == user_id, User.isDelete == 0)
    )
    user = result.scalar_one_or_none()
    if not user:
        raise BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录")
    return user


async def get_optional_user(
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> Optional[User]:
    """可选的登录用户，未登录返回 None"""
    try:
        return await get_current_user(request, db)
    except BusinessException:
        return None


async def require_admin(
    current_user: User = Depends(get_current_user),
) -> User:
    """要求管理员权限"""
    if current_user.user_role != "admin":
        raise BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限")
    return current_user
