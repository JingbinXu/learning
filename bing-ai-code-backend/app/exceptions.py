from enum import IntEnum


class ErrorCode(IntEnum):
    SUCCESS = 0
    PARAMS_ERROR = 40000
    NOT_LOGIN_ERROR = 40100
    NO_AUTH_ERROR = 40101
    TOO_MANY_REQUEST = 42900
    NOT_FOUND_ERROR = 40400
    FORBIDDEN_ERROR = 40300
    SYSTEM_ERROR = 50000
    OPERATION_ERROR = 50001


class BusinessException(Exception):
    def __init__(self, code: ErrorCode, message: str = ""):
        self.code = code
        self.message = message or code.name
        super().__init__(self.message)
