import { useCallback, useEffect, useMemo, useState } from 'react'
import api, { extractApiMessage, unwrapObject } from '../api/client'
import { Alert, Card, Loading, PageHeader, StatCard } from '../components/ui'
import type { AnalyticsSummary } from '../types'
import { asNumber, formatMoney, formatNumber } from '../utils/format'

function Bar({ label, value, max, suffix = '' }: { label: string; value: number; max: number; suffix?: string }) {
  const pct = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0
  return (
    <div className="metric-line" style={{ flexDirection: 'column', alignItems: 'stretch', gap: 6 }}>
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <span>{label}</span>
        <strong>
          {formatNumber(value, suffix === '%' ? 1 : 0)}
          {suffix}
        </strong>
      </div>
      <div
        style={{
          height: 10,
          borderRadius: 6,
          background: 'var(--line)',
          overflow: 'hidden',
        }}
        role="img"
        aria-label={`${label}: ${value}${suffix}`}
      >
        <div
          style={{
            height: '100%',
            width: `${pct}%`,
            background: 'var(--accent)',
            borderRadius: 6,
          }}
        />
      </div>
    </div>
  )
}

export default function Overview() {
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.get('/api/v1/analytics/summary')
      setSummary(unwrapObject<AnalyticsSummary>(response.data))
    } catch (err) {
      setError(extractApiMessage(err))
      setSummary(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const waitTime = asNumber(summary?.averageWaitTimeMinutes ?? summary?.avgWaitTime)
  const noShowRate = asNumber(summary?.noShowRate) * (asNumber(summary?.noShowRate) <= 1 ? 100 : 1)
  const occupancy = asNumber(summary?.bedOccupancyRate) * (asNumber(summary?.bedOccupancyRate) <= 1 ? 100 : 1)
  const totalRevenue = asNumber(summary?.totalRevenue)

  const byDepartment = useMemo(() => {
    const list = summary?.revenueByDepartment || []
    return list
      .map((entry) => ({
        name: entry.department || entry.name || 'Unknown',
        revenue: asNumber(entry.revenue ?? entry.amount),
      }))
      .sort((a, b) => b.revenue - a.revenue)
  }, [summary])

  const maxDeptRevenue = Math.max(1, ...byDepartment.map((d) => d.revenue))

  return (
    <>
      <PageHeader
        title="Analytics overview"
        description="Average wait time, no-show rate, bed occupancy and revenue by department, from analytics-service."
      />

      {error && (
        <Alert tone="warning" title="Live data unavailable">
          {error} Showing the dashboard with whatever analytics-service returned (or nothing, if it is
          unreachable).
        </Alert>
      )}

      {loading ? (
        <Loading label="Loading analytics…" />
      ) : (
        <>
          <div className="stats-grid">
            <StatCard
              label="Average wait time"
              value={`${formatNumber(waitTime)} min`}
              hint="Across all departments, today"
              tone={waitTime > 30 ? 'warning' : 'success'}
            />
            <StatCard
              label="No-show rate"
              value={`${formatNumber(noShowRate, 1)}%`}
              hint="Trailing 30 days"
              tone={noShowRate > 15 ? 'danger' : 'success'}
            />
            <StatCard
              label="Bed occupancy"
              value={`${formatNumber(occupancy, 1)}%`}
              hint="Current snapshot"
              tone={occupancy > 85 ? 'warning' : 'info'}
            />
            <StatCard
              label="Total revenue"
              value={formatMoney(totalRevenue)}
              hint="Trailing 30 days"
              tone="success"
            />
          </div>

          <div className="two-col">
            <Card title="Wait time & no-show rate" subtitle="Chart placeholders wired to analytics-service.">
              <Bar label="Average wait time" value={waitTime} max={60} suffix=" min" />
              <div style={{ height: 12 }} />
              <Bar label="No-show rate" value={noShowRate} max={100} suffix="%" />
              <div style={{ height: 12 }} />
              <Bar label="Bed occupancy" value={occupancy} max={100} suffix="%" />
            </Card>

            <Card title="Revenue by department" subtitle="From analytics-service revenue breakdown.">
              {byDepartment.length === 0 ? (
                <p className="muted small">No department revenue data available.</p>
              ) : (
                byDepartment.map((dept) => (
                  <div key={dept.name} style={{ marginBottom: 12 }}>
                    <Bar label={dept.name} value={dept.revenue} max={maxDeptRevenue} />
                  </div>
                ))
              )}
            </Card>
          </div>
        </>
      )}
    </>
  )
}
