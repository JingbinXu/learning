from fastapi import APIRouter, Query
from sse_starlette.sse import EventSourceResponse

from app.schemas.common import BaseResponse
from app.langgraph.workflow import execute_workflow, execute_workflow_with_sse

router = APIRouter(prefix="/workflow", tags=["workflow"])


@router.post("/execute")
async def execute(prompt: str = Query(...)):
    """同步执行工作流，返回最终上下文"""
    result = await execute_workflow(prompt)
    return BaseResponse(data=result)


@router.get("/execute-sse")
async def execute_sse(prompt: str = Query(...)):
    """SSE 流式执行工作流"""
    return EventSourceResponse(execute_workflow_with_sse(prompt))


@router.get("/execute-flux")
async def execute_flux(prompt: str = Query(...)):
    """Flux/SSE 流式执行工作流（同 execute-sse）"""
    return EventSourceResponse(execute_workflow_with_sse(prompt))
