import time
import logging
from functools import wraps
from fastapi import Request, HTTPException
import redis.asyncio as aioredis

from app.config import get_settings
from app.exceptions import BusinessException, ErrorCode

settings = get_settings()
logger = logging.getLogger(__name__)

_redis_client = None


async def get_redis():
    global _redis_client
    if _redis_client is None:
        _redis_client = aioredis.from_url(
            settings.redis_url,
            decode_responses=True,
        )
    return _redis_client


async def check_rate_limit(
    key: str,
    rate: int = 5,
    rate_interval: int = 60,
) -> bool:
    """检查 Redis 限流，返回是否允许通过"""
    redis = await get_redis()
    try:
        current = await redis.get(key)
        if current is None:
            # 首次请求
            await redis.setex(key, rate_interval, 1)
            return True

        count = int(current)
        if count >= rate:
            return False

        await redis.incr(key)
        return True

    except Exception as e:
        logger.error(f"Rate limit check error: {e}")
        return True  # Redis 故障时放行


async def rate_limit_by_user(
    request: Request,
    rate: int = 5,
    rate_interval: int = 60,
):
    """按用户限流的 FastAPI 依赖"""
    user_data = request.session.get("user")
    if user_data and isinstance(user_data, dict):
        user_id = user_data.get("id", "unknown")
    else:
        # 降级为 IP 限流
        user_id = request.client.host if request.client else "unknown"

    key = f"rate_limit:user:{user_id}"
    allowed = await check_rate_limit(key, rate, rate_interval)
    if not allowed:
        raise BusinessException(ErrorCode.TOO_MANY_REQUEST, "请勿频繁请求")


async def rate_limit_by_ip(
    request: Request,
    rate: int = 5,
    rate_interval: int = 60,
):
    """按 IP 限流"""
    client_ip = request.headers.get("X-Forwarded-For", "").split(",")[0].strip()
    if not client_ip:
        client_ip = request.headers.get("X-Real-IP", "")
    if not client_ip:
        client_ip = request.client.host if request.client else "unknown"

    key = f"rate_limit:ip:{client_ip}"
    allowed = await check_rate_limit(key, rate, rate_interval)
    if not allowed:
        raise BusinessException(ErrorCode.TOO_MANY_REQUEST, "请勿频繁请求")
