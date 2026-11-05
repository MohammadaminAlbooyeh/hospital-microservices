from __future__ import annotations

from collections.abc import Mapping, Sequence
from datetime import date, datetime, timedelta, timezone
from decimal import Decimal, InvalidOperation
from io import BytesIO
from typing import Any
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    KeepTogether,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

HOSPITAL_NAME = "Central Hospital"
HOSPITAL_ADDRESS = "18 Health Sciences Avenue, Medical District"
HOSPITAL_CONTACT = "Tel: +1 (555) 010-2400  |  billing@centralhospital.example"
HOSPITAL_EMAIL = "records@centralhospital.example"

_PRIMARY = colors.HexColor("#123C69")
_ACCENT = colors.HexColor("#1F6FB2")
_LIGHT = colors.HexColor("#EAF2F8")
_MUTED = colors.HexColor("#5B6770")
_BORDER = colors.HexColor("#C9D4DD")

_PAGE_WIDTH, _PAGE_HEIGHT = A4
_MARGIN = 20 * mm
_CONTENT_WIDTH = _PAGE_WIDTH - (2 * _MARGIN)


def _get(data: Any, key: str, default: Any = None) -> Any:
    if isinstance(data, Mapping):
        return data.get(key, default)
    return getattr(data, key, default)


def _as_date(value: Any, fallback: date | None = None) -> date:
    if value is None:
        return fallback or date.today()
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    if isinstance(value, str):
        try:
            return datetime.fromisoformat(value.replace("Z", "+00:00")).date()
        except ValueError:
            return fallback or date.today()
    return fallback or date.today()


def _as_datetime(value: Any, fallback: datetime | None = None) -> datetime:
    if value is None:
        return fallback or datetime.now(timezone.utc)
    if isinstance(value, datetime):
        return value
    if isinstance(value, date):
        return datetime(value.year, value.month, value.day, tzinfo=timezone.utc)
    if isinstance(value, str):
        try:
            return datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError:
            return fallback or datetime.now(timezone.utc)
    return fallback or datetime.now(timezone.utc)


def _as_decimal(value: Any) -> Decimal:
    if isinstance(value, Decimal):
        return value
    if value is None:
        return Decimal("0")
    try:
        return Decimal(str(value))
    except (InvalidOperation, ValueError):
        return Decimal("0")


def _money(value: Any, currency: str = "USD") -> str:
    amount = _as_decimal(value)
    formatted = f"{amount:,.2f}"
    return f"{currency} {formatted}" if currency else formatted


def _quantity(value: Any) -> str:
    amount = _as_decimal(value)
    if amount == amount.to_integral_value():
        return str(int(amount))
    return f"{amount.normalize()}"


def _safe(text: Any) -> str:
    return escape(str(text if text is not None else ""))


def _lines(data: Any) -> list[Any]:
    raw = _get(data, "lines", []) or []
    if isinstance(raw, Sequence) and not isinstance(raw, (str, bytes, bytearray)):
        return list(raw)
    return []


def compute_subtotal(lines: Sequence[Any]) -> Decimal:
    subtotal = Decimal("0")
    for line in lines:
        subtotal += _as_decimal(_get(line, "quantity", 0)) * _as_decimal(_get(line, "unit_price", 0))
    return subtotal


