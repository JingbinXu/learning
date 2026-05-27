import logging
from app.langgraph.state import WorkflowState
from app.ai.routing_service import route_code_gen_type

logger = logging.getLogger(__name__)


async def router_node(state: WorkflowState) -> dict:
    """路由节点：根据用户 prompt 选择代码生成类型"""
    context = state["workflow_context"]
    original_prompt = context["original_prompt"]

    logger.info(f"Router node: routing prompt '{original_prompt[:50]}...'")

    code_gen_type = route_code_gen_type(original_prompt)

    context["current_step"] = "router"
    context["generation_type"] = code_gen_type.value

    logger.info(f"Router selected: {code_gen_type.value}")

    return {"workflow_context": context}
