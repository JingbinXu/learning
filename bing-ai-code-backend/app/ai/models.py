from pydantic import BaseModel


class HtmlCodeResult(BaseModel):
    htmlCode: str = ""
    description: str = ""


class MultiFileCodeResult(BaseModel):
    htmlCode: str = ""
    cssCode: str = ""
    jsCode: str = ""
    description: str = ""


class QualityResult(BaseModel):
    isValid: bool = True
    errors: list[str] = []
    suggestions: list[str] = []


class ImageResource(BaseModel):
    description: str = ""
    url: str = ""
    category: str = ""  # CONTENT, LOGO, ILLUSTRATION, ARCHITECTURE


class ImageCollectionPlan(BaseModel):
    contentImageTasks: list[str] = []
    illustrationTasks: list[str] = []
    diagramTasks: list[str] = []
    logoTasks: list[str] = []
