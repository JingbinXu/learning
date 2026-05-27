import os
from pathlib import Path
from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # Server
    server_host: str = "0.0.0.0"
    server_port: int = 8124
    api_prefix: str = "/api"

    # MySQL
    mysql_host: str = "localhost"
    mysql_port: int = 3306
    mysql_user: str = "root"
    mysql_password: str = "123456"
    mysql_database: str = "bing_ai_code"

    # Redis
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_password: str = ""
    redis_db: int = 0
    redis_ttl: int = 3600

    # Session
    session_secret_key: str = "your-secret-key-change-in-production"
    session_max_age: int = 2592000

    # DeepSeek AI
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com/"
    deepseek_model: str = "deepseek-chat"
    deepseek_reasoning_model: str = "deepseek-reasoner"
    deepseek_max_tokens: int = 8129
    deepseek_reasoning_max_tokens: int = 32768

    # DashScope (Qwen routing)
    dashscope_api_key: str = ""
    dashscope_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    dashscope_model: str = "qwen-turbo"
    dashscope_image_model: str = "wan2.2-t2i-flash"

    # Tencent COS
    cos_host: str = ""
    cos_secret_id: str = ""
    cos_secret_key: str = ""
    cos_region: str = "ap-beijing"
    cos_bucket: str = ""

    # Pexels
    pexels_api_key: str = ""

    # Code Deploy
    code_deploy_host: str = "http://localhost"

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}

    @property
    def mysql_url(self) -> str:
        return (
            f"mysql+aiomysql://{self.mysql_user}:{self.mysql_password}"
            f"@{self.mysql_host}:{self.mysql_port}/{self.mysql_database}"
        )

    @property
    def redis_url(self) -> str:
        if self.redis_password:
            return f"redis://:{self.redis_password}@{self.redis_host}:{self.redis_port}/{self.redis_db}"
        return f"redis://{self.redis_host}:{self.redis_port}/{self.redis_db}"

    @property
    def project_root(self) -> Path:
        return Path(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

    @property
    def code_output_root_dir(self) -> str:
        return str(self.project_root / "tmp" / "code_output")

    @property
    def code_deploy_root_dir(self) -> str:
        return str(self.project_root / "tmp" / "code_deploy")

    @property
    def prompts_dir(self) -> str:
        return str(self.project_root / "prompts")


@lru_cache()
def get_settings() -> Settings:
    return Settings()
