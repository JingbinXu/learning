from datetime import datetime
from sqlalchemy import BigInteger, Column, DateTime, String, Integer
from app.database import Base


class User(Base):
    __tablename__ = "user"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    userAccount = Column(String(256), nullable=False, unique=True)
    userPassword = Column(String(512), nullable=False)
    userName = Column(String(256), nullable=True)
    userAvatar = Column(String(1024), nullable=True)
    userProfile = Column(String(512), nullable=True)
    userRole = Column(String(256), nullable=False, default="user")
    editTime = Column(DateTime, nullable=False, default=datetime.now)
    createTime = Column(DateTime, nullable=False, default=datetime.now)
    updateTime = Column(DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)
    isDelete = Column(Integer, nullable=False, default=0)
