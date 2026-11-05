"""create documents

Revision ID: 0001_document
Revises:
Create Date: 2026-01-01
"""

from alembic import op
import sqlalchemy as sa

revision = "0001_document"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "documents",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("doc_type", sa.String(length=32), nullable=False),
        sa.Column("reference", sa.String(length=128), nullable=True),
        sa.Column("filename", sa.String(length=255), nullable=False),
        sa.Column("content_type", sa.String(length=128), nullable=False),
        sa.Column("size", sa.BigInteger(), nullable=False),
        sa.Column("sha256", sa.String(length=64), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_documents_doc_type", "documents", ["doc_type"])
    op.create_index("ix_documents_reference", "documents", ["reference"])
    op.create_index("ix_documents_sha256", "documents", ["sha256"])
    op.create_index("ix_documents_created_at", "documents", ["created_at"])


def downgrade() -> None:
    op.drop_index("ix_documents_created_at", table_name="documents")
    op.drop_index("ix_documents_sha256", table_name="documents")
    op.drop_index("ix_documents_reference", table_name="documents")
    op.drop_index("ix_documents_doc_type", table_name="documents")
    op.drop_table("documents")
