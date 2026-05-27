import re
from app.ai.models import HtmlCodeResult, MultiFileCodeResult


class HtmlCodeParser:
    HTML_CODE_PATTERN = re.compile(r"```html\s*\n([\s\S]*?)```", re.IGNORECASE)

    def parse_code(self, code_content: str) -> HtmlCodeResult:
        html_code = self._extract_html_code(code_content)
        if html_code and html_code.strip():
            return HtmlCodeResult(htmlCode=html_code.strip())
        return HtmlCodeResult(htmlCode=code_content.strip())

    def _extract_html_code(self, content: str) -> str | None:
        match = self.HTML_CODE_PATTERN.search(content)
        return match.group(1) if match else None


class MultiFileCodeParser:
    HTML_CODE_PATTERN = re.compile(r"```html\s*\n([\s\S]*?)```", re.IGNORECASE)
    CSS_CODE_PATTERN = re.compile(r"```css\s*\n([\s\S]*?)```", re.IGNORECASE)
    JS_CODE_PATTERN = re.compile(r"```(?:js|javascript)\s*\n([\s\S]*?)```", re.IGNORECASE)

    def parse_code(self, code_content: str) -> MultiFileCodeResult:
        html_code = self._extract_by_pattern(code_content, self.HTML_CODE_PATTERN)
        css_code = self._extract_by_pattern(code_content, self.CSS_CODE_PATTERN)
        js_code = self._extract_by_pattern(code_content, self.JS_CODE_PATTERN)

        result = MultiFileCodeResult()
        if html_code and html_code.strip():
            result.htmlCode = html_code.strip()
        if css_code and css_code.strip():
            result.cssCode = css_code.strip()
        if js_code and js_code.strip():
            result.jsCode = js_code.strip()
        return result

    def _extract_by_pattern(self, content: str, pattern: re.Pattern) -> str | None:
        match = pattern.search(content)
        return match.group(1) if match else None


html_parser = HtmlCodeParser()
multi_file_parser = MultiFileCodeParser()


def execute_parser(code_content: str, code_gen_type: str):
    from app.schemas.enums import CodeGenTypeEnum
    if code_gen_type == CodeGenTypeEnum.HTML.value:
        return html_parser.parse_code(code_content)
    elif code_gen_type == CodeGenTypeEnum.MULTI_FILE.value:
        return multi_file_parser.parse_code(code_content)
    else:
        raise ValueError(f"不支持的代码生成类型: {code_gen_type}")
