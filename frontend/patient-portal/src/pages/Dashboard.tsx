import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { extractApiMessage, unwrapList } from '../api/client'
import api from '../api/client'
import { useAuth } from '../auth/AuthContext'
import {
  Alert,
  Badge,
  Card,
  EmptyState,
  LinkButton,
  Loading,
  PageHeader,
  StatCard,
  Table,
} from '../components/ui'
import type { Appointment, Invoice, Notification } from '../types'
import {
  appointmentDateValue,
  asNumber,
  asString,
  doctorName,
  firstDefined,
  formatDate,
  formatDateTime,
  formatMoney,
  statusTone,
  titleCase,
} from '../utils/format'

export default function Dashboard() {
  const { profile } = useAuth()
  const patientId = profile?.patientId
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!patientId) {
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    const results = await Promise.allSettled([
      api.get('/api/v1/appointments', { params: { patientId } }),
      api.get('/api/v1/billing/invoices', { params: { patientId } }),
      profile?.email
        ? api.get('/api/v1/notifications', { params: { recipient: profile.email } })
        : Promise.resolve({ data: [] }),
    ])
    const failures: string[] = []
    if (results[0].status === 'fulfilled') {
      setAppointments(unwrapList<Appointment>(results[0].value.data))
    } else {
      failures.push(extractApiMessage(results[0].reason))
    }
    if (results[1].status === 'fulfilled') {
      setInvoices(unwrapList<Invoice>(results[1].value.data))
    } else {
      failures.push(extractApiMessage(results[1].reason))
    }
    if (results[2].status === 'fulfilled') {
      setNotifications(unwrapList<Notification>(results[2].value.data))
    }
    setError(failures[0] || null)
    setLoading(false)
  }, [patientId, profile?.email])

  useEffect(() => {
    void load()
  }, [load])

  const upcoming = useMemo(
    () =>
      appointments
        .filter(
          (appointment) =>
            !['CANCELLED', 'COMPLETED', 'NO_SHOW'].includes(
              asString(appointment.status).toUpperCase(),
            ),
        )
        .sort((a, b) => {
          const left = new Date(String(appointmentDateValue(a) || 0)).getTime() || 0
          const right = new Date(String(appointmentDateValue(b) || 0)).getTime() || 0
          return left - right
        })
        .slice(0, 5),
    [appointments],
  )

  const outstanding = useMemo(
    () =>
      invoices
        .filter(
          (invoice) =>
            !['PAID', 'CANCELLED', 'VOID'].includes(asString(invoice.status).toUpperCase()),
        )
        .reduce(
          (total, invoice) =>
            total +
            asNumber(
              firstDefined(invoice.balance, invoice.patientShare, invoice.totalAmount, invoice.amount),
            ),
          0,
        ),
    [invoices],
  )

  const unread = notifications.filter(
    (item) => !item.read && asString(item.status).toUpperCase() !== 'READ',
  ).length

  const firstName = profile?.firstName || profile?.name?.split(' ')[0] || 'there'

  return (
    <>
      <PageHeader
        title={`Welcome back, ${firstName}`}
        description="Your upcoming visits, bills and updates at a glance."
        actions={
          <>
            <LinkButton to="/find-doctor" variant="primary">
              Find a doctor
            </LinkButton>
            <LinkButton to="/appointments" variant="secondary">
              My appointments
            </LinkButton>
          </>
        }
      />

      {!patientId && (
        <Alert tone="warning" title="Patient record not linked">
          Your sign-in is valid but no patient ID is stored yet. Open{' '}
          <Link to="/profile">your profile</Link> to link the record by email, or contact reception.
        </Alert>
      )}

      {error && <Alert tone="danger">{error}</Alert>}

      {loading ? (
        <Loading label="Loading your dashboard…" />
      ) : (
        <>
          <div className="stats-grid">
            <StatCard
              label="Upcoming visits"
              value={upcoming.length}
              hint="Scheduled or checked in"
              tone="info"
            />
            <StatCard
              label="Outstanding balance"
              value={formatMoney(outstanding)}
              hint={`${invoices.length} invoice${invoices.length === 1 ? '' : 's'} on file`}
              tone={outstanding > 0 ? 'warning' : 'success'}
            />
            <StatCard
              label="Unread updates"
              value={unread}
              hint="Notifications from the hospital"
              tone={unread > 0 ? 'info' : 'success'}
            />
            <StatCard
              label="Patient ID"
              value={
                patientId ? (
                  <span style={{ fontSize: '0.92rem' }} className="kbd">
                    {patientId.slice(0, 8)}…
                  </span>
                ) : (
                  'Not linked'
                )
              }
              hint={profile?.email}
              tone={patientId ? 'success' : 'danger'}
            />
          </div>

          <div className="two-col">
            <Card
              title="Next appointments"
              subtitle="The five nearest visits on your account."
              actions={<LinkButton to="/appointments">View all</LinkButton>}
            >
              <Table
                columns={[
                  {
                    header: 'Doctor',
                    cell: (appointment) =>
                      asString(appointment.doctorName) ||
                      doctorName(appointment.doctor as Record<string, unknown> | undefined),
                  },
                  {
                    header: 'When',
                    cell: (appointment) => formatDateTime(appointmentDateValue(appointment)),
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
                ]}
                rows={upcoming}
                rowKey={(appointment) =>
                  asString(appointment.id) || String(appointmentDateValue(appointment))
                }
                empty="You have no upcoming appointments. Find a doctor to book one."
                onRetry={() => void load()}
              />
            </Card>

            <Card title="Quick actions">
              <div className="quick-grid">
                <Link className="quick-link" to="/find-doctor">
                  Find a doctor
                  <span>Browse specialists and availability</span>
                </Link>
                <Link className="quick-link" to="/book">
                  Book an appointment
                  <span>Choose a time that works for you</span>
                </Link>
                <Link className="quick-link" to="/bills">
                  My bills
                  <span>Review invoices and receipts</span>
                </Link>
                <Link className="quick-link" to="/profile">
                  My profile
                  <span>Keep contact and insurance details current</span>
                </Link>
              </div>
            </Card>
          </div>

          <div style={{ height: 18 }} />

          <Card
            title="Notifications"
            subtitle="Appointment reminders, results and billing updates."
          >
            {notifications.length === 0 ? (
              <EmptyState message="No notifications right now." />
            ) : (
              <ul className="list-reset">
                {notifications.slice(0, 8).map((notification, index) => (
                  <li key={asString(notification.id) || index}>
                    <div>
                      <strong>
                        {asString(notification.title) ||
                          asString(notification.subject) ||
                          'Update'}
                      </strong>
                      <p className="muted small">
                        {asString(notification.message) || asString(notification.body)}
                      </p>
                    </div>
                    <div className="row" style={{ justifyContent: 'flex-end' }}>
                      <span className="muted small">
                        {formatDate(notification.createdAt)}
                      </span>
                      <Badge
                        tone={
                          notification.read ||
                          asString(notification.status).toUpperCase() === 'READ'
                            ? 'neutral'
                            : 'info'
                        }
                      >
                        {notification.read ? 'Read' : titleCase(notification.status || 'New')}
                      </Badge>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </>
      )}
    </>
  )
}
