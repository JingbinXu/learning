from fastapi import APIRouter
from app.schemas.common import BaseResponse

router = APIRouter()


@router.get("/", response_model=BaseResponse[str])
async def health_check():
    return BaseResponse(data="ok!", message="ok")
