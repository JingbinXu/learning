import logging
from app.langgraph.state import WorkflowState
from app.schemas.enums import CodeGenTypeEnum
from app.core.code_gen_facade import generate_and_save_code

logger = logging.getLogger(__name__)


async def code_generator_node(state: WorkflowState) -> dict:
    """代码生成节点：调用 AI 生成代码并保存"""
    context = state["workflow_context"]
    generation_type = context["generation_type"]
    quality_result = context.get("quality_result")

    # 根据是否需要修复选择提示词
    if quality_result and not quality_result.get("isValid", True):
        errors = quality_result.get("errors", [])
        suggestions = quality_result.get("suggestions", [])
        error_text = "\n".join(errors)
        suggestion_text = "\n".join(suggestions)
        user_message = (
            f"{context['original_prompt']}\n\n"
            f"## 上次生成的代码存在以下问题，请修复：\n"
            f"### 错误：\n{error_text}\n"
            f"### 建议：\n{suggestion_text}"
        )
    else:
        user_message = context.get("enhanced_prompt") or context["original_prompt"]

    code_gen_type = CodeGenTypeEnum.get_by_value(generation_type)
    if not code_gen_type:
        context["error_message"] = f"不支持的代码生成类型: {generation_type}"
        context["current_step"] = "code_generator_error"
        return {"workflow_context": context}

    logger.info(f"Code generator: type={generation_type}")

    try:
        generated_code_dir = await generate_and_save_code(
            user_message, code_gen_type, 0
        )
        context["current_step"] = "code_generator"
        context["generated_code_dir"] = generated_code_dir
        # 重置质检结果
        context["quality_result"] = None
    except Exception as e:
        logger.error(f"Code generator error: {e}")
        context["error_message"] = str(e)
        context["current_step"] = "code_generator_error"

    return {"workflow_context": context}
