import os
from langchain_core.tools import tool

from app.config import get_settings
from app.schemas.enums import CodeGenTypeEnum

settings = get_settings()


def _get_project_dir(app_id: int) -> str:
    """获取项目目录"""
    dir_path = os.path.join(
        settings.code_output_root_dir,
        f"{CodeGenTypeEnum.VUE_PROJECT.value}_{app_id}",
    )
    os.makedirs(dir_path, exist_ok=True)
    return dir_path


def create_file_tools(app_id: int):
    """为指定应用创建文件操作工具集"""
    project_dir = _get_project_dir(app_id)

    @tool
    def write_file(file_path: str, content: str) -> str:
        """写入文件到项目目录。参数：file_path - 相对于项目根目录的文件路径，content - 文件内容"""
        abs_path = os.path.join(project_dir, file_path)
        os.makedirs(os.path.dirname(abs_path), exist_ok=True)
        with open(abs_path, "w", encoding="utf-8") as f:
            f.write(content)
        return f"文件写入成功: {file_path}"

    @tool
    def read_file(file_path: str) -> str:
        """读取项目目录中的文件内容。参数：file_path - 相对于项目根目录的文件路径"""
        abs_path = os.path.join(project_dir, file_path)
        if not os.path.exists(abs_path):
            return f"文件不存在: {file_path}"
        with open(abs_path, "r", encoding="utf-8") as f:
            return f.read()

    @tool
    def modify_file(file_path: str, old_content: str, new_content: str) -> str:
        """修改文件中的指定内容。参数：file_path - 文件路径，old_content - 要替换的旧内容，new_content - 新内容"""
        abs_path = os.path.join(project_dir, file_path)
        if not os.path.exists(abs_path):
            return f"文件不存在: {file_path}"
        with open(abs_path, "r", encoding="utf-8") as f:
            content = f.read()
        if old_content not in content:
            return f"未找到要替换的内容"
        content = content.replace(old_content, new_content, 1)
        with open(abs_path, "w", encoding="utf-8") as f:
            f.write(content)
        return f"文件修改成功: {file_path}"

    @tool
    def delete_file(file_path: str) -> str:
        """删除项目目录中的文件。参数：file_path - 相对于项目根目录的文件路径"""
        abs_path = os.path.join(project_dir, file_path)
        if not os.path.exists(abs_path):
            return f"文件不存在: {file_path}"
        os.remove(abs_path)
        return f"文件删除成功: {file_path}"

    @tool
    def read_dir(dir_path: str = "") -> str:
        """读取项目目录中的文件列表。参数：dir_path - 相对于项目根目录的目录路径，空字符串表示根目录"""
        abs_path = os.path.join(project_dir, dir_path) if dir_path else project_dir
        if not os.path.exists(abs_path):
            return f"目录不存在: {dir_path}"
        items = []
        for item in os.listdir(abs_path):
            item_path = os.path.join(abs_path, item)
            if os.path.isdir(item_path):
                items.append(f"[DIR] {item}")
            else:
                items.append(f"[FILE] {item}")
        return "\n".join(items) if items else "目录为空"

    @tool
    def exit_tool(message: str = "代码生成完成") -> str:
        """当代码生成完成时调用此工具退出。参数：message - 完成消息"""
        return message

    return [write_file, read_file, modify_file, delete_file, read_dir, exit_tool]
