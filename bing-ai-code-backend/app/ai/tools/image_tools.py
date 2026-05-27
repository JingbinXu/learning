import httpx
import logging
import json
from langchain_core.tools import tool

from app.config import get_settings
from app.ai.models import ImageResource

settings = get_settings()
logger = logging.getLogger(__name__)


def search_pexels_images(query: str, per_page: int = 3) -> list[ImageResource]:
    """搜索 Pexels 图片"""
    try:
        response = httpx.get(
            "https://api.pexels.com/v1/search",
            params={"query": query, "per_page": per_page},
            headers={"Authorization": settings.pexels_api_key},
            timeout=10,
        )
        response.raise_for_status()
        data = response.json()
        images = []
        for photo in data.get("photos", []):
            images.append(ImageResource(
                description=query,
                url=photo.get("src", {}).get("medium", ""),
                category="CONTENT",
            ))
        return images
    except Exception as e:
        logger.error(f"Pexels search error: {e}")
        return []


def generate_dashscope_image(prompt: str) -> ImageResource | None:
    """使用 DashScope 生成图片（Logo）"""
    try:
        response = httpx.post(
            f"{settings.dashscope_base_url}/images/generations",
            json={
                "model": settings.dashscope_image_model,
                "input": {"prompt": prompt},
                "parameters": {"n": 1, "size": "512*512"},
            },
            headers={
                "Authorization": f"Bearer {settings.dashscope_api_key}",
                "Content-Type": "application/json",
            },
            timeout=30,
        )
        response.raise_for_status()
        data = response.json()
        results = data.get("output", {}).get("results", [])
        if results:
            return ImageResource(
                description=prompt,
                url=results[0].get("url", ""),
                category="LOGO",
            )
        return None
    except Exception as e:
        logger.error(f"DashScope image generation error: {e}")
        return None


# LangChain tools wrapping the above functions
@tool
def image_search_tool(query: str) -> str:
    """搜索图片资源。参数：query - 搜索关键词"""
    images = search_pexels_images(query)
    return json.dumps([img.model_dump() for img in images], ensure_ascii=False)


@tool
def logo_generator_tool(description: str) -> str:
    """生成 Logo 图片。参数：description - Logo 描述"""
    result = generate_dashscope_image(description)
    if result:
        return json.dumps(result.model_dump(), ensure_ascii=False)
    return "Logo 生成失败"
