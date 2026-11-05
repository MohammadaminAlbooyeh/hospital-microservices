from __future__ import annotations

from io import BytesIO
from typing import Any

MAX_UPLOAD_BYTES = 10 * 1024 * 1024

ALLOWED_CONTENT_TYPES = {
    "image/png",
    "image/jpeg",
    "image/jpg",
    "image/tiff",
    "image/bmp",
    "image/webp",
    "image/gif",
}


class OcrValidationError(ValueError):
    pass


class OcrUnavailableError(RuntimeError):
    pass


def normalise_content_type(content_type: str | None) -> str:
    return (content_type or "").split(";", 1)[0].strip().lower()


def validate_upload(filename: str | None, content_type: str | None, content: bytes) -> str:
    if not filename or not filename.strip():
        raise OcrValidationError("An uploaded file with a filename is required")
    if not content:
        raise OcrValidationError("The uploaded file is empty")
    if len(content) > MAX_UPLOAD_BYTES:
        raise OcrValidationError("File exceeds the maximum allowed size of 10 MB")
    normalised = normalise_content_type(content_type)
    if normalised not in ALLOWED_CONTENT_TYPES:
        raise OcrValidationError(
            "Unsupported file type. Upload a PNG, JPEG, TIFF, BMP, WEBP, or GIF image."
        )
    try:
        from PIL import Image

        with Image.open(BytesIO(content)) as image:
            image.verify()
    except Exception as exc:
        raise OcrValidationError("The uploaded file is not a valid image") from exc
    return normalised


def extract_text(content: bytes) -> str:
    try:
        import pytesseract
        from PIL import Image
    except ImportError as exc:
        raise OcrUnavailableError(
            "OCR is unavailable because the pytesseract/Pillow packages are not installed"
        ) from exc

    try:
        pytesseract.get_tesseract_version()
    except Exception as exc:
        raise OcrUnavailableError(
            "OCR is unavailable because the Tesseract executable is not installed on the server"
        ) from exc

    try:
        with Image.open(BytesIO(content)) as image:
            image.load()
            if image.mode not in ("RGB", "L"):
                image = image.convert("RGB")
            return pytesseract.image_to_string(image)
    except Exception as exc:
        raise OcrValidationError("The uploaded image could not be processed for OCR") from exc


def ocr_result(filename: str, content_type: str, size: int, text: str) -> dict[str, Any]:
    return {
        "filename": filename,
        "content_type": normalise_content_type(content_type),
        "size": size,
        "text": text.strip(),
        "character_count": len(text.strip()),
    }
