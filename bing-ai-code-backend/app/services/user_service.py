import hashlib
from typing import Optional
from sqlalchemy import select, func, and_
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User
from app.schemas.user import LoginUserVO, UserVO, UserQueryRequest
from app.exceptions import BusinessException, ErrorCode
from app.schemas.common import PageResult


def get_encrypt_password(user_password: str) -> str:
    """MD5("bing" + password) 兼容 Java 版密码哈希"""
    salt = "bing"
    return hashlib.md5((salt + user_password).encode()).hexdigest()


def get_login_user_vo(user: User) -> LoginUserVO:
    return LoginUserVO(
        id=user.id,
        userAccount=user.userAccount,
        userName=user.userName,
        userAvatar=user.userAvatar,
        userProfile=user.userProfile,
        userRole=user.userRole,
        createTime=user.createTime,
        updateTime=user.updateTime,
    )


def get_user_vo(user: User) -> UserVO:
    return UserVO(
        id=user.id,
        userAccount=user.userAccount,
        userName=user.userName,
        userAvatar=user.userAvatar,
        userProfile=user.userProfile,
        userRole=user.userRole,
        createTime=user.createTime,
    )


def get_user_vo_list(users: list[User]) -> list[UserVO]:
    return [get_user_vo(u) for u in users]


async def user_register(
    db: AsyncSession,
    user_account: str,
    user_password: str,
    check_password: str,
) -> int:
    if not user_account or not user_password or not check_password:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空")
    if len(user_account) < 4:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短")
    if len(user_password) < 8 or len(check_password) < 8:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短")
    if user_password != check_password:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致")

    # 检查账号是否存在
    result = await db.execute(
        select(func.count()).select_from(User).where(
            User.userAccount == user_account, User.isDelete == 0
        )
    )
    if result.scalar() > 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "用户账号已存在")

    # 加密密码
    encrypt_password = get_encrypt_password(user_password)

    # 创建用户
    user = User(
        userAccount=user_account,
        userPassword=encrypt_password,
        userRole="user",
    )
    db.add(user)
    await db.flush()
    await db.refresh(user)
    return user.id


async def user_login(
    db: AsyncSession,
    user_account: str,
    user_password: str,
) -> tuple[User, LoginUserVO]:
    if not user_account or not user_password:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空")
    if len(user_account) < 4:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短")
    if len(user_password) < 8:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短")

    encrypt_password = get_encrypt_password(user_password)
    result = await db.execute(
        select(User).where(
            User.userAccount == user_account,
            User.userPassword == encrypt_password,
            User.isDelete == 0,
        )
    )
    user = result.scalar_one_or_none()
    if not user:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误")

    return user, get_login_user_vo(user)


async def get_login_user_from_session(
    db: AsyncSession,
    session_user_data: dict,
) -> User:
    """从 session 数据获取完整用户"""
    if not session_user_data:
        raise BusinessException(ErrorCode.NOT_LOGIN_ERROR)

    user_id = session_user_data.get("id")
    if not user_id:
        raise BusinessException(ErrorCode.NOT_LOGIN_ERROR)

    result = await db.execute(
        select(User).where(User.id == user_id, User.isDelete == 0)
    )
    user = result.scalar_one_or_none()
    if not user:
        raise BusinessException(ErrorCode.NOT_LOGIN_ERROR)
    return user


def build_user_query_filters(query_request: UserQueryRequest):
    """构建用户查询过滤条件"""
    filters = []
    if query_request.id is not None:
        filters.append(User.id == query_request.id)
    if query_request.userRole is not None:
        filters.append(User.userRole == query_request.userRole)
    if query_request.userAccount is not None:
        filters.append(User.userAccount.like(f"%{query_request.userAccount}%"))
    if query_request.userName is not None:
        filters.append(User.userName.like(f"%{query_request.userName}%"))
    if query_request.userProfile is not None:
        filters.append(User.userProfile.like(f"%{query_request.userProfile}%"))
    return filters