def _styles() -> dict[str, ParagraphStyle]:
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "DocumentTitle",
            parent=base["Title"],
            fontName="Helvetica-Bold",
            fontSize=21,
            leading=25,
            textColor=_PRIMARY,
            alignment=TA_RIGHT,
        ),
        "subtitle": ParagraphStyle(
            "DocumentSubtitle",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=9,
            leading=12,
            textColor=_MUTED,
            alignment=TA_RIGHT,
        ),
        "hospital": ParagraphStyle(
            "HospitalName",
            parent=base["Heading1"],
            fontName="Helvetica-Bold",
            fontSize=16,
            leading=19,
            textColor=_PRIMARY,
            spaceAfter=2,
        ),
        "hospital_meta": ParagraphStyle(
            "HospitalMeta",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=8.5,
            leading=11,
            textColor=_MUTED,
        ),
        "section": ParagraphStyle(
            "Section",
            parent=base["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=10.5,
            leading=14,
            textColor=_PRIMARY,
            spaceBefore=10,
            spaceAfter=5,
        ),
        "body": ParagraphStyle(
            "Body",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=9.5,
            leading=13.5,
            textColor=colors.HexColor("#222B33"),
        ),
        "small": ParagraphStyle(
            "Small",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=8,
            leading=11,
            textColor=_MUTED,
        ),
        "table_head": ParagraphStyle(
            "TableHead",
            parent=base["Normal"],
            fontName="Helvetica-Bold",
            fontSize=8.5,
            leading=11,
            textColor=colors.white,
        ),
        "table_cell": ParagraphStyle(
            "TableCell",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=8.5,
            leading=11,
            textColor=colors.HexColor("#222B33"),
        ),
        "table_cell_right": ParagraphStyle(
            "TableCellRight",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=8.5,
            leading=11,
            textColor=colors.HexColor("#222B33"),
            alignment=TA_RIGHT,
        ),
        "total_label": ParagraphStyle(
            "TotalLabel",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=9,
            leading=12,
            textColor=_MUTED,
            alignment=TA_RIGHT,
        ),
        "total_value": ParagraphStyle(
            "TotalValue",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=9,
            leading=12,
            textColor=colors.HexColor("#222B33"),
            alignment=TA_RIGHT,
        ),
        "total_due_label": ParagraphStyle(
            "TotalDueLabel",
            parent=base["Normal"],
            fontName="Helvetica-Bold",
            fontSize=10,
            leading=13,
            textColor=_PRIMARY,
            alignment=TA_RIGHT,
        ),
        "total_due_value": ParagraphStyle(
            "TotalDueValue",
            parent=base["Normal"],
            fontName="Helvetica-Bold",
            fontSize=10,
            leading=13,
            textColor=_PRIMARY,
            alignment=TA_RIGHT,
        ),
        "signature": ParagraphStyle(
            "Signature",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=9.5,
            leading=13,
            textColor=colors.HexColor("#222B33"),
            alignment=TA_LEFT,
        ),
    }


def _paragraph(text: Any, style: ParagraphStyle) -> Paragraph:
    return Paragraph(_safe(text).replace("\n", "<br/>"), style)


def _draw_footer(canvas: Any, doc: Any) -> None:
    canvas.saveState()
    canvas.setStrokeColor(_BORDER)
    canvas.setLineWidth(0.6)
    canvas.line(_MARGIN, 16 * mm, _PAGE_WIDTH - _MARGIN, 16 * mm)
    canvas.setFont("Helvetica", 7.5)
    canvas.setFillColor(_MUTED)
    canvas.drawString(_MARGIN, 11 * mm, f"{HOSPITAL_NAME} | Confidential medical document")
    canvas.drawRightString(
        _PAGE_WIDTH - _MARGIN,
        11 * mm,
        f"Page {doc.page} | Generated {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}",
    )
    canvas.restoreState()


def _header(styles: dict[str, ParagraphStyle], title: str, subtitle: str) -> Table:
    left = [
        Paragraph(HOSPITAL_NAME, styles["hospital"]),
        Paragraph(HOSPITAL_ADDRESS, styles["hospital_meta"]),
        Paragraph(HOSPITAL_CONTACT, styles["hospital_meta"]),
    ]
    right = [
        Paragraph(title, styles["title"]),
        Paragraph(subtitle, styles["subtitle"]),
    ]
    table = Table([[left, right]], colWidths=[_CONTENT_WIDTH * 0.6, _CONTENT_WIDTH * 0.4])
    table.setStyle(
        TableStyle(
            [
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 0),
                ("RIGHTPADDING", (0, 0), (-1, -1), 0),
                ("TOPPADDING", (0, 0), (-1, -1), 0),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
                ("LINEBELOW", (0, 0), (-1, -1), 1.2, _PRIMARY),
            ]
        )
    )
    return table


def _build_pdf(story: list[Any]) -> bytes:
    buffer = BytesIO()
    document = SimpleDocTemplate(
        buffer,
        pagesize=A4,
        leftMargin=_MARGIN,
        rightMargin=_MARGIN,
        topMargin=_MARGIN,
        bottomMargin=22 * mm,
        title="Hospital Document",
        author=HOSPITAL_NAME,
    )
    document.build(story, onFirstPage=_draw_footer, onLaterPages=_draw_footer)
    return buffer.getvalue()


