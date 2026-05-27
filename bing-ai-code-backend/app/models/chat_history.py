from datetime import datetime
from sqlalchemy import BigInteger, Column, DateTime, String, Integer, Text, Index
from app.database import Base


class ChatHistory(Base):
    __tablename__ = "chat_history"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    message = Column(Text, nullable=False)
    messageType = Column(String(32), nullable=False)
    appId = Column(BigInteger, nullable=False)
    userId = Column(BigInteger, nullable=False)
    createTime = Column(DateTime, nullable=False, default=datetime.now)
    updateTime = Column(DateTime, nullable=False, default=datetime.now, onupdate=datetime.now)
    isDelete = Column(Integer, nullable=False, default=0)

    __table_args__ = (
        Index("idx_appId_createTime", "appId", "createTime"),
    )
