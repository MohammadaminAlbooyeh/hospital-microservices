"""create triage_records

Revision ID: 0001_triage
Revises:
Create Date: 2026-01-01
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "0001_triage"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "triage_records",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("patient_id", sa.String(length=64), nullable=False, index=True),
        sa.Column("encounter_id", sa.String(length=64), nullable=True, index=True),
        sa.Column("chief_complaint", sa.Text(), nullable=False),
        sa.Column("esi_level", sa.Integer(), nullable=False, index=True),
        sa.Column("priority", sa.String(length=32), nullable=False),
        sa.Column("recommended_action", sa.String(length=255), nullable=False),
        sa.Column("vitals", postgresql.JSONB(), nullable=False),
        sa.Column("rationale", postgresql.JSONB(), nullable=False),
        sa.Column("model_used", sa.String(length=64), nullable=False, server_default="rules"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )


def downgrade() -> None:
    op.drop_table("triage_records")
