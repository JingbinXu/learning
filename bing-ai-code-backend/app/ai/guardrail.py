import re
from app.exceptions import BusinessException, ErrorCode

# 敏感词列表
SENSITIVE_WORDS = [
    "忽略之前的指令", "ignore previous instructions", "破解",
    "hack", "bypass", "jailbreak", "ignore above",
    "disregard", "override", "system prompt", "reveal",
]

# 注入模式
INJECTION_PATTERNS = [
    r"ignore\s+(?:previous|above|all)\s+(?:instructions?|prompts?|rules?)",
    r"(?:you are|act as|pretend to be)\s+(?:a|an)\s+(?:hacked|jailbroken)",
    r"(?:reveal|show|display)\s+(?:your|the)\s+(?:system\s+)?(?:prompt|instructions?|rules?)",
]

INJECTION_REGEX = [re.compile(p, re.IGNORECASE) for p in INJECTION_PATTERNS]


def check_input_guardrail(user_input: str) -> str:
    """输入安全检查，返回清理后的输入或抛出异常"""
    if not user_input or not user_input.strip():
        raise BusinessException(ErrorCode.PARAMS_ERROR, "输入不能为空")

    if len(user_input) > 1000:
        raise BusinessException(ErrorCode.PARAMS_ERROR, "输入内容过长，请控制在1000字以内")

    # 检查敏感词
    lower_input = user_input.lower()
    for word in SENSITIVE_WORDS:
        if word.lower() in lower_input:
            raise BusinessException(ErrorCode.PARAMS_ERROR, "输入包含不当内容")

    # 检查注入模式
    for pattern in INJECTION_REGEX:
        if pattern.search(user_input):
            raise BusinessException(ErrorCode.PARAMS_ERROR, "输入包含潜在的提示注入攻击")

    return user_input


def check_output_guardrail(ai_output: str) -> bool:
    """输出安全检查，返回是否通过"""
    if not ai_output or len(ai_output.strip()) < 10:
        return False

    # 检查是否包含敏感内容
    output_sensitive_words = [
        "密码", "password", "secret", "token", "api key",
        "api_key", "credential", "private key",
    ]
    lower_output = ai_output.lower()
    for word in output_sensitive_words:
        if word in lower_output:
            return False

    return True
