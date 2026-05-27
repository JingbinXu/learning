from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field
from app.schemas.common import PageRequest


# --- Request DTOs ---

class UserLoginRequest(BaseModel):
    userAccount: str = Field(alias="userAccount")
    userPassword: str = Field(alias="userPassword")

    class Config:
        populate_by_name = True


class UserRegisterRequest(BaseModel):
    userAccount: str = Field(alias="userAccount")
    userPassword: str = Field(alias="userPassword")
    checkPassword: str = Field(alias="checkPassword")

    class Config:
        populate_by_name = True


class UserAddRequest(BaseModel):
    userName: Optional[str] = Field(default=None, alias="userName")
    userAccount: str = Field(alias="userAccount")
    userAvatar: Optional[str] = Field(default=None, alias="userAvatar")
    userProfile: Optional[str] = Field(default=None, alias="userProfile")
    userRole: Optional[str] = Field(default="user", alias="userRole")

    class Config:
        populate_by_name = True


class UserUpdateRequest(BaseModel):
    id: int = Field(alias="id")
    userName: Optional[str] = Field(default=None, alias="userName")
    userAvatar: Optional[str] = Field(default=None, alias="userAvatar")
    userProfile: Optional[str] = Field(default=None, alias="userProfile")
    userRole: Optional[str] = Field(default=None, alias="userRole")

    class Config:
        populate_by_name = True


class UserQueryRequest(PageRequest):
    id: Optional[int] = Field(default=None, alias="id")
    userName: Optional[str] = Field(default=None, alias="userName")
    userAccount: Optional[str] = Field(default=None, alias="userAccount")
    userProfile: Optional[str] = Field(default=None, alias="userProfile")
    userRole: Optional[str] = Field(default=None, alias="userRole")

    class Config:
        populate_by_name = True


# --- Response VOs ---

class LoginUserVO(BaseModel):
    id: int = Field(alias="id")
    userAccount: str = Field(alias="userAccount")
    userName: Optional[str] = Field(default=None, alias="userName")
    userAvatar: Optional[str] = Field(default=None, alias="userAvatar")
    userProfile: Optional[str] = Field(default=None, alias="userProfile")
    userRole: str = Field(alias="userRole")
    createTime: Optional[datetime] = Field(default=None, alias="createTime")
    updateTime: Optional[datetime] = Field(default=None, alias="updateTime")

    class Config:
        populate_by_name = True


class UserVO(BaseModel):
    id: int = Field(alias="id")
    userAccount: str = Field(alias="userAccount")
    userName: Optional[str] = Field(default=None, alias="userName")
    userAvatar: Optional[str] = Field(default=None, alias="userAvatar")
    userProfile: Optional[str] = Field(default=None, alias="userProfile")
    userRole: str = Field(alias="userRole")
    createTime: Optional[datetime] = Field(default=None, alias="createTime")

    class Config:
        populate_by_name = True
