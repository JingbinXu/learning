import os
import json
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


def route_code_gen_type(init_prompt: str) -> CodeGenTypeEnum:
    """AI 智能选择代码生成类型"""
    system_prompt = _load_prompt("codegen-routing-system-prompt.txt")

    llm = ChatOpenAI(
        model=settings.dashscope_model,
        api_key=settings.dashscope_api_key,
        base_url=settings.dashscope_base_url,
        temperature=0,
        max_tokens=256,
    )

    response = llm.invoke([
        SystemMessage(content=system_prompt),
        HumanMessage(content=init_prompt),
    ])

    result_text = response.content.strip().lower()
    if "vue" in result_text or "vue_project" in result_text:
        return CodeGenTypeEnum.VUE_PROJECT
    elif "multi" in result_text or "multi_file" in result_text:
        return CodeGenTypeEnum.MULTI_FILE
    else:
        return CodeGenTypeEnum.HTML
