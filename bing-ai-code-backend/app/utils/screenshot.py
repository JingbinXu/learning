import os
import uuid
import asyncio
import logging
from datetime import datetime
from PIL import Image
import io

logger = logging.getLogger(__name__)


async def take_screenshot(url: str, output_dir: str = None) -> str | None:
    """使用 Playwright 截取网页截图，返回压缩后的 JPG 路径"""
    if output_dir is None:
        from app.config import get_settings
        settings = get_settings()
        output_dir = os.path.join(settings.project_root, "tmp", "screenshots")

    uid = uuid.uuid4().hex[:8]
    screenshot_dir = os.path.join(output_dir, uid)
    os.makedirs(screenshot_dir, exist_ok=True)

    try:
        from playwright.async_api import async_playwright

        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=True)
            page = await browser.new_page(viewport={"width": 1600, "height": 900})

            await page.goto(url, wait_until="networkidle", timeout=30000)
            # 等待动态内容
            await asyncio.sleep(2)

            screenshot_bytes = await page.screenshot(type="png", full_page=False)
            await browser.close()

        # 保存 PNG
        png_path = os.path.join(screenshot_dir, f"{uid}.png")
        with open(png_path, "wb") as f:
            f.write(screenshot_bytes)

        # 压缩为 JPG
        jpg_path = os.path.join(screenshot_dir, f"{uid}_compressed.jpg")
        img = Image.open(png_path)
        img = img.convert("RGB")
        img.save(jpg_path, "JPEG", quality=30)

        # 删除原 PNG
        os.remove(png_path)

        logger.info(f"Screenshot saved: {jpg_path}")
        return jpg_path

    except Exception as e:
        logger.error(f"Screenshot failed for {url}: {e}")
        return None


async def generate_and_upload_screenshot(web_url: str) -> str | None:
    """生成截图并上传到 COS"""
    from app.utils.cos_manager import upload_file

    # 截图
    screenshot_path = await take_screenshot(web_url)
    if not screenshot_path:
        return None

    try:
        # 生成 COS key
        now = datetime.now()
        uid = uuid.uuid4().hex[:8]
        cos_key = f"/screenshots/{now.year}/{now.month:02d}/{now.day:02d}/{uid}_compressed.jpg"

        # 上传
        cos_url = upload_file(cos_key, screenshot_path)

        # 清理本地文件
        if os.path.exists(screenshot_path):
            os.remove(screenshot_path)
            # 清理目录
            screenshot_dir = os.path.dirname(screenshot_path)
            if not os.listdir(screenshot_dir):
                os.rmdir(screenshot_dir)

        return cos_url

    except Exception as e:
        logger.error(f"Screenshot upload failed: {e}")
        return None
