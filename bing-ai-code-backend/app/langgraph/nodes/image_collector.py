import logging
from app.langgraph.state import WorkflowState

logger = logging.getLogger(__name__)


async def image_collector_node(state: WorkflowState) -> dict:
    """图片采集节点：收集各种类型的图片资源"""
    context = state["workflow_context"]
    original_prompt = context["original_prompt"]

    logger.info(f"Image collector: collecting images for '{original_prompt[:50]}...'")

    # TODO: 实现完整的图片采集逻辑
    # 1. 调用 ImageCollectionPlanService 获取采集计划
    # 2. 并发执行各类型图片采集
    #    - ImageSearchTool (Pexels) -> 内容图片
    #    - LogoGeneratorTool (DashScope) -> Logo
    #    - MermaidDiagramTool -> 架构图
    #    - UndrawIllustrationTool -> 插画
    # 3. 汇总结果

    # 暂时跳过图片采集
    context["current_step"] = "image_collector"
    context["image_list"] = []
    context["image_list_str"] = ""

    logger.info("Image collector: skipped (not yet implemented)")

    return {"workflow_context": context}
