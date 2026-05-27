import logging
from app.langgraph.state import WorkflowState

logger = logging.getLogger(__name__)


async def prompt_enhancer_node(state: WorkflowState) -> dict:
    """提示词增强节点：将图片资源附加到用户 prompt"""
    context = state["workflow_context"]
    original_prompt = context["original_prompt"]
    image_list = context.get("image_list", [])

    enhanced_prompt = original_prompt

    if image_list:
        lines = ["\n## 可用素材资源", "请在生成网站使用以下图片资源："]
        for img in image_list:
            category = img.get("category", "")
            description = img.get("description", "")
            url = img.get("url", "")
            lines.append(f"- {category}：{description}（{url}）")
        enhanced_prompt = original_prompt + "\n" + "\n".join(lines)

    context["current_step"] = "prompt_enhancer"
    context["enhanced_prompt"] = enhanced_prompt

    logger.info(f"Prompt enhancer: enhanced prompt length = {len(enhanced_prompt)}")

    return {"workflow_context": context}
