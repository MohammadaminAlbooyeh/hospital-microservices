from __future__ import annotations

import uuid
from collections.abc import AsyncIterator
from datetime import datetime
from decimal import Decimal

from sqlalchemy import DateTime, Float, Integer, Numeric, String, Uuid, func
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

from app.core.config import settings


class Base(DeclarativeBase):
    pass


class AppointmentFact(Base):
    __tablename__ = "appointment_facts"

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True, default=uuid.uuid4)
    event_id: Mapped[str] = mapped_column(String(128), unique=True, index=True)
    appointment_id: Mapped[str] = mapped_column(String(64), index=True)
    patient_id: Mapped[str] = mapped_column(String(64), index=True)
    doctor_id: Mapped[str] = mapped_column(String(64))
    department_id: Mapped[str | None] = mapped_column(String(64), nullable=True, index=True)
    slot_start: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
    booked_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    status: Mapped[str] = mapped_column(String(32), default="BOOKED", index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )


class EncounterFact(Base):
    __tablename__ = "encounter_facts"

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True, default=uuid.uuid4)
    event_id: Mapped[str] = mapped_column(String(128), unique=True, index=True)
    encounter_id: Mapped[str] = mapped_column(String(64), index=True)
    patient_id: Mapped[str] = mapped_column(String(64), index=True)
    department_id: Mapped[str | None] = mapped_column(String(64), nullable=True, index=True)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    wait_minutes: Mapped[float] = mapped_column(Float, default=0.0)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class InvoiceFact(Base):
    __tablename__ = "invoice_facts"

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True, default=uuid.uuid4)
    event_id: Mapped[str] = mapped_column(String(128), unique=True, index=True)
    invoice_id: Mapped[str] = mapped_column(String(64), index=True)
    patient_id: Mapped[str] = mapped_column(String(64), index=True)
    department_id: Mapped[str | None] = mapped_column(String(64), nullable=True, index=True)
    total_amount: Mapped[Decimal] = mapped_column(Numeric(14, 2))
    patient_share: Mapped[Decimal] = mapped_column(Numeric(14, 2))
    insurer_share: Mapped[Decimal] = mapped_column(Numeric(14, 2))
    currency: Mapped[str] = mapped_column(String(3), default="USD")
    issued_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
    paid_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    paid_amount: Mapped[Decimal | None] = mapped_column(Numeric(14, 2), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


engine = create_async_engine(settings.database_url, pool_pre_ping=True, future=True)
SessionLocal = async_sessionmaker(engine, expire_on_commit=False, class_=AsyncSession)


async def init_db() -> None:
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)


async def get_session() -> AsyncIterator[AsyncSession]:
    async with SessionLocal() as session:
        yield session
