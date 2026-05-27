import json
import os
import zipfile
import io
from datetime import datetime
from fastapi import APIRouter, Depends, Request, Query
from fastapi.responses import StreamingResponse
from sqlalchemy import select, func, and_
from sqlalchemy.ext.asyncio import AsyncSession
from sse_starlette.sse import EventSourceResponse

from app.database import get_db
from app.models.app import App
from app.models.user import User
from app.models.chat_history import ChatHistory
from app.schemas.common import BaseResponse, DeleteRequest, PageResult
from app.schemas.app import (
    AppAddRequest,
    AppUpdateRequest,
    AppAdminUpdateRequest,
    AppDeployRequest,
    AppQueryRequest,
    AppVO,
)
from app.services.app_service import (
    create_app,
    deploy_app,
    chat_to_gen_code,
    get_app_vo,
    get_app_vo_list,
    build_app_query_filters,
    GOOD_APP_PRIORITY,
)
from app.services.user_service import get_user_vo
from app.dependencies import get_current_user, require_admin
from app.exceptions import BusinessException, ErrorCode
from app.config import get_settings
from app.middleware.rate_limit import rate_limit_by_user

settings = get_settings()
router = APIRouter(prefix="/app", tags=["app"])


@router.get("/chat/gen/code")
async def chat_gen_code(
    request: Request,
    appId: int = Query(...),
    message: str = Query(...),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """SSE 流式代码生成（限流：5请求/60秒/用户）"""
    # 限流检查
    await rate_limit_by_user(request, rate=5, rate_interval=60)
    if not appId or appId <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "应用 id 错误")
    if not message or not message.strip():
        raise BusinessException(ErrorCode.PARAMS_ERROR, "提示词不能为空")

    stream_gen = await chat_to_gen_code(db, appId, message, current_user)

    async def event_generator():
        async for chunk in stream_gen:
            yield {"data": json.dumps({"d": chunk}, ensure_ascii=False)}
        yield {"event": "done", "data": ""}

    return EventSourceResponse(event_generator())


@router.get("/download/{appId}")
async def download_app_code(
    appId: int,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if not appId or appId <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "应用ID无效")
    result = await db.execute(
        select(App).where(App.id == appId, App.isDelete == 0)
    )
    app = result.scalar_one_or_none()
    if not app:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在")
    if app.userId != current_user.id:
        raise BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码")

    code_gen_type = app.codeGenType
    source_dir_name = f"{code_gen_type}_{appId}"
    source_dir_path = os.path.join(settings.code_output_root_dir, source_dir_name)
    if not os.path.isdir(source_dir_path):
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码")

    # 打包为 ZIP 下载
    zip_buffer = io.BytesIO()
    with zipfile.ZipFile(zip_buffer, "w", zipfile.ZIP_DEFLATED) as zf:
        exclude = {"node_modules", ".git", "dist", "build", ".DS_Store", ".env",
                   "target", ".mvn", ".idea", ".vscode"}
        exclude_ext = {".log", ".tmp", ".cache"}
        for root, dirs, files in os.walk(source_dir_path):
            dirs[:] = [d for d in dirs if d not in exclude]
            for file in files:
                if os.path.splitext(file)[1] in exclude_ext:
                    continue
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, source_dir_path)
                zf.write(file_path, arcname)

    zip_buffer.seek(0)
    return StreamingResponse(
        zip_buffer,
        media_type="application/zip",
        headers={"Content-Disposition": f"attachment; filename={appId}.zip"},
    )


@router.post("/update", response_model=BaseResponse[bool])
async def update_app(
    request: AppUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if not request.id:
        raise BusinessException(ErrorCode.PARAMS_ERROR)
    result = await db.execute(
        select(App).where(App.id == request.id, App.isDelete == 0)
    )
    app = result.scalar_one_or_none()
    if not app:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR)
    if app.userId != current_user.id:
        raise BusinessException(ErrorCode.NO_AUTH_ERROR)
    if request.appName is not None:
        app.appName = request.appName
    app.editTime = datetime.now()
    await db.flush()
    return BaseResponse(data=True)


@router.post("/delete", response_model=BaseResponse[bool])
async def delete_app(
    request: DeleteRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if not request.id or request.id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR)
    result = await db.execute(
        select(App).where(App.id == request.id, App.isDelete == 0)
    )
    app = result.scalar_one_or_none()
    if not app:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR)
    if app.userId != current_user.id and current_user.userRole != "admin":
        raise BusinessException(ErrorCode.NO_AUTH_ERROR)

    # 删除关联的聊天历史
    await db.execute(
        ChatHistory.__table__.delete().where(ChatHistory.appId == request.id)
    )
    # 逻辑删除
    app.isDelete = 1
    await db.flush()
    return BaseResponse(data=True)


