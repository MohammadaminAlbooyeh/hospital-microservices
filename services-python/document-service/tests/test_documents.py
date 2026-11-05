from __future__ import annotations

from io import BytesIO

import pytest

from app.domain.ocr import MAX_UPLOAD_BYTES, OcrValidationError, validate_upload
from app.domain.pdf_generator import (
    generate_discharge_summary_pdf,
    generate_invoice_pdf,
    render_medical_certificate_text,
)


def _invoice_payload() -> dict:
    return {
        "invoice_number": "INV-2026-0042",
        "patient_name": "Jane A. Doe",
        "patient_id": "PAT-10023",
        "issued_at": "2026-02-14T10:30:00Z",
        "currency": "USD",
        "lines": [
            {"description": "Consultation - cardiology", "quantity": 1, "unit_price": "250.00"},
            {"description": "Electrocardiogram (ECG)", "quantity": 2, "unit_price": "75.50"},
        ],
        "insurer_share": "240.00",
        "patient_share": "161.00",
    }


def _discharge_payload() -> dict:
    return {
        "patient_name": "Jane A. Doe",
        "patient_id": "PAT-10023",
        "admission_date": "2026-02-01",
        "discharge_date": "2026-02-04",
        "diagnosis": "Acute decompensated heart failure, improved",
        "medications": ["Furosemide 40 mg daily", "Lisinopril 10 mg daily"],
        "follow_up_instructions": "Cardiology review in 7 days. Daily weight monitoring.",
        "attending_physician": "Dr. Alan Rivers",
    }


def test_invoice_pdf_is_valid_and_non_trivial():
    pdf = generate_invoice_pdf(_invoice_payload())
    assert pdf.startswith(b"%PDF")
    assert len(pdf) > 1500


def test_discharge_summary_pdf_is_valid_and_non_trivial():
    pdf = generate_discharge_summary_pdf(_discharge_payload())
    assert pdf.startswith(b"%PDF")
    assert len(pdf) > 1500


def test_medical_certificate_text_contains_required_details():
    text = render_medical_certificate_text(
        {
            "patient_name": "Jane A. Doe",
            "patient_id": "PAT-10023",
            "diagnosis": "Acute bronchitis",
            "rest_days": 3,
            "physician": "Dr. Alan Rivers",
            "issue_date": "2026-03-01",
            "start_date": "2026-03-02",
        }
    )
    assert "MEDICAL CERTIFICATE" in text
    assert "Jane A. Doe" in text
    assert "Acute bronchitis" in text
    assert "3 day(s)" in text
    assert "Dr. Alan Rivers" in text
    assert "2026-03-02" in text
    assert "2026-03-04" in text


def test_ocr_rejects_oversized_upload():
    with pytest.raises(OcrValidationError, match="10 MB"):
        validate_upload("scan.png", "image/png", b"0" * (MAX_UPLOAD_BYTES + 1))


def test_ocr_rejects_wrong_content_type():
    with pytest.raises(OcrValidationError, match="Unsupported file type"):
        validate_upload("notes.txt", "text/plain", b"hello world")


def test_ocr_rejects_invalid_image_bytes():
    with pytest.raises(OcrValidationError, match="not a valid image"):
        validate_upload("scan.png", "image/png", b"not-really-a-png")


def test_ocr_accepts_a_real_image():
    from PIL import Image

    buffer = BytesIO()
    Image.new("RGB", (20, 20), "white").save(buffer, format="PNG")
    content_type = validate_upload("scan.png", "image/png", buffer.getvalue())
    assert content_type == "image/png"