def generate_invoice_pdf(data: Any) -> bytes:
    styles = _styles()
    currency = str(_get(data, "currency", "USD") or "USD")
    issued_at = _as_datetime(_get(data, "issued_at"), datetime.now(timezone.utc))
    lines = _lines(data)
    subtotal = compute_subtotal(lines)
    insurer_share = _as_decimal(_get(data, "insurer_share", 0))
    patient_share = _as_decimal(_get(data, "patient_share", 0))

    story: list[Any] = [
        _header(styles, "INVOICE", f"Invoice {_get(data, 'invoice_number', '')}"),
        Spacer(1, 8),
    ]

    patient_block = [
        Paragraph("BILLED TO", styles["small"]),
        Paragraph(f"<b>{_safe(_get(data, 'patient_name', ''))}</b>", styles["body"]),
        Paragraph(f"Patient ID: {_safe(_get(data, 'patient_id', ''))}", styles["body"]),
    ]
    invoice_block = [
        Paragraph("INVOICE DETAILS", styles["small"]),
        Paragraph(f"Invoice number: <b>{_safe(_get(data, 'invoice_number', ''))}</b>", styles["body"]),
        Paragraph(f"Issued: {issued_at.strftime('%d %B %Y')}", styles["body"]),
        Paragraph(f"Currency: {_safe(currency)}", styles["body"]),
    ]
    info = Table([[patient_block, invoice_block]], colWidths=[_CONTENT_WIDTH * 0.5, _CONTENT_WIDTH * 0.5])
    info.setStyle(
        TableStyle(
            [
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 0),
                ("RIGHTPADDING", (0, 0), (-1, -1), 0),
                ("BACKGROUND", (0, 0), (-1, -1), _LIGHT),
                ("BOX", (0, 0), (-1, -1), 0.5, _BORDER),
                ("TOPPADDING", (0, 0), (-1, -1), 8),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
                ("LEFTPADDING", (0, 0), (0, -1), 8),
                ("RIGHTPADDING", (1, 0), (1, -1), 8),
            ]
        )
    )
    story.extend([info, Spacer(1, 10), Paragraph("SERVICES", styles["section"])])

    rows: list[list[Any]] = [
        [
            Paragraph("Description", styles["table_head"]),
            Paragraph("Qty", styles["table_head"]),
            Paragraph("Unit price", styles["table_head"]),
            Paragraph("Amount", styles["table_head"]),
        ]
    ]
    for line in lines:
        quantity = _as_decimal(_get(line, "quantity", 0))
        unit_price = _as_decimal(_get(line, "unit_price", 0))
        rows.append(
            [
                _paragraph(_get(line, "description", ""), styles["table_cell"]),
                Paragraph(_quantity(quantity), styles["table_cell_right"]),
                Paragraph(_money(unit_price, currency), styles["table_cell_right"]),
                Paragraph(_money(quantity * unit_price, currency), styles["table_cell_right"]),
            ]
        )
    if len(rows) == 1:
        rows.append(
            [
                Paragraph("No billable items supplied.", styles["table_cell"]),
                Paragraph("", styles["table_cell"]),
                Paragraph("", styles["table_cell"]),
                Paragraph("", styles["table_cell"]),
            ]
        )

    items = Table(rows, colWidths=[_CONTENT_WIDTH - 60 - 90 - 95, 60, 90, 95], repeatRows=1)
    items.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), _PRIMARY),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("GRID", (0, 0), (-1, -1), 0.4, _BORDER),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F7FAFC")]),
                ("TOPPADDING", (0, 0), (-1, -1), 6),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
            ]
        )
    )
    story.append(items)
    story.append(Spacer(1, 8))

    totals_data = [
        [Paragraph("Subtotal", styles["total_label"]), Paragraph(_money(subtotal, currency), styles["total_value"])],
        [
            Paragraph("Insurer share", styles["total_label"]),
            Paragraph(_money(insurer_share, currency), styles["total_value"]),
        ],
        [
            Paragraph("Patient share", styles["total_label"]),
            Paragraph(_money(patient_share, currency), styles["total_value"]),
        ],
        [
            Paragraph("Amount due", styles["total_due_label"]),
            Paragraph(_money(patient_share, currency), styles["total_due_value"]),
        ],
    ]
    totals = Table(totals_data, colWidths=[_CONTENT_WIDTH - 130, 130])
    totals.setStyle(
        TableStyle(
            [
                ("ALIGN", (1, 0), (1, -1), "RIGHT"),
                ("TOPPADDING", (0, 0), (-1, -1), 3),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
                ("LINEABOVE", (0, 2), (-1, 2), 0.5, _BORDER),
                ("LINEABOVE", (0, 3), (-1, 3), 0.8, _PRIMARY),
                ("BACKGROUND", (0, 3), (-1, 3), _LIGHT),
            ]
        )
    )
    story.append(KeepTogether(totals))

    story.extend(
        [
            Spacer(1, 18),
            Paragraph("PAYMENT INFORMATION", styles["section"]),
            Paragraph(
                "Please settle the patient balance by the due date shown on your statement. "
                "Quote the invoice number with every payment. Insurance claims are coordinated "
                "directly with the registered insurer.",
                styles["body"],
            ),
            Spacer(1, 12),
            Paragraph(
                "This is a computer-generated invoice and does not require a handwritten signature. "
                f"For billing enquiries contact {HOSPITAL_EMAIL}.",
                styles["small"],
            ),
        ]
    )
    return _build_pdf(story)


