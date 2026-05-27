import os
import asyncio
import logging
from app.langgraph.state import WorkflowState

logger = logging.getLogger(__name__)


async def project_builder_node(state: WorkflowState) -> dict:
    """项目构建节点：用于 Vue 项目执行 npm install + npm run build"""
    context = state["workflow_context"]
    generated_code_dir = context.get("generated_code_dir", "")

    if not generated_code_dir or not os.path.isdir(generated_code_dir):
        context["error_message"] = "生成代码目录不存在"
        context["current_step"] = "build_error"
        return {"workflow_context": context}

    package_json = os.path.join(generated_code_dir, "package.json")
    if not os.path.exists(package_json):
        context["error_message"] = "package.json 不存在"
        context["current_step"] = "build_error"
        return {"workflow_context": context}

    logger.info(f"Project builder: building {generated_code_dir}")

    try:
        # npm install
        npm_cmd = "npm" if os.name != "nt" else "npm.cmd"
        proc = await asyncio.create_subprocess_exec(
            npm_cmd, "install",
            cwd=generated_code_dir,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        stdout, stderr = await asyncio.wait_for(proc.communicate(), timeout=300)
        if proc.returncode != 0:
            context["error_message"] = f"npm install failed: {stderr.decode()}"
            context["current_step"] = "build_error"
            return {"workflow_context": context}

        # npm run build
        proc = await asyncio.create_subprocess_exec(
            npm_cmd, "run", "build",
            cwd=generated_code_dir,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        stdout, stderr = await asyncio.wait_for(proc.communicate(), timeout=180)
        if proc.returncode != 0:
            context["error_message"] = f"npm run build failed: {stderr.decode()}"
            context["current_step"] = "build_error"
            return {"workflow_context": context}

        dist_dir = os.path.join(generated_code_dir, "dist")
        if not os.path.isdir(dist_dir):
            context["error_message"] = "构建完成但 dist 目录不存在"
            context["current_step"] = "build_error"
            return {"workflow_context": context}

        context["build_result_dir"] = dist_dir
        context["current_step"] = "build_success"
        logger.info(f"Project builder: build success, dist at {dist_dir}")

    except asyncio.TimeoutError:
        context["error_message"] = "构建超时"
        context["current_step"] = "build_error"
    except Exception as e:
        context["error_message"] = f"构建异常: {e}"
        context["current_step"] = "build_error"

    return {"workflow_context": context}
