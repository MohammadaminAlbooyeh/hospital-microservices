from __future__ import annotations

import uuid
from collections.abc import AsyncIterator
from datetime import datetime

from sqlalchemy import DateTime, Integer, String, Text, Uuid, func
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

from app.core.config import settings


class Base(DeclarativeBase):
    pass


class TriageRecord(Base):
    __tablename__ = "triage_records"

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True, default=uuid.uuid4)
    patient_id: Mapped[str] = mapped_column(String(64), index=True)
    encounter_id: Mapped[str | None] = mapped_column(String(64), nullable=True, index=True)
    chief_complaint: Mapped[str] = mapped_column(Text)
    esi_level: Mapped[int] = mapped_column(Integer, index=True)
    priority: Mapped[str] = mapped_column(String(32))
    recommended_action: Mapped[str] = mapped_column(String(255))
    vitals: Mapped[dict] = mapped_column(JSONB)
    rationale: Mapped[list] = mapped_column(JSONB)
    model_used: Mapped[str] = mapped_column(String(64), default="rules")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


engine = create_async_engine(settings.database_url, pool_pre_ping=True, future=True)
SessionLocal = async_sessionmaker(engine, expire_on_commit=False, class_=AsyncSession)


async def init_db() -> None:
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)


async def get_session() -> AsyncIterator[AsyncSession]:
    async with SessionLocal() as session:
        yield session
