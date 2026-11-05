import { useState } from 'react'
import type { FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import api, { extractApiMessage, unwrapObject } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import {
  Alert,
  Button,
  Card,
  Field,
  Input,
  LinkButton,
  PageHeader,
  Textarea,
} from '../components/ui'
import type { Encounter as EncounterRecord } from '../types'

export default function Encounter() {
  const { profile } = useAuth()
  const [searchParams] = useSearchParams()
  const [patientId, setPatientId] = useState(searchParams.get('patientId') || '')
  const [appointmentId, setAppointmentId] = useState(searchParams.get('appointmentId') || '')
  const [chiefComplaint, setChiefComplaint] = useState('')
  const [notes, setNotes] = useState('')
  const [diagnosis, setDiagnosis] = useState('')
  const [treatmentPlan, setTreatmentPlan] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState<EncounterRecord | null>(null)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    if (!patientId.trim()) {
      setError('Enter the patient ID for this encounter.')
      return
    }
    if (!chiefComplaint.trim()) {
      setError('Record the chief complaint or reason for the visit.')
      return
    }
    setSubmitting(true)
    try {
      const payload = {
        patientId: patientId.trim(),
        appointmentId: appointmentId.trim() || undefined,
        doctorId: profile?.doctorId,
        chiefComplaint: chiefComplaint.trim(),
        notes: notes.trim(),
        diagnosis: diagnosis.trim(),
        treatmentPlan: treatmentPlan.trim(),
        status: 'COMPLETED',
      }
      // Posts against encounter-service / ehr-service via the gateway. The backend
      // does not need to be running for this page to be exercised in isolation.
      const response = await api.post('/api/v1/encounters', payload)
      setSaved(unwrapObject<EncounterRecord>(response.data))
    } catch (err) {
      setError(extractApiMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (saved) {
    return (
      <>
        <PageHeader title="Encounter saved" description="The clinical note has been recorded." />
        <Card title="Encounter summary">
          <Alert tone="success">Encounter saved for patient {patientId}.</Alert>
          <dl className="detail-grid">
            <div className="detail-item">
              <dt>Reference</dt>
              <dd>{saved.id || '—'}</dd>
            </div>
            <div className="detail-item">
              <dt>Diagnosis</dt>
              <dd>{diagnosis || '—'}</dd>
            </div>
          </dl>
          <div className="form-actions">
            <LinkButton to="/patients" variant="primary">
              Back to patient search
            </LinkButton>
            <LinkButton to="/appointments">Back to appointments</LinkButton>
          </div>
        </Card>
      </>
    )
  }

  return (
    <>
      <PageHeader
        title="Encounter note"
        description="Capture a clinical note for this visit. Submitted against encounter-service / ehr-service through the gateway."
      />

      {error && <Alert tone="danger">{error}</Alert>}

      <Card title="New encounter">
        <form onSubmit={handleSubmit} noValidate>
          <Field label="Patient ID" htmlFor="patientId" required>
            <Input
              id="patientId"
              value={patientId}
              onChange={(event) => setPatientId(event.target.value)}
              required
            />
          </Field>
          <Field label="Appointment ID" htmlFor="appointmentId" hint="Optional — links the note to a booked visit.">
            <Input
              id="appointmentId"
              value={appointmentId}
              onChange={(event) => setAppointmentId(event.target.value)}
            />
          </Field>
          <Field label="Chief complaint" htmlFor="chiefComplaint" required>
            <Input
              id="chiefComplaint"
              value={chiefComplaint}
              onChange={(event) => setChiefComplaint(event.target.value)}
              placeholder="Reason for today's visit"
              required
            />
          </Field>
          <Field label="Clinical notes" htmlFor="notes">
            <Textarea
              id="notes"
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              placeholder="Examination findings, vitals, observations"
            />
          </Field>
          <Field label="Diagnosis" htmlFor="diagnosis">
            <Input
              id="diagnosis"
              value={diagnosis}
              onChange={(event) => setDiagnosis(event.target.value)}
            />
          </Field>
          <Field label="Treatment plan" htmlFor="treatmentPlan">
            <Textarea
              id="treatmentPlan"
              value={treatmentPlan}
              onChange={(event) => setTreatmentPlan(event.target.value)}
              placeholder="Prescriptions, follow-up, referrals"
            />
          </Field>
          <div className="form-actions">
            <Button type="submit" loading={submitting}>
              Save encounter
            </Button>
          </div>
        </form>
      </Card>
    </>
  )
}
