from __future__ import annotations

import re
import uuid
from pathlib import Path

from fastapi import APIRouter, Depends, File, HTTPException, Query, UploadFile, status
from fastapi.responses import FileResponse, Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.v1.schemas import (
    DischargeSummaryRequest,
    DocumentResponse,
    InvoiceRequest,
    MedicalCertificateRequest,
    OcrResponse,
)
from app.core.config import settings
from app.domain.ocr import (
    OcrUnavailableError,
    OcrValidationError,
    extract_text,
    ocr_result,
    validate_upload,
)
from app.domain.pdf_generator import (
    generate_discharge_summary_pdf,
    generate_invoice_pdf,
    generate_medical_certificate_pdf,
)
from app.infrastructure.db import Document, get_session
from app.infrastructure.storage import storage
from hospital_shared.errors import NotFoundError

router = APIRouter(prefix="/api/v1/documents", tags=["documents"])

_CONTENT_TYPE = "application/pdf"


def _safe_filename(doc_type: str, reference: str | None, document_id: uuid.UUID) -> str:
    raw = f"{doc_type.lower()}-{reference or document_id}"
    cleaned = re.sub(r"[^A-Za-z0-9._-]+", "-", raw).strip("-._")
    return f"{cleaned or document_id}.pdf"


def _pdf_response(content: bytes, document_id: uuid.UUID, filename: str) -> Response:
    return Response(
        content=content,
        media_type=_CONTENT_TYPE,
        headers={
            "Content-Disposition": f'attachment; filename="{filename}"',
            "X-Document-Id": str(document_id),
            "Content-Length": str(len(content)),
        },
    )


async def _store_document(
    session: AsyncSession,
    *,
    document_id: uuid.UUID,
    doc_type: str,
    reference: str | None,
    content: bytes,
) -> Document:
    filename = storage.save(document_id, content)
    record = Document(
        id=document_id,
        doc_type=doc_type,
        reference=reference,
        filename=filename,
        content_type=_CONTENT_TYPE,
        size=len(content),
        sha256=storage.sha256(content),
    )
    session.add(record)
    try:
        await session.commit()
        await session.refresh(record)
    except Exception:
        await session.rollback()
        storage.delete(filename)
        raise
    return record


@router.post("/invoices", status_code=status.HTTP_201_CREATED)
async def create_invoice(
    payload: InvoiceRequest, session: AsyncSession = Depends(get_session)
) -> Response:
    document_id = uuid.uuid4()
    content = generate_invoice_pdf(payload)
    await _store_document(
        session,
        document_id=document_id,
        doc_type="INVOICE",
        reference=payload.invoice_number,
        content=content,
    )
    return _pdf_response(
        content, document_id, _safe_filename("invoice", payload.invoice_number, document_id)
    )


@router.post("/discharge-summaries", status_code=status.HTTP_201_CREATED)
async def create_discharge_summary(
    payload: DischargeSummaryRequest, session: AsyncSession = Depends(get_session)
) -> Response:
    document_id = uuid.uuid4()
    content = generate_discharge_summary_pdf(payload)
    reference = payload.encounter_id or payload.patient_id
    await _store_document(
        session,
        document_id=document_id,
        doc_type="DISCHARGE_SUMMARY",
        reference=reference,
        content=content,
    )
    return _pdf_response(
        content,
        document_id,
        _safe_filename("discharge-summary", reference, document_id),
    )


@router.post("/medical-certificates", status_code=status.HTTP_201_CREATED)
async def create_medical_certificate(
    payload: MedicalCertificateRequest, session: AsyncSession = Depends(get_session)
) -> Response:
    document_id = uuid.uuid4()
    content = generate_medical_certificate_pdf(payload)
    await _store_document(
        session,
        document_id=document_id,
        doc_type="MEDICAL_CERTIFICATE",
        reference=payload.patient_id,
        content=content,
    )
    return _pdf_response(
        content,
        document_id,
        _safe_filename("medical-certificate", payload.patient_id, document_id),
    )


@router.post("/ocr", response_model=OcrResponse)
async def ocr_document(file: UploadFile = File(...)) -> OcrResponse:
    content = await file.read(settings.max_upload_bytes + 1)
    try:
        content_type = validate_upload(file.filename, file.content_type, content)
        text = extract_text(content)
    except OcrValidationError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except OcrUnavailableError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc

    filename = Path(file.filename or "upload").name
    return OcrResponse(**ocr_result(filename, content_type, len(content), text))


@router.get("/{document_id}/download")
async def download_document(
    document_id: uuid.UUID, session: AsyncSession = Depends(get_session)
) -> FileResponse:
    record = await session.get(Document, document_id)
    if record is None:
        raise NotFoundError(f"Document {document_id} not found")
    path = storage.path_for(record.filename)
    if not path.is_file():
        raise NotFoundError(f"Stored file for document {document_id} is not available")
    return FileResponse(
        path=path,
        media_type=record.content_type,
        filename=record.filename,
        headers={"X-Document-Id": str(document_id)},
    )


@router.get("/{document_id}", response_model=None)
async def get_document(
    document_id: uuid.UUID,
    download: bool = Query(default=False),
    session: AsyncSession = Depends(get_session),
):
    record = await session.get(Document, document_id)
    if record is None:
        raise NotFoundError(f"Document {document_id} not found")

    if download:
        path = storage.path_for(record.filename)
        if not path.is_file():
            raise NotFoundError(f"Stored file for document {document_id} is not available")
        return FileResponse(
            path=path,
            media_type=record.content_type,
            filename=record.filename,
            headers={"X-Document-Id": str(document_id)},
        )

    return DocumentResponse(
        id=record.id,
        doc_type=record.doc_type,
        reference=record.reference,
        filename=record.filename,
        content_type=record.content_type,
        size=record.size,
        sha256=record.sha256,
        created_at=record.created_at,
        stored=storage.exists(record.filename),
        download_url=f"/api/v1/documents/{record.id}?download=true",
    )
