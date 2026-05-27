from typing import Generic, TypeVar, Optional
from pydantic import BaseModel, Field

T = TypeVar("T")


class BaseResponse(BaseModel, Generic[T]):
    code: int = 0
    data: Optional[T] = None
    message: str = ""

    class Config:
        populate_by_name = True


class PageRequest(BaseModel):
    pageNum: int = Field(default=1, alias="pageNum")
    pageSize: int = Field(default=10, alias="pageSize")
    sortField: Optional[str] = Field(default=None, alias="sortField")
    sortOrder: str = Field(default="descend", alias="sortOrder")

    class Config:
        populate_by_name = True


class DeleteRequest(BaseModel):
    id: int = Field(alias="id")

    class Config:
        populate_by_name = True


class PageResult(BaseModel, Generic[T]):
    records: list[T]
    total: int
    size: int
    current: int
    pages: int = 0

    class Config:
        populate_by_name = True
