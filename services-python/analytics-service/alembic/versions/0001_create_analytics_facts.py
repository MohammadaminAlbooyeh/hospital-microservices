"""create analytics fact tables

Revision ID: 0001_analytics
Revises:
Create Date: 2026-01-01
"""

from alembic import op
import sqlalchemy as sa

revision = "0001_analytics"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "appointment_facts",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("event_id", sa.String(length=128), nullable=False),
        sa.Column("appointment_id", sa.String(length=64), nullable=False),
        sa.Column("patient_id", sa.String(length=64), nullable=False),
        sa.Column("doctor_id", sa.String(length=64), nullable=False),
        sa.Column("department_id", sa.String(length=64), nullable=True),
        sa.Column("slot_start", sa.DateTime(timezone=True), nullable=False),
        sa.Column("booked_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("status", sa.String(length=32), nullable=False, server_default="BOOKED"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_index("ix_appointment_facts_event_id", "appointment_facts", ["event_id"], unique=True)
    op.create_index("ix_appointment_facts_appointment_id", "appointment_facts", ["appointment_id"])
    op.create_index("ix_appointment_facts_patient_id", "appointment_facts", ["patient_id"])
    op.create_index("ix_appointment_facts_department_id", "appointment_facts", ["department_id"])
    op.create_index("ix_appointment_facts_slot_start", "appointment_facts", ["slot_start"])
    op.create_index("ix_appointment_facts_status", "appointment_facts", ["status"])

    op.create_table(
        "encounter_facts",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("event_id", sa.String(length=128), nullable=False),
        sa.Column("encounter_id", sa.String(length=64), nullable=False),
        sa.Column("patient_id", sa.String(length=64), nullable=False),
        sa.Column("department_id", sa.String(length=64), nullable=True),
        sa.Column("started_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("completed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("wait_minutes", sa.Float(), nullable=False, server_default="0"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_index("ix_encounter_facts_event_id", "encounter_facts", ["event_id"], unique=True)
    op.create_index("ix_encounter_facts_encounter_id", "encounter_facts", ["encounter_id"])
    op.create_index("ix_encounter_facts_patient_id", "encounter_facts", ["patient_id"])
    op.create_index("ix_encounter_facts_department_id", "encounter_facts", ["department_id"])
    op.create_index("ix_encounter_facts_started_at", "encounter_facts", ["started_at"])

    op.create_table(
        "invoice_facts",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("event_id", sa.String(length=128), nullable=False),
        sa.Column("invoice_id", sa.String(length=64), nullable=False),
        sa.Column("patient_id", sa.String(length=64), nullable=False),
        sa.Column("department_id", sa.String(length=64), nullable=True),
        sa.Column("total_amount", sa.Numeric(precision=14, scale=2), nullable=False),
        sa.Column("patient_share", sa.Numeric(precision=14, scale=2), nullable=False),
        sa.Column("insurer_share", sa.Numeric(precision=14, scale=2), nullable=False),
        sa.Column("currency", sa.String(length=3), nullable=False, server_default="USD"),
        sa.Column("issued_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("paid_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("paid_amount", sa.Numeric(precision=14, scale=2), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_index("ix_invoice_facts_event_id", "invoice_facts", ["event_id"], unique=True)
    op.create_index("ix_invoice_facts_invoice_id", "invoice_facts", ["invoice_id"])
    op.create_index("ix_invoice_facts_patient_id", "invoice_facts", ["patient_id"])
    op.create_index("ix_invoice_facts_department_id", "invoice_facts", ["department_id"])
    op.create_index("ix_invoice_facts_issued_at", "invoice_facts", ["issued_at"])


def downgrade() -> None:
    op.drop_table("invoice_facts")
    op.drop_table("encounter_facts")
    op.drop_table("appointment_facts")
