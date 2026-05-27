import os
import asyncio
import logging

logger = logging.getLogger(__name__)


async def build_vue_project(project_path: str) -> bool:
    """构建 Vue 项目：npm install + npm run build"""
    if not os.path.isdir(project_path):
        logger.error(f"Project path does not exist: {project_path}")
        return False

    package_json = os.path.join(project_path, "package.json")
    if not os.path.exists(package_json):
        logger.error(f"package.json not found in {project_path}")
        return False

    npm_cmd = "npm" if os.name != "nt" else "npm.cmd"

    try:
        # npm install
        logger.info(f"Running npm install in {project_path}")
        proc = await asyncio.create_subprocess_exec(
            npm_cmd, "install",
            cwd=project_path,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        _, stderr = await asyncio.wait_for(proc.communicate(), timeout=300)
        if proc.returncode != 0:
            logger.error(f"npm install failed: {stderr.decode()}")
            return False

        # npm run build
        logger.info(f"Running npm run build in {project_path}")
        proc = await asyncio.create_subprocess_exec(
            npm_cmd, "run", "build",
            cwd=project_path,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        _, stderr = await asyncio.wait_for(proc.communicate(), timeout=180)
        if proc.returncode != 0:
            logger.error(f"npm run build failed: {stderr.decode()}")
            return False

        dist_dir = os.path.join(project_path, "dist")
        if not os.path.isdir(dist_dir):
            logger.error("Build succeeded but dist directory not found")
            return False

        logger.info(f"Vue project built successfully: {dist_dir}")
        return True

    except asyncio.TimeoutError:
        logger.error("Build timed out")
        return False
    except Exception as e:
        logger.error(f"Build error: {e}")
        return False
