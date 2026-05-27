from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field
from app.schemas.common import PageRequest
from app.schemas.user import UserVO


# --- Request DTOs ---

class AppAddRequest(BaseModel):
    initPrompt: str = Field(alias="initPrompt")

    class Config:
        populate_by_name = True


class AppUpdateRequest(BaseModel):
    id: int = Field(alias="id")
    appName: Optional[str] = Field(default=None, alias="appName")

    class Config:
        populate_by_name = True


class AppAdminUpdateRequest(BaseModel):
    id: int = Field(alias="id")
    appName: Optional[str] = Field(default=None, alias="appName")
    cover: Optional[str] = Field(default=None, alias="cover")
    priority: Optional[int] = Field(default=None, alias="priority")

    class Config:
        populate_by_name = True


class AppDeployRequest(BaseModel):
    appId: int = Field(alias="appId")

    class Config:
        populate_by_name = True


class AppQueryRequest(PageRequest):
    id: Optional[int] = Field(default=None, alias="id")
    appName: Optional[str] = Field(default=None, alias="appName")
    cover: Optional[str] = Field(default=None, alias="cover")
    initPrompt: Optional[str] = Field(default=None, alias="initPrompt")
    codeGenType: Optional[str] = Field(default=None, alias="codeGenType")
    deployKey: Optional[str] = Field(default=None, alias="deployKey")
    priority: Optional[int] = Field(default=None, alias="priority")
    userId: Optional[int] = Field(default=None, alias="userId")

    class Config:
        populate_by_name = True


# --- Response VOs ---

class AppVO(BaseModel):
    id: int = Field(alias="id")
    appName: Optional[str] = Field(default=None, alias="appName")
    cover: Optional[str] = Field(default=None, alias="cover")
    initPrompt: Optional[str] = Field(default=None, alias="initPrompt")
    codeGenType: Optional[str] = Field(default=None, alias="codeGenType")
    deployKey: Optional[str] = Field(default=None, alias="deployKey")
    deployedTime: Optional[datetime] = Field(default=None, alias="deployedTime")
    priority: int = Field(default=0, alias="priority")
    userId: int = Field(alias="userId")
    createTime: Optional[datetime] = Field(default=None, alias="createTime")
    updateTime: Optional[datetime] = Field(default=None, alias="updateTime")
    user: Optional[UserVO] = Field(default=None, alias="user")

    class Config:
        populate_by_name = True
