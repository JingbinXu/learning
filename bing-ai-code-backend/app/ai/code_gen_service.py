import os
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage

from app.config import get_settings
from app.schemas.enums import CodeGenTypeEnum

settings = get_settings()

PROMPT_DIR = settings.prompts_dir


def _load_prompt(filename: str) -> str:
    path = os.path.join(PROMPT_DIR, filename)
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def create_code_gen_llm(
    model: str | None = None,
    max_tokens: int | None = None,
    streaming: bool = True,
    reasoning: bool = False,
) -> ChatOpenAI:
    """创建代码生成 LLM 实例"""
    return ChatOpenAI(
        model=model or (settings.deepseek_reasoning_model if reasoning else settings.deepseek_model),
        api_key=settings.deepseek_api_key,
        base_url=settings.deepseek_base_url,
        max_tokens=max_tokens or (settings.deepseek_reasoning_max_tokens if reasoning else settings.deepseek_max_tokens),
        streaming=streaming,
        temperature=0.1 if reasoning else 0,
    )


async def generate_html_code_stream(user_message: str):
    """生成 HTML 单文件代码 (流式)"""
    system_prompt = _load_prompt("codegen-html-system-prompt.txt")
    llm = create_code_gen_llm()

    async for chunk in llm.astream([
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_message),
    ]):
        if chunk.content:
            yield chunk.content


async def generate_multi_file_code_stream(user_message: str):
    """生成多文件代码 (HTML + CSS + JS) (流式)"""
    system_prompt = _load_prompt("codegen-multi-file-system-prompt.txt")
    llm = create_code_gen_llm()

    async for chunk in llm.astream([
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_message),
    ]):
        if chunk.content:
            yield chunk.content


async def generate_vue_project_code_stream(app_id: int, user_message: str):
    """生成 Vue 项目代码 (流式, 带工具调用)"""
    system_prompt = _load_prompt("codegen-vue-project-system-prompt.txt")
    llm = create_code_gen_llm(reasoning=True)

    # TODO: 绑定工具 (Phase 8 - file write/read tools)
    # tools = get_vue_tools(app_id)
    # llm_with_tools = llm.bind_tools(tools)

    async for chunk in llm.astream([
        SystemMessage(content=system_prompt),
        HumanMessage(content=user_message),
    ]):
        if chunk.content:
            yield chunk.content


async def generate_code_stream(
    user_message: str,
    code_gen_type: CodeGenTypeEnum,
    app_id: int = 0,
):
    """统一入口：根据类型生成代码 (流式)"""
    if code_gen_type == CodeGenTypeEnum.HTML:
        async for chunk in generate_html_code_stream(user_message):
            yield chunk
    elif code_gen_type == CodeGenTypeEnum.MULTI_FILE:
        async for chunk in generate_multi_file_code_stream(user_message):
            yield chunk
    elif code_gen_type == CodeGenTypeEnum.VUE_PROJECT:
        async for chunk in generate_vue_project_code_stream(app_id, user_message):
            yield chunk
    else:
        raise ValueError(f"不支持的生成类型：{code_gen_type.value}")


async def route_code_gen_type(init_prompt: str) -> CodeGenTypeEnum:
    """路由选择代码生成类型"""
    from app.ai.routing_service import route_code_gen_type as _route
    return _route(init_prompt)


async def check_code_quality(code_content: str) -> dict:
    """检查代码质量"""
    system_prompt = _load_prompt("code-quality-check-system-prompt.txt")
    llm = ChatOpenAI(
        model=settings.deepseek_model,
        api_key=settings.deepseek_api_key,
        base_url=settings.deepseek_base_url,
        max_tokens=2048,
    )

    response = llm.invoke([
        SystemMessage(content=system_prompt),
        HumanMessage(content=f"请检查以下代码的质量：\n\n{code_content}"),
    ])

    result_text = response.content.strip()
    try:
        return json.loads(result_text)
    except json.JSONDecodeError:
        return {"isValid": True, "errors": [], "suggestions": []}
