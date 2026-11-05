import { useCallback, useEffect, useMemo, useState } from 'react'
import api, { extractApiMessage, unwrapList } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import {
  Alert,
  Badge,
  Button,
  Card,
  Loading,
  PageHeader,
  StatCard,
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

export default function Dashboard() {
  const { profile, hasRole } = useAuth()
  const isReceptionist = hasRole('RECEPTIONIST')
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [updatingId, setUpdatingId] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params: Record<string, unknown> = { date: todayISO() }
      if (profile?.doctorId && !isReceptionist) params.doctorId = profile.doctorId
      const response = await api.get('/api/v1/appointments', { params })
      setAppointments(unwrapList<Appointment>(response.data))
    } catch (err) {
      setError(extractApiMessage(err))
    } finally {
      setLoading(false)
    }
  }, [profile?.doctorId, isReceptionist])

  useEffect(() => {
    void load()
  }, [load])

  const today = useMemo(
    () =>
      appointments
        .slice()
        .sort((a, b) => {
          const left = new Date(String(appointmentDateValue(a) || 0)).getTime() || 0
          const right = new Date(String(appointmentDateValue(b) || 0)).getTime() || 0
          return left - right
        }),
    [appointments],
  )

  const waiting = today.filter((appointment) =>
    ['SCHEDULED', 'CHECKED_IN', 'WAITING'].includes(asString(appointment.status).toUpperCase()),
  )
  const checkedIn = today.filter(
    (appointment) => asString(appointment.status).toUpperCase() === 'CHECKED_IN',
  )
  const completed = today.filter(
    (appointment) => asString(appointment.status).toUpperCase() === 'COMPLETED',
  )

  const checkIn = async (appointment: Appointment) => {
    const id = asString(appointment.id)
    if (!id) return
    setUpdatingId(id)
    try {
      await api.patch(`/api/v1/appointments/${encodeURIComponent(id)}`, { status: 'CHECKED_IN' })
      await load()
    } catch (err) {
      setError(extractApiMessage(err))
    } finally {
      setUpdatingId(null)
    }
  }

  const firstName = profile?.firstName || profile?.name?.split(' ')[0] || 'there'

  return (
    <>
      <PageHeader
        title={`Welcome back, ${firstName}`}
        description={
          isReceptionist
            ? "Today's check-in queue across all doctors."
            : "Today's schedule for your patients."
        }
      />

      {error && <Alert tone="danger">{error}</Alert>}

      {loading ? (
        <Loading label="Loading today's schedule…" />
      ) : (
        <>
          <div className="stats-grid">
            <StatCard label="Scheduled today" value={today.length} tone="info" />
            <StatCard label="Waiting / scheduled" value={waiting.length} tone="warning" />
            <StatCard label="Checked in" value={checkedIn.length} tone="info" />
            <StatCard label="Completed" value={completed.length} tone="success" />
          </div>

          <Card
            title={isReceptionist ? 'Check-in queue' : "Today's appointments"}
            subtitle="Pulled from encounter-service / appointment records for today."
          >
            <Table
              columns={[
                {
                  header: 'Time',
                  cell: (appointment) => formatDateTime(appointmentDateValue(appointment)),
                },
                {
                  header: 'Patient',
                  cell: (appointment) =>
                    asString(appointment.patientName) ||
                    asString(appointment.patientId, 'Unknown patient'),
                },
                {
                  header: 'Doctor',
                  cell: (appointment) =>
                    asString(appointment.doctorName) ||
                    doctorName(appointment.doctor as Record<string, unknown> | undefined),
                },
                {
                  header: 'Reason',
                  cell: (appointment) => asString(appointment.reason, '—'),
                },
                {
                  header: 'Status',
                  cell: (appointment) => (
                    <Badge tone={statusTone(appointment.status)}>
                      {titleCase(appointment.status)}
                    </Badge>
                  ),
                },
                {
                  header: 'Action',
                  cell: (appointment) => {
                    const status = asString(appointment.status).toUpperCase()
                    const id = asString(appointment.id)
                    if (status === 'CHECKED_IN' || status === 'COMPLETED') return '—'
                    return (
                      <Button
                        variant="secondary"
                        loading={updatingId === id}
                        onClick={() => void checkIn(appointment)}
                      >
                        Check in
                      </Button>
                    )
                  },
                },
              ]}
              rows={today}
              rowKey={(appointment) =>
                asString(appointment.id) || String(appointmentDateValue(appointment))
              }
              empty="No appointments scheduled for today."
              onRetry={() => void load()}
            />
          </Card>
        </>
      )}
    </>
  )
}
