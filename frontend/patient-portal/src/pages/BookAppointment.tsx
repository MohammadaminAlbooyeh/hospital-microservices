import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api, { extractApiMessage, unwrapList, unwrapObject } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  LinkButton,
  Loading,
  PageHeader,
  Select,
  Textarea,
} from '../components/ui'
import type { AvailabilitySlot, Doctor } from '../types'
import { asString, doctorName, formatDate, formatTime, todayISO } from '../utils/format'

function normaliseSlots(data: unknown): AvailabilitySlot[] {
  const object = data as Record<string, unknown> | null
  const candidates: unknown[] = [
    data,
    object?.slots,
    object?.availableSlots,
    object?.available_slots,
    object?.availability,
    object?.data,
  ]
  let list: unknown[] = []
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) {
      list = candidate
      break
    }
  }
  if (!list.length) list = unwrapList<unknown>(data)

  const seen = new Set<string>()
  const result: AvailabilitySlot[] = []
  list.forEach((item) => {
    if (typeof item === 'string') {
      if (!item || seen.has(item)) return
      seen.add(item)
      result.push({ value: item, label: formatTime(item), available: true, raw: { startTime: item } })
      return
    }
    if (!item || typeof item !== 'object') return
    const raw = item as Record<string, unknown>
    const valueRaw =
      raw.slotId ??
      raw.id ??
      raw.startTime ??
      raw.start_time ??
      raw.time ??
      raw.dateTime ??
      raw.start ??
      raw.slot
    const value = asString(valueRaw)
    if (!value || seen.has(value)) return
    seen.add(value)
    const status = asString(raw.status).toUpperCase()
    const available =
      raw.available !== false &&
      !['BOOKED', 'UNAVAILABLE', 'FULL', 'CANCELLED', 'CLOSED'].includes(status)
    result.push({
      value,
      label: formatTime(raw.label ?? raw.startTime ?? raw.start_time ?? raw.time ?? value),
      available,
      raw,
    })
  })
  return result.sort((a, b) => a.value.localeCompare(b.value))
}

interface BookingResult {
  id?: string
  appointmentDate?: string
  startTime?: string
  scheduledAt?: string
  status?: string
  [key: string]: unknown
}

