"""create insurance policies and claims

Revision ID: 0001_insurance
Revises:
Create Date: 2026-01-01
"""

from alembic import op
import sqlalchemy as sa

revision = "0001_insurance"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "policies",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("policy_number", sa.String(length=64), nullable=False, unique=True),
        sa.Column("provider", sa.String(length=128), nullable=False),
        sa.Column("member_id", sa.String(length=64), nullable=False),
        sa.Column("patient_id", sa.String(length=64), nullable=True),
        sa.Column("coverage_percent", sa.Integer(), nullable=False),
        sa.Column("valid_from", sa.DateTime(timezone=True), nullable=False),
        sa.Column("valid_to", sa.DateTime(timezone=True), nullable=False),
        sa.Column("active", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_policies_member_id", "policies", ["member_id"])
    op.create_index("ix_policies_patient_id", "policies", ["patient_id"])

    op.create_table(
        "claims",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("claim_number", sa.String(length=64), nullable=False, unique=True),
        sa.Column("policy_number", sa.String(length=64), nullable=False),
        sa.Column("patient_id", sa.String(length=64), nullable=False),
        sa.Column("encounter_id", sa.String(length=64), nullable=True),
        sa.Column("invoice_id", sa.String(length=64), nullable=True, unique=True),
        sa.Column("amount", sa.Numeric(12, 2), nullable=False),
        sa.Column("status", sa.String(length=32), nullable=False, server_default="SUBMITTED"),
        sa.Column("approved_amount", sa.Numeric(12, 2), nullable=True),
        sa.Column("rejection_reason", sa.Text(), nullable=True),
        sa.Column("notes", sa.Text(), nullable=True),
        sa.Column("submitted_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_claims_policy_number", "claims", ["policy_number"])
    op.create_index("ix_claims_patient_id", "claims", ["patient_id"])
    op.create_index("ix_claims_encounter_id", "claims", ["encounter_id"])
    op.create_index("ix_claims_status", "claims", ["status"])


def downgrade() -> None:
    op.drop_index("ix_claims_status", table_name="claims")
    op.drop_index("ix_claims_encounter_id", table_name="claims")
    op.drop_index("ix_claims_patient_id", table_name="claims")
    op.drop_index("ix_claims_policy_number", table_name="claims")
    op.drop_table("claims")
    op.drop_index("ix_policies_patient_id", table_name="policies")
    op.drop_index("ix_policies_member_id", table_name="policies")
    op.drop_table("policies")
