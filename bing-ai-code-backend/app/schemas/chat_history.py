from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field
from app.schemas.common import PageRequest


class ChatHistoryQueryRequest(PageRequest):
    id: Optional[int] = Field(default=None, alias="id")
    message: Optional[str] = Field(default=None, alias="message")
    messageType: Optional[str] = Field(default=None, alias="messageType")
    appId: Optional[int] = Field(default=None, alias="appId")
    userId: Optional[int] = Field(default=None, alias="userId")
    lastCreateTime: Optional[datetime] = Field(default=None, alias="lastCreateTime")

    class Config:
        populate_by_name = True


class ChatHistoryVO(BaseModel):
    id: int = Field(alias="id")
    message: str = Field(alias="message")
    messageType: str = Field(alias="messageType")
    appId: int = Field(alias="appId")
    userId: int = Field(alias="userId")
    createTime: Optional[datetime] = Field(default=None, alias="createTime")
    updateTime: Optional[datetime] = Field(default=None, alias="updateTime")

    class Config:
        populate_by_name = True