@router.get("/get/vo", response_model=BaseResponse[AppVO])
async def get_app_vo_by_id(
    id: int,
    db: AsyncSession = Depends(get_db),
):
    if id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR)
    result = await db.execute(
        select(App).where(App.id == id, App.isDelete == 0)
    )
    app = result.scalar_one_or_none()
    if not app:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR)
    # 查询关联用户
    from app.models.user import User as UserModel
    user_result = await db.execute(
        select(UserModel).where(UserModel.id == app.userId, UserModel.isDelete == 0)
    )
    user = user_result.scalar_one_or_none()
    return BaseResponse(data=get_app_vo(app, user))


@router.post("/my/list/page/vo", response_model=BaseResponse[PageResult[AppVO]])
async def list_my_app_vo_by_page(
    request: AppQueryRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if request.pageSize > 20:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用")
    # 强制只查当前用户
    request.userId = current_user.id
    return await _query_app_list(request, db)


@router.post("/good/list/page/vo", response_model=BaseResponse[PageResult[AppVO]])
async def list_good_app_vo_by_page(
    request: AppQueryRequest,
    db: AsyncSession = Depends(get_db),
):
    if request.pageSize > 20:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用")
    request.priority = GOOD_APP_PRIORITY
    return await _query_app_list(request, db)


@router.post("/add", response_model=BaseResponse[int])
async def add_app(
    request: AppAddRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    app_id = await create_app(db, request, current_user)
    return BaseResponse(data=app_id)


@router.post("/deploy", response_model=BaseResponse[str])
async def deploy(
    request: AppDeployRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    deploy_url = await deploy_app(db, request.appId, current_user)
    return BaseResponse(data=deploy_url)


# --- Admin endpoints ---

@router.post("/admin/delete", response_model=BaseResponse[bool])
async def admin_delete_app(
    request: DeleteRequest,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    if not request.id or request.id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR)
    result = await db.execute(
        select(App).where(App.id == request.id, App.isDelete == 0)
    )
    app = result.scalar_one_or_none()
    if not app:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR)
    await db.execute(
        ChatHistory.__table__.delete().where(ChatHistory.appId == request.id)
    )
    app.isDelete = 1
    await db.flush()
    return BaseResponse(data=True)


@router.post("/admin/update", response_model=BaseResponse[bool])
async def admin_update_app(
    request: AppAdminUpdateRequest,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    if not request.id:
        raise BusinessException(ErrorCode.PARAMS_ERROR)
    result = await db.execute(
        select(App).where(App.id == request.id, App.isDelete == 0)
    )
    app = result.scalar_one_or_none()
    if not app:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR)
    if request.appName is not None:
        app.appName = request.appName
    if request.cover is not None:
        app.cover = request.cover
    if request.priority is not None:
        app.priority = request.priority
    app.editTime = datetime.now()
    await db.flush()
    return BaseResponse(data=True)


@router.post("/admin/list/page/vo", response_model=BaseResponse[PageResult[AppVO]])
async def admin_list_app_vo_by_page(
    request: AppQueryRequest,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    return await _query_app_list(request, db)


@router.get("/admin/get/vo", response_model=BaseResponse[AppVO])
async def admin_get_app_vo_by_id(
    id: int,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    if id <= 0:
        raise BusinessException(ErrorCode.PARAMS_ERROR)
    result = await db.execute(
        select(App).where(App.id == id, App.isDelete == 0)
    )
    app = result.scalar_one_or_none()
    if not app:
        raise BusinessException(ErrorCode.NOT_FOUND_ERROR)
    from app.models.user import User as UserModel
    user_result = await db.execute(
        select(UserModel).where(UserModel.id == app.userId, UserModel.isDelete == 0)
    )
    user = user_result.scalar_one_or_none()
    return BaseResponse(data=get_app_vo(app, user))


# --- Helper ---

async def _query_app_list(
    request: AppQueryRequest,
    db: AsyncSession,
) -> BaseResponse[PageResult[AppVO]]:
    query = select(App).where(App.isDelete == 0)
    filters = build_app_query_filters(request)
    if filters:
        query = query.where(and_(*filters))

    if request.sortField:
        col = getattr(App, request.sortField, None)
        if col is not None:
            query = query.order_by(col.desc() if request.sortOrder == "descend" else col.asc())

    # 计数
    count_query = select(func.count()).select_from(App).where(App.isDelete == 0)
    if filters:
        count_query = count_query.where(and_(*filters))
    total = (await db.execute(count_query)).scalar()

    # 分页
    offset = (request.pageNum - 1) * request.pageSize
    query = query.offset(offset).limit(request.pageSize)
    result = await db.execute(query)
    apps = list(result.scalars().all())

    # 批量获取用户
    user_ids = {a.userId for a in apps}
    user_map = {}
    if user_ids:
        user_result = await db.execute(
            select(User).where(User.id.in_(user_ids), User.isDelete == 0)
        )
        for u in user_result.scalars().all():
            user_map[u.id] = u

    app_vo_list = get_app_vo_list(apps, user_map)
    page_result = PageResult(
        records=app_vo_list,
        total=total,
        size=request.pageSize,
        current=request.pageNum,
    )
    return BaseResponse(data=page_result)