export default function BookAppointment() {
  const { profile } = useAuth()
  const patientId = profile?.patientId
  const [searchParams] = useSearchParams()
  const initialDoctorId = searchParams.get('doctorId') || ''

  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [doctorId, setDoctorId] = useState(initialDoctorId)
  const [date, setDate] = useState(todayISO(1))
  const [slots, setSlots] = useState<AvailabilitySlot[]>([])
  const [slotId, setSlotId] = useState('')
  const [reason, setReason] = useState('')
  const [loadingDoctors, setLoadingDoctors] = useState(true)
  const [loadingSlots, setLoadingSlots] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [slotError, setSlotError] = useState<string | null>(null)
  const [result, setResult] = useState<BookingResult | null>(null)

  useEffect(() => {
    let active = true
    setLoadingDoctors(true)
    api
      .get('/api/v1/doctors')
      .then((response) => {
        if (!active) return
        const list = unwrapList<Doctor>(response.data)
        setDoctors(list)
        if (!initialDoctorId && list.length === 1) setDoctorId(asString(list[0].id))
      })
      .catch((err) => {
        if (active) setError(extractApiMessage(err))
      })
      .finally(() => {
        if (active) setLoadingDoctors(false)
      })
    return () => {
      active = false
    }
  }, [initialDoctorId])

  useEffect(() => {
    if (!doctorId || !date) {
      setSlots([])
      setSlotId('')
      return
    }
    let active = true
    setLoadingSlots(true)
    setSlotError(null)
    setSlotId('')
    api
      .get(`/api/v1/doctors/${encodeURIComponent(doctorId)}/availability`, { params: { date } })
      .then((response) => {
        if (!active) return
        setSlots(normaliseSlots(response.data))
      })
      .catch((err) => {
        if (!active) return
        setSlots([])
        setSlotError(extractApiMessage(err))
      })
      .finally(() => {
        if (active) setLoadingSlots(false)
      })
    return () => {
      active = false
    }
  }, [doctorId, date])

  const selectedDoctor = useMemo(
    () => doctors.find((doctor) => asString(doctor.id) === doctorId),
    [doctors, doctorId],
  )
  const selectedSlot = slots.find((slot) => slot.value === slotId)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    if (!patientId) {
      setError('Your patient record is not linked yet. Update your profile before booking.')
      return
    }
    if (!doctorId) {
      setError('Choose a doctor.')
      return
    }
    if (!selectedSlot) {
      setError('Choose an available time slot.')
      return
    }
    if (reason.trim().length < 3) {
      setError('Tell the care team the reason for your visit.')
      return
    }
    setSubmitting(true)
    try {
      const raw = selectedSlot.raw
      const startValue = asString(raw.startTime || raw.start_time || selectedSlot.value)
      const payload: Record<string, unknown> = {
        patientId,
        doctorId,
        appointmentDate: date,
        reason: reason.trim(),
        startTime: startValue,
        scheduledAt: startValue,
      }
      const slotKey = raw.slotId ?? raw.id
      if (slotKey) payload.slotId = slotKey
      const response = await api.post('/api/v1/appointments', payload)
      setResult(unwrapObject<BookingResult>(response.data))
      setReason('')
      setSelectedSlotReset()
    } catch (err) {
      setError(extractApiMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  const setSelectedSlotReset = () => {
    setSlotId('')
    setSlots((current) => current.map((slot) => ({ ...slot, available: slot.available })))
  }

  if (result) {
    return (
      <>
        <PageHeader title="Appointment requested" description="Your booking has been recorded." />
        <Card title="Booking confirmation">
          <Alert tone="success" title="You're booked">
            {formatDate(result.appointmentDate || date)} at{' '}
            {formatTime(result.startTime || result.scheduledAt || selectedSlot?.value)} with{' '}
            {doctorName(selectedDoctor as Record<string, unknown> | undefined)}.
          </Alert>
          <dl className="detail-grid">
            <div className="detail-item">
              <dt>Reference</dt>
              <dd>{asString(result.id, '—')}</dd>
            </div>
            <div className="detail-item">
              <dt>Status</dt>
              <dd>{asString(result.status, 'SCHEDULED')}</dd>
            </div>
            <div className="detail-item">
              <dt>Reason</dt>
              <dd>{reason || '—'}</dd>
            </div>
          </dl>
          <div className="form-actions">
            <LinkButton to="/appointments" variant="primary">
              View my appointments
            </LinkButton>
            <LinkButton to="/find-doctor">Find another doctor</LinkButton>
          </div>
        </Card>
      </>
    )
  }

  return (
    <>
      <PageHeader
        title="Book an appointment"
        description="Pick a doctor, choose an available time and tell us why you're coming."
      />

      {!patientId && (
        <Alert tone="warning" title="Link your patient record first">
          We could not find your patient ID. Visit <Link to="/profile">your profile</Link> to link it
          by email, then return to booking.
        </Alert>
      )}
      {error && <Alert tone="danger">{error}</Alert>}

      <div className="two-col">
        <Card title="Appointment details">
          {loadingDoctors ? (
            <Loading label="Loading doctors…" />
          ) : (
            <form onSubmit={handleSubmit} noValidate>
              <Field label="Doctor" htmlFor="doctor" required>
                <Select
                  id="doctor"
                  value={doctorId}
                  onChange={(event) => setDoctorId(event.target.value)}
                  required
                >
                  <option value="">Select a doctor</option>
                  {doctors.map((doctor, index) => {
                    const doctorValue = asString(doctor.id)
                    if (!doctorValue) return null
                    return (
                      <option key={doctorValue || index} value={doctorValue}>
                        {doctorName(doctor)}
                        {doctor.specialty || doctor.speciality
                          ? ` — ${asString(doctor.specialty || doctor.speciality)}`
                          : ''}
                      </option>
                    )
                  })}
                </Select>
              </Field>

              <Field label="Preferred date" htmlFor="date" required>
                <Input
                  id="date"
                  type="date"
                  min={todayISO()}
                  value={date}
                  onChange={(event) => setDate(event.target.value)}
                  required
                />
              </Field>

              <Field
                label="Available times"
                htmlFor="slots"
                required
                error={slotError || undefined}
                hint={
                  doctorId
                    ? 'Availability is shown for the selected date.'
                    : 'Select a doctor to load availability.'
                }
              >
                <div id="slots" aria-live="polite">
                  {loadingSlots ? (
                    <Loading label="Checking availability…" />
                  ) : !doctorId ? (
                    <p className="muted small">Select a doctor first.</p>
                  ) : slots.filter((slot) => slot.available).length === 0 ? (
                    <EmptyState message="No open slots for this date. Try another day." />
                  ) : (
                    <div className="slot-grid" role="list">
                      {slots
                        .filter((slot) => slot.available)
                        .map((slot) => (
                          <button
                            type="button"
                            key={slot.value}
                            className={`slot ${slotId === slot.value ? 'selected' : ''}`}
                            aria-pressed={slotId === slot.value}
                            onClick={() => setSlotId(slot.value)}
                          >
                            {slot.label}
                          </button>
                        ))}
                    </div>
                  )}
                </div>
              </Field>

              <Field
                label="Reason for visit"
                htmlFor="reason"
                required
                hint="Symptoms, a follow-up reference, or the service you need."
              >
                <Textarea
                  id="reason"
                  required
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  placeholder="For example: recurring headaches for two weeks"
                />
              </Field>

              <div className="form-actions">
                <Button type="submit" loading={submitting} disabled={!patientId}>
                  Confirm booking
                </Button>
                <LinkButton to="/find-doctor">Back to doctors</LinkButton>
              </div>
            </form>
          )}
        </Card>

        <Card title="Your selection">
          {selectedDoctor ? (
            <div className="stack">
              <div>
                <h3>{doctorName(selectedDoctor)}</h3>
                <p className="muted small">
                  {asString(selectedDoctor.specialty || selectedDoctor.speciality, 'General practice')}
                </p>
                {asString(selectedDoctor.department || selectedDoctor.departmentName) && (
                  <p className="muted small">
                    {asString(selectedDoctor.department || selectedDoctor.departmentName)}
                  </p>
                )}
              </div>
              <div className="metric-line">
                <span>Date</span>
                <strong>{formatDate(date)}</strong>
              </div>
              <div className="metric-line">
                <span>Time</span>
                <strong>{selectedSlot ? selectedSlot.label : 'Not selected'}</strong>
              </div>
              <div className="metric-line">
                <span>Patient</span>
                <strong>{profile?.name || profile?.email || '—'}</strong>
              </div>
              {selectedDoctor.bio && <p className="muted small">{asString(selectedDoctor.bio)}</p>}
            </div>
          ) : (
            <p className="muted small">
              Choose a doctor and a time slot to see a summary before you confirm.
            </p>
          )}
        </Card>
      </div>
    </>
  )
}
