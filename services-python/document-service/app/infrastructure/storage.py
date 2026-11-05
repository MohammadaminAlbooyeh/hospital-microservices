from __future__ import annotations

import hashlib
import os
import uuid
from pathlib import Path

from app.core.config import settings


class DocumentStorage:
    def __init__(self, base_dir: str | Path) -> None:
        self._base_dir = Path(base_dir)

    @property
    def base_dir(self) -> Path:
        return self._base_dir

    def ensure_ready(self) -> None:
        self._base_dir.mkdir(parents=True, exist_ok=True)

    def filename_for(self, document_id: uuid.UUID) -> str:
        return f"{document_id}.pdf"

    def path_for(self, filename: str) -> Path:
        safe_name = Path(filename).name
        return self._base_dir / safe_name

    def save(self, document_id: uuid.UUID, content: bytes) -> str:
        self.ensure_ready()
        filename = self.filename_for(document_id)
        destination = self.path_for(filename)
        temporary = destination.with_suffix(".pdf.tmp")
        try:
            temporary.write_bytes(content)
            os.replace(temporary, destination)
        finally:
            temporary.unlink(missing_ok=True)
        return filename

    def exists(self, filename: str) -> bool:
        return self.path_for(filename).is_file()

    def read(self, filename: str) -> bytes:
        return self.path_for(filename).read_bytes()

    def delete(self, filename: str) -> None:
        self.path_for(filename).unlink(missing_ok=True)

    @staticmethod
    def sha256(content: bytes) -> str:
        return hashlib.sha256(content).hexdigest()


storage = DocumentStorage(settings.upload_dir)