def generate_discharge_summary_pdf(data: Any) -> bytes:
    styles = _styles()
    admission = _as_date(_get(data, "admission_date"))
    discharge = _as_date(_get(data, "discharge_date"), admission)
    stay_days = max((discharge - admission).days, 0)
    medications = _get(data, "medications", []) or []
    if isinstance(medications, str):
        medications = [medications]

    story: list[Any] = [
        _header(styles, "DISCHARGE SUMMARY", f"Patient {_get(data, 'patient_id', '')}"),
        Spacer(1, 8),
    ]

    patient_rows = [
        [Paragraph("<b>Patient</b>", styles["body"]), Paragraph(_safe(_get(data, "patient_name", "")), styles["body"])],
        [Paragraph("<b>Patient ID</b>", styles["body"]), Paragraph(_safe(_get(data, "patient_id", "")), styles["body"])],
        [Paragraph("<b>Admission date</b>", styles["body"]), Paragraph(admission.strftime("%d %B %Y"), styles["body"])],
        [Paragraph("<b>Discharge date</b>", styles["body"]), Paragraph(discharge.strftime("%d %B %Y"), styles["body"])],
        [Paragraph("<b>Length of stay</b>", styles["body"]), Paragraph(f"{stay_days} day(s)", styles["body"])],
    ]
    if _get(data, "attending_physician"):
        patient_rows.append(
            [
                Paragraph("<b>Attending physician</b>", styles["body"]),
                Paragraph(_safe(_get(data, "attending_physician")), styles["body"]),
            ]
        )
    patient_table = Table(patient_rows, colWidths=[_CONTENT_WIDTH * 0.28, _CONTENT_WIDTH * 0.72])
    patient_table.setStyle(
        TableStyle(
            [
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("BACKGROUND", (0, 0), (0, -1), _LIGHT),
                ("GRID", (0, 0), (-1, -1), 0.4, _BORDER),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]
        )
    )
    story.append(patient_table)

    story.extend(
        [
            Paragraph("DIAGNOSIS", styles["section"]),
            _paragraph(_get(data, "diagnosis", ""), styles["body"]),
            Paragraph("MEDICATIONS AT DISCHARGE", styles["section"]),
        ]
    )
    medication_items = list(medications)
    if medication_items:
        for index, medication in enumerate(medication_items, start=1):
            story.append(Paragraph(f"{index}. {_safe(medication)}", styles["body"]))
    else:
        story.append(Paragraph("No medications were prescribed at discharge.", styles["body"]))

    story.extend(
        [
            Paragraph("FOLLOW-UP INSTRUCTIONS", styles["section"]),
            _paragraph(_get(data, "follow_up_instructions", ""), styles["body"]),
            Spacer(1, 22),
            Paragraph("DISCHARGE CONFIRMATION", styles["section"]),
            Paragraph(
                "The patient has been reviewed, is medically stable for discharge, and has received "
                "the instructions above. Please attend all follow-up appointments and seek urgent "
                "care if symptoms worsen.",
                styles["body"],
            ),
            Spacer(1, 28),
            Table(
                [
                    ["", ""],
                    [
                        Paragraph("Attending physician signature", styles["small"]),
                        Paragraph("Date", styles["small"]),
                    ],
                ],
                colWidths=[_CONTENT_WIDTH * 0.62, _CONTENT_WIDTH * 0.38],
                style=TableStyle(
                    [
                        ("LINEABOVE", (0, 0), (0, 0), 0.8, _PRIMARY),
                        ("LINEABOVE", (1, 0), (1, 0), 0.8, _PRIMARY),
                        ("TOPPADDING", (0, 0), (-1, 0), 22),
                        ("BOTTOMPADDING", (0, 1), (-1, 1), 2),
                    ]
                ),
            ),
        ]
    )
    return _build_pdf(story)


