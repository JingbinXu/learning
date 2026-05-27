import json
from fastapi import APIRouter, Depends, Request
from sqlalchemy import select, func, and_
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.user import User
from app.schemas.common import BaseResponse, DeleteRequest, PageResult
from app.schemas.user import (
    UserRegisterRequest,
    UserLoginRequest,
    UserAddRequest,
    UserUpdateRequest,
    UserQueryRequest,
    LoginUserVO,
    UserVO,
)
from app.services.user_service import (
    user_register,
    user_login,
    get_login_user_vo,
    get_user_vo,
    get_user_vo_list,
    get_encrypt_password,
    get_login_user_from_session,
    build_user_query_filters,
)
from app.dependencies import get_current_user, require_admin
from app.exceptions import BusinessException, ErrorCode

router = APIRouter(prefix="/user", tags=["user"])


@router.post("/register", response_model=BaseResponse[int])
async def register(
    request: UserRegisterRequest,
    db: AsyncSession = Depends(get_db),
):
    user_id = await user_register(
        db,
        request.userAccount,
        request.userPassword,
        request.checkPassword,
    )
    return BaseResponse(data=user_id)


@router.post("/login", response_model=BaseResponse[LoginUserVO])
async def login(
    request: UserLoginRequest,
    http_request: Request,
    db: AsyncSession = Depends(get_db),
):
    user, login_vo = await user_login(db, request.userAccount, request.userPassword)
    # 记录登录态到 session
    http_request.session["user"] = {"id": user.id}
    return BaseResponse(data=login_vo)


@router.post("/logout", response_model=BaseResponse[bool])
async def logout(http_request: Request):
    http_request.session.pop("user", None)
    return BaseResponse(data=True)


@router.get("/get/login", response_model=BaseResponse[LoginUserVO])
async def get_login_user(
    current_user: User = Depends(get_current_user),
):
    return BaseResponse(data=get_login_user_vo(current_user))


@router.post("/add", response_model=BaseResponse[int])
async def add_user(
    request: UserAddRequest,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    user = User(
        userName=request.userName,
        userAccount=request.userAccount,
        userAvatar=request.userAvatar,
        userProfile=request.userProfile,
        userRole=request.userRole or "user",
        userPassword=get_encrypt_password("12345678"),
    )
    db.add(user)
    await db.flush()
    await db.refresh(user)
    return BaseResponse(data=user.id)


@router.get("/get", response_model=BaseResponse[UserVO])
async def get_user(
    id: int,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    if id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR)
    result = await db.execute(
        select(User).where(User.id == id, User.isDelete == 0)
    )
    user = result.scalar_one_or_none()
    if not user:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR)
    return BaseResponse(data=get_user_vo(user))


@router.get("/get/vo", response_model=BaseResponse[UserVO])
async def get_user_vo_by_id(
    id: int,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR)
    result = await db.execute(
        select(User).where(User.id == id, User.isDelete == 0)
    )
    user = result.scalar_one_or_none()
    if not user:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR)
    return BaseResponse(data=get_user_vo(user))


@router.post("/delete", response_model=BaseResponse[bool])
async def delete_user(
    request: DeleteRequest,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    if request.id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR)
    result = await db.execute(
        select(User).where(User.id == request.id, User.isDelete == 0)
    )
    user = result.scalar_one_or_none()
    if not user:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR)
    user.isDelete = 1
    await db.flush()
    return BaseResponse(data=True)


@router.post("/update", response_model=BaseResponse[bool])
async def update_user(
    request: UserUpdateRequest,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    if not request.id:
        raise BusinessException(ErrorCode.PARAMS_ERROR)
    result = await db.execute(
        select(User).where(User.id == request.id, User.isDelete == 0)
    )
    user = result.scalar_one_or_none()
    if not user:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR)
    if request.userName is not None:
        user.userName = request.userName
    if request.userAvatar is not None:
        user.userAvatar = request.userAvatar
    if request.userProfile is not None:
        user.userProfile = request.userProfile
    if request.userRole is not None:
        user.userRole = request.userRole
    await db.flush()
    return BaseResponse(data=True)


@router.post("/list/page/vo", response_model=BaseResponse[PageResult[UserVO]])
async def list_user_vo_by_page(
    request: UserQueryRequest,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    # 构建查询
    query = select(User).where(User.isDelete == 0)
    filters = build_user_query_filters(request)
    if filters:
        query = query.where(and_(*filters))

    # 排序
    if request.sortField:
        col = getattr(User, request.sortField, None)
        if col is not None:
            query = query.order_by(col.desc() if request.sortOrder == "descend" else col.asc())

    # 计数
    count_query = select(func.count()).select_from(User).where(User.isDelete == 0)
    if filters:
        count_query = count_query.where(and_(*filters))
    total = (await db.execute(count_query)).scalar()

    # 分页
    offset = (request.pageNum - 1) * request.pageSize
    query = query.offset(offset).limit(request.pageSize)
    result = await db.execute(query)
    users = result.scalars().all()

    user_vo_list = get_user_vo_list(list(users))
    page_result = PageResult(
        records=user_vo_list,
        total=total,
        size=request.pageSize,
        current=request.pageNum,
    )
    return BaseResponse(data=page_result)
