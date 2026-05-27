from enum import Enum


class CodeGenTypeEnum(Enum):
    HTML = "html"
    MULTI_FILE = "multi_file"
    VUE_PROJECT = "vue_project"

    @classmethod
    def get_by_value(cls, value: str):
        for member in cls:
            if member.value == value:
                return member
        return None


class UserRoleEnum(Enum):
    USER = "user"
    ADMIN = "admin"

    @classmethod
    def get_by_value(cls, value: str):
        for member in cls:
            if member.value == value:
                return member
        return None


class ChatHistoryMessageTypeEnum(Enum):
    USER = "user"
    AI = "ai"

    @classmethod
    def get_by_value(cls, value: str):
        for member in cls:
            if member.value == value:
                return member
        return None


class StreamMessageTypeEnum(Enum):
    AI_RESPONSE = "ai_response"
    TOOL_REQUEST = "tool_request"
    TOOL_EXECUTED = "tool_executed"


class ImageCategoryEnum(Enum):
    CONTENT = "CONTENT"
    LOGO = "LOGO"
    ILLUSTRATION = "ILLUSTRATION"
    ARCHITECTURE = "ARCHITECTURE"


class RateLimitType(Enum):
    API = "api"
    USER = "user"
    IP = "ip"