def _certificate_dates(data: Any) -> tuple[date, date | None, int]:
    rest_days = int(_get(data, "rest_days", 0) or 0)
    issue_date = _as_date(_get(data, "issue_date"))
    start = _as_date(_get(data, "start_date"), issue_date)
    raw_end = _get(data, "end_date")
    end = _as_date(raw_end) if raw_end is not None else None
    if end is None and start != issue_date:
        end = start + timedelta(days=max(rest_days - 1, 0))
    return start, end, rest_days


def render_medical_certificate_text(data: Any) -> str:
    start, end, rest_days = _certificate_dates(data)
    physician = _get(data, "physician", "") or ""
    period = f"from {start.strftime('%d %B %Y')}"
    if end is not None:
        period = f"from {start.strftime('%d %B %Y')} to {end.strftime('%d %B %Y')}"
    lines = [
        "MEDICAL CERTIFICATE",
        "",
        f"Date of issue: {_as_date(_get(data, 'issue_date')).strftime('%d %B %Y')}",
        "",
        f"This is to certify that {_get(data, 'patient_name', '')} "
        f"(patient ID: {_get(data, 'patient_id', '')}) was examined and diagnosed with "
        f"{_get(data, 'diagnosis', '')}.",
        "",
        f"The patient is advised to take {rest_days} day(s) of rest {period}.",
        "",
        f"Physician: {physician}",
        "",
        "Signature: ______________________________",
    ]
    return "\n".join(lines)


def generate_medical_certificate_pdf(data: Any) -> bytes:
    styles = _styles()
    start, end, rest_days = _certificate_dates(data)
    physician = str(_get(data, "physician", "") or "")
    issue_date = _as_date(_get(data, "issue_date"))
    period = f"from {start.strftime('%d %B %Y')}"
    if end is not None:
        period = f"from {start.strftime('%d %B %Y')} to {end.strftime('%d %B %Y')}"

    story: list[Any] = [
        _header(styles, "MEDICAL CERTIFICATE", f"Issued {issue_date.strftime('%d %B %Y')}"),
        Spacer(1, 18),
        Paragraph(
            f"<b>Patient:</b> {_safe(_get(data, 'patient_name', ''))} &nbsp;&nbsp; "
            f"<b>Patient ID:</b> {_safe(_get(data, 'patient_id', ''))}",
            styles["body"],
        ),
        Spacer(1, 18),
        Paragraph("TO WHOM IT MAY CONCERN", styles["section"]),
        Paragraph(
            f"This is to certify that <b>{_safe(_get(data, 'patient_name', ''))}</b> "
            f"(patient ID: {_safe(_get(data, 'patient_id', ''))}) was examined at {HOSPITAL_NAME} "
            f"and diagnosed with <b>{_safe(_get(data, 'diagnosis', ''))}</b>.",
            styles["body"],
        ),
        Spacer(1, 12),
        Paragraph(
            f"The patient is medically advised to take <b>{rest_days} day(s)</b> of rest "
            f"{_safe(period)}. During this period the patient should avoid strenuous activity "
            "and follow the treating physician's recommendations.",
            styles["body"],
        ),
        Spacer(1, 12),
        Paragraph(
            "This certificate is issued at the patient's request for medical, employment, or "
            "insurance purposes. It does not disclose information beyond what is necessary for "
            "the stated purpose.",
            styles["body"],
        ),
        Spacer(1, 42),
        Table(
            [
                ["", ""],
                [
                    Paragraph(f"{_safe(physician)}", styles["signature"]),
                    Paragraph(issue_date.strftime("%d %B %Y"), styles["signature"]),
                ],
                [
                    Paragraph("Treating physician", styles["small"]),
                    Paragraph("Date of issue", styles["small"]),
                ],
            ],
            colWidths=[_CONTENT_WIDTH * 0.62, _CONTENT_WIDTH * 0.38],
            style=TableStyle(
                [
                    ("LINEABOVE", (0, 0), (0, 0), 0.8, _PRIMARY),
                    ("LINEABOVE", (1, 0), (1, 0), 0.8, _PRIMARY),
                    ("TOPPADDING", (0, 0), (-1, 0), 24),
                    ("BOTTOMPADDING", (0, 1), (-1, 1), 2),
                ]
            ),
        ),
    ]
    return _build_pdf(story)
