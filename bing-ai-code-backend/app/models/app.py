from datetime import datetime
from sqlalchemy import BigInteger, Column, DateTime, String, Integer, Text
from app.database import Base


class App(Base):
    __tablename__ = "app"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    appName = Column(String(256), nullable=True)
    cover = Column(String(512), nullable=True)
    initPrompt = Column(Text, nullable=True)
    codeGenType = Column(String(64), nullable=True)
    deployKey = Column(String(64), nullable=True, unique=True)
    deployedTime = Column(DateTime, nullable=True)
    priority = Column(Integer, nullable=False, default=0)
    userId = Column(BigInteger, nullable=False)
    editTime = Column(DateTime, nullable=False, default=datetime.now)
    createTime = Column(DateTime, nullable=False, default=datetime.now)
    updateTime = Column(DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)
    isDelete = Column(Integer, nullable=False, default=0)
