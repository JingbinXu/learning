import os
from app.config import get_settings
from app.ai.models import HtmlCodeResult, MultiFileCodeResult
from app.schemas.enums import CodeGenTypeEnum

settings = get_settings()


def write_to_file(dir_path: str, filename: str, content: str):
    """写入文件到指定目录"""
    if content is None:
        return
    os.makedirs(dir_path, exist_ok=True)
    file_path = os.path.join(dir_path, filename)
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)


def build_unique_dir(code_type: str, app_id: int) -> str:
    """构建唯一目录: {code_type}_{appId}"""
    unique_dir_name = f"{code_type}_{app_id}"
    dir_path = os.path.join(settings.code_output_root_dir, unique_dir_name)
    os.makedirs(dir_path, exist_ok=True)
    return dir_path


def save_html_code(result: HtmlCodeResult, app_id: int) -> str:
    """保存 HTML 单文件代码"""
    if not result or not result.htmlCode:
        raise ValueError("输入不能为空")
    dir_path = build_unique_dir(CodeGenTypeEnum.HTML.value, app_id)
    write_to_file(dir_path, "index.html", result.htmlCode)
    return dir_path


def save_multi_file_code(result: MultiFileCodeResult, app_id: int) -> str:
    """保存多文件代码 (HTML + CSS + JS)"""
    if not result or not result.htmlCode:
        raise ValueError("HTML代码内容不能为空")
    dir_path = build_unique_dir(CodeGenTypeEnum.MULTI_FILE.value, app_id)
    write_to_file(dir_path, "index.html", result.htmlCode)
    write_to_file(dir_path, "style.css", result.cssCode)
    write_to_file(dir_path, "script.js", result.jsCode)
    return dir_path


def execute_saver(code_result, code_gen_type: str, app_id: int) -> str:
    """根据代码生成类型执行保存"""
    if code_gen_type == CodeGenTypeEnum.HTML.value:
        return save_html_code(code_result, app_id)
    elif code_gen_type == CodeGenTypeEnum.MULTI_FILE.value:
        return save_multi_file_code(code_result, app_id)
    else:
        raise ValueError(f"不支持的代码生成类型: {code_gen_type}")
