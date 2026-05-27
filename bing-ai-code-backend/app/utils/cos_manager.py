import os
from qcloud_cos import CosConfig, CosS3Client
from app.config import get_settings

settings = get_settings()


def _get_cos_client() -> CosS3Client:
    """获取 COS 客户端"""
    config = CosConfig(
        Region=settings.cos_region,
        SecretId=settings.cos_secret_id,
        SecretKey=settings.cos_secret_key,
    )
    return CosS3Client(config)


def upload_file(key: str, file_path: str) -> str | None:
    """上传文件到 COS，返回 URL"""
    client = _get_cos_client()
    try:
        with open(file_path, "rb") as f:
            client.put_object(
                Bucket=settings.cos_bucket,
                Body=f,
                Key=key,
            )
        return f"{settings.cos_host}/{key}"
    except Exception:
        return None


def upload_bytes(key: str, data: bytes) -> str | None:
    """上传字节数据到 COS，返回 URL"""
    client = _get_cos_client()
    try:
        client.put_object(
            Bucket=settings.cos_bucket,
            Body=data,
            Key=key,
        )
        return f"{settings.cos_host}/{key}"
    except Exception:
        return None
