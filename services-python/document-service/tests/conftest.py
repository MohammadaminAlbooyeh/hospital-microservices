from __future__ import annotations

import sys
from pathlib import Path

shared = Path(__file__).resolve().parents[2] / "shared"
if shared.is_dir() and str(shared) not in sys.path:
    sys.path.insert(0, str(shared))
