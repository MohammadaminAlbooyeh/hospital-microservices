import { useCallback, useEffect, useState } from 'react'
import api, { extractApiMessage, unwrapList } from '../api/client'
import {
  Alert,
  Badge,
  Button,
  Card,
  Field,
  Input,
  Loading,
  PageHeader,
  Table,
} from '../components/ui'
import type { Appointment } from '../types'
import {
  appointmentDateValue,
  asString,
  doctorName,
  formatDateTime,
  statusTone,
  titleCase,
  todayISO,
} from '../utils/format'

export default function Appointments() {
  const [date, setDate] = useState(todayISO())
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [updatingId, setUpdatingId] = useState<string | null>(null)

  const load = useCallback(async (forDate: string) => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.get('/api/v1/appointments', { params: { date: forDate } })
      setAppointments(unwrapList<Appointment>(response.data))
    } catch (err) {
      setError(extractApiMessage(err))
      setAppointments([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load(date)
  }, [date, load])

  const updateStatus = async (appointment: Appointment, status: string) => {
    const id = asString(appointment.id)
    if (!id) return
    setUpdatingId(id)
    try {
      await api.patch(`/api/v1/appointments/${encodeURIComponent(id)}`, { status })
      await load(date)
    } catch (err) {
      setError(extractApiMessage(err))
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <>
      <PageHeader
        title="Appointment management"
        description="View, check in, reschedule or cancel appointments for a given day."
      />

      <Card>
        <div className="toolbar">
          <Field label="Date" htmlFor="appt-date">
            <Input
              id="appt-date"
              type="date"
              value={date}
              onChange={(event) => setDate(event.target.value)}
            />
          </Field>
        </div>
      </Card>

      <div style={{ height: 18 }} />

      {error && <Alert tone="danger">{error}</Alert>}

      {loading ? (
        <Loading label="Loading appointments…" />
      ) : (
        <Card title={`Appointments on ${date}`}>
          <Table
            columns={[
              { header: 'Time', cell: (a) => formatDateTime(appointmentDateValue(a)) },
              {
                header: 'Patient',
                cell: (a) => asString(a.patientName) || asString(a.patientId, '—'),
              },
              {
                header: 'Doctor',
                cell: (a) =>
                  asString(a.doctorName) || doctorName(a.doctor as Record<string, unknown> | undefined),
              },
              { header: 'Reason', cell: (a) => asString(a.reason, '—') },
              {
                header: 'Status',
                cell: (a) => <Badge tone={statusTone(a.status)}>{titleCase(a.status)}</Badge>,
              },
              {
                header: 'Actions',
                cell: (appointment) => {
                  const id = asString(appointment.id)
                  const status = asString(appointment.status).toUpperCase()
                  const busy = updatingId === id
                  return (
                    <div className="row" style={{ gap: 6 }}>
                      {status !== 'CHECKED_IN' && status !== 'COMPLETED' && status !== 'CANCELLED' && (
                        <Button
                          variant="secondary"
                          loading={busy}
                          onClick={() => void updateStatus(appointment, 'CHECKED_IN')}
                        >
                          Check in
                        </Button>
                      )}
                      {status !== 'CANCELLED' && status !== 'COMPLETED' && (
                        <Button
                          variant="danger"
                          loading={busy}
                          onClick={() => void updateStatus(appointment, 'CANCELLED')}
                        >
                          Cancel
                        </Button>
                      )}
                    </div>
                  )
                },
              },
            ]}
            rows={appointments}
            rowKey={(a) => asString(a.id) || String(appointmentDateValue(a))}
            empty="No appointments found for this day."
            onRetry={() => void load(date)}
          />
        </Card>
      )}
    </>
  )
}
