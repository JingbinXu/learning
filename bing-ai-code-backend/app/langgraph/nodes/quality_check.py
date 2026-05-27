import os
import logging
from app.langgraph.state import WorkflowState
from app.ai.code_gen_service import check_code_quality

logger = logging.getLogger(__name__)

# 允许检查的代码文件扩展名
CODE_EXTENSIONS = {".html", ".css", ".js", ".vue", ".ts", ".jsx", ".tsx"}
# 排除的目录
EXCLUDE_DIRS = {"node_modules", "dist", ".git", "build"}


async def code_quality_check_node(state: WorkflowState) -> dict:
    """代码质量检查节点"""
    context = state["workflow_context"]
    generated_code_dir = context.get("generated_code_dir", "")

    if not generated_code_dir or not os.path.isdir(generated_code_dir):
        context["quality_result"] = {"isValid": True, "errors": [], "suggestions": []}
        context["current_step"] = "quality_check_skip"
        return {"workflow_context": context}

    # 读取所有代码文件
    code_contents = []
    for root, dirs, files in os.walk(generated_code_dir):
        # 排除特定目录
        dirs[:] = [d for d in dirs if d not in EXCLUDE_DIRS]
        for filename in files:
            ext = os.path.splitext(filename)[1].lower()
            if ext in CODE_EXTENSIONS:
                file_path = os.path.join(root, filename)
                try:
                    with open(file_path, "r", encoding="utf-8") as f:
                        content = f.read()
                    rel_path = os.path.relpath(file_path, generated_code_dir)
                    code_contents.append(f"--- {rel_path} ---\n{content}")
                except Exception as e:
                    logger.warning(f"Failed to read {file_path}: {e}")

    if not code_contents:
        context["quality_result"] = {"isValid": True, "errors": [], "suggestions": []}
        context["current_step"] = "quality_check_skip"
        return {"workflow_context": context}

    all_code = "\n\n".join(code_contents)

    logger.info(f"Quality check: checking {len(code_contents)} files")

    try:
        quality_result = await check_code_quality(all_code)
        context["quality_result"] = quality_result
        context["current_step"] = "quality_check"
    except Exception as e:
        logger.error(f"Quality check error: {e}")
        context["quality_result"] = {"isValid": True, "errors": [], "suggestions": []}
        context["current_step"] = "quality_check_error"

    return {"workflow_context": context}


def should_build(state: WorkflowState) -> str:
    """条件路由：根据质检结果决定下一步"""
    context = state["workflow_context"]
    quality_result = context.get("quality_result")
    error_message = context.get("error_message", "")

    if error_message:
        return "error"

    if quality_result and not quality_result.get("isValid", True):
        return "fail"

    generation_type = context.get("generation_type", "")
    if generation_type == "vue_project":
        return "build"

    return "skip_build"
