import type { Tone } from '../components/ui'

export function asString(value: unknown, fallback = ''): string {
  if (value === null || value === undefined) return fallback
  return String(value)
}

export function asNumber(value: unknown): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/[^0-9.-]/g, ''))
    return Number.isFinite(parsed) ? parsed : 0
  }
  return 0
}

export function firstDefined<T>(...values: T[]): T | undefined {
  return values.find((value) => value !== undefined && value !== null && value !== '')
}

export function formatDate(value: unknown): string {
  if (!value) return '—'
  const date = new Date(String(value))
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export function formatDateTime(value: unknown): string {
  if (!value) return '—'
  const date = new Date(String(value))
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatTime(value: unknown): string {
  if (!value) return '—'
  if (typeof value === 'string' && /^\d{2}:\d{2}/.test(value)) return value.slice(0, 5)
  const date = new Date(String(value))
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
}

export function formatMoney(value: unknown, currency = 'USD'): string {
  const amount = asNumber(value)
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount)
  } catch {
    return `${amount.toFixed(2)} ${currency}`
  }
}

export function formatNumber(value: unknown, digits = 0): string {
  const number = asNumber(value)
  return number.toLocaleString(undefined, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })
}

export function titleCase(value: unknown): string {
  const text = asString(value)
  if (!text) return '—'
  return text
    .replace(/[_-]+/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase())
}

export function fullName(person: Record<string, unknown> | null | undefined): string {
  if (!person) return '—'
  const explicit = asString(person.name || person.fullName || person.displayName)
  if (explicit) return explicit
  const combined = [person.firstName, person.lastName].filter(Boolean).join(' ')
  return combined || asString(person.email, '—')
}

export function initials(value: string): string {
  return value
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
}

export function todayISO(offsetDays = 0): string {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  return date.toISOString().slice(0, 10)
}

export function statusTone(status: unknown): Tone {
  const normalised = asString(status).toUpperCase()
  if (['ACTIVE', 'PAID', 'COMPLETED', 'CONFIRMED', 'AVAILABLE', 'SUCCESS', 'DISPENSED', 'VERIFIED'].includes(normalised)) {
    return 'success'
  }
  if (['PENDING', 'SCHEDULED', 'CHECKED_IN', 'WAITING', 'IN_PROGRESS', 'PARTIAL', 'LOW'].includes(normalised)) {
    return 'warning'
  }
  if (['CANCELLED', 'NO_SHOW', 'FAILED', 'OVERDUE', 'REJECTED', 'OUT_OF_STOCK', 'CRITICAL', 'EXPIRED'].includes(normalised)) {
    return 'danger'
  }
  if (['INACTIVE', 'CLOSED', 'DISCHARGED'].includes(normalised)) return 'neutral'
  return 'info'
}

export function appointmentDateValue(appointment: Record<string, unknown>): unknown {
  return firstDefined(
    appointment.scheduledAt,
    appointment.startTime,
    appointment.appointmentDate,
    appointment.dateTime,
    appointment.date,
    appointment.slot?.toString?.(),
  )
}

export function doctorName(doctor: Record<string, unknown> | null | undefined): string {
  if (!doctor) return '—'
  const explicit = asString(doctor.name || doctor.fullName)
  if (explicit) return explicit
  const combined = [doctor.firstName, doctor.lastName].filter(Boolean).join(' ')
  return combined || asString(doctor.doctorName, 'Doctor')
}
