import logging
import json
from app.schemas.enums import CodeGenTypeEnum
from app.ai.code_gen_service import generate_code_stream
from app.core.code_parser import execute_parser
from app.core.code_saver import execute_saver
from app.ai.models import HtmlCodeResult, MultiFileCodeResult

logger = logging.getLogger(__name__)


async def generate_and_save_code_stream(
    user_message: str,
    code_gen_type: CodeGenTypeEnum,
    app_id: int,
):
    """统一入口：生成代码并保存（流式返回给前端）"""
    if not code_gen_type:
        raise ValueError("生成类型不能为空")

    if code_gen_type == CodeGenTypeEnum.VUE_PROJECT:
        # Vue 项目：流式返回，工具调用时 AI 直接写文件
        async for chunk in generate_vue_stream_with_save(user_message, app_id):
            yield chunk
    else:
        # HTML / MULTI_FILE：流式返回，完成后解析保存
        code_builder = []
        async for chunk in generate_code_stream(user_message, code_gen_type, app_id):
            code_builder.append(chunk)
            yield chunk

        # 流式完成后解析并保存
        try:
            complete_code = "".join(code_builder)
            parsed_result = execute_parser(complete_code, code_gen_type.value)
            save_dir = execute_saver(parsed_result, code_gen_type.value, app_id)
            logger.info(f"文件创建完成，目录为: {save_dir}")
        except Exception as e:
            logger.error(f"保存失败: {e}")


async def generate_vue_stream_with_save(user_message: str, app_id: int):
    """Vue 项目流式生成（含工具调用信息）"""
    async for chunk in generate_code_stream(
        user_message, CodeGenTypeEnum.VUE_PROJECT, app_id
    ):
        # 包装为 AI 响应消息格式（与 Java 版 JsonMessageStreamHandler 一致）
        msg = {"type": "ai_response", "content": chunk}
        yield json.dumps(msg, ensure_ascii=False)


async def generate_and_save_code(
    user_message: str,
    code_gen_type: CodeGenTypeEnum,
    app_id: int,
) -> str:
    """非流式生成并保存（用于工作流等场景）"""
    if not code_gen_type:
        raise ValueError("生成类型不能为空")

    code_chunks = []
    async for chunk in generate_code_stream(user_message, code_gen_type, app_id):
        code_chunks.append(chunk)

    complete_code = "".join(code_chunks)

    if code_gen_type != CodeGenTypeEnum.VUE_PROJECT:
        parsed_result = execute_parser(complete_code, code_gen_type.value)
        save_dir = execute_saver(parsed_result, code_gen_type.value, app_id)
        return save_dir
    else:
        # Vue 项目由工具直接写文件，目录为 {type}_{appId}
        from app.config import get_settings
        settings = get_settings()
        import os
        save_dir = os.path.join(
            settings.code_output_root_dir,
            f"{CodeGenTypeEnum.VUE_PROJECT.value}_{app_id}",
        )
        return save_dir
