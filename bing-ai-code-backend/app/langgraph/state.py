from typing import TypedDict, Annotated, Optional
from langgraph.graph import add_messages
from app.ai.models import QualityResult, ImageResource, ImageCollectionPlan
from app.schemas.enums import CodeGenTypeEnum


class WorkflowContext(TypedDict):
    current_step: str
    original_prompt: str
    image_list_str: str
    image_list: list[dict]  # ImageResource as dict
    enhanced_prompt: str
    generation_type: str  # CodeGenTypeEnum value
    generated_code_dir: str
    build_result_dir: str
    quality_result: Optional[dict]  # QualityResult as dict
    error_message: str
    image_collection_plan: Optional[dict]  # ImageCollectionPlan as dict
    content_images: list[dict]
    illustrations: list[dict]
    diagrams: list[dict]
    logos: list[dict]


class WorkflowState(TypedDict):
    messages: Annotated[list, add_messages]
    workflow_context: WorkflowContext


def create_initial_context(prompt: str) -> WorkflowContext:
    """创建工作流初始上下文"""
    return WorkflowContext(
        current_step="start",
        original_prompt=prompt,
        image_list_str="",
        image_list=[],
        enhanced_prompt="",
        generation_type="",
        generated_code_dir="",
        build_result_dir="",
        quality_result=None,
        error_message="",
        image_collection_plan=None,
        content_images=[],
        illustrations=[],
        diagrams=[],
        logos=[],
    )
