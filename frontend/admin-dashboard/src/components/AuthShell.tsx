import type { ReactNode } from 'react'

export function AuthShell({
  title,
  subtitle,
  children,
  footer,
  heroTitle,
  heroText,
}: {
  title: string
  subtitle: string
  children: ReactNode
  footer?: ReactNode
  heroTitle?: string
  heroText?: string
}) {
  return (
    <div className="auth-page">
      <section className="auth-hero" aria-label="About the admin dashboard">
        <div>
          <div className="brand">
            <span className="brand-mark" aria-hidden="true">
              +
            </span>
            <div>
              <strong>CarePoint</strong>
              <span>Admin Dashboard</span>
            </div>
          </div>
          <div style={{ marginTop: 46 }}>
            <h1>{heroTitle || 'The hospital, at a glance.'}</h1>
            <p>
              {heroText ||
                'Track wait times, no-show rates, bed occupancy and revenue by department, and manage departments and doctors.'}
            </p>
            <ul className="auth-points">
              <li>Live management dashboards sourced from analytics-service.</li>
              <li>Add, edit and retire departments and doctors.</li>
              <li>Every access is audited.</li>
            </ul>
          </div>
        </div>
        <p className="auth-hero-foot">CarePoint Hospital Platform · Management access only</p>
      </section>
      <section className="auth-panel">
        <div className="auth-card">
          <h1>{title}</h1>
          <p className="muted">{subtitle}</p>
          {children}
          {footer && <div className="auth-alt">{footer}</div>}
          <p className="auth-note">
            Access is monitored. Never share your password or sign in on a public computer.
          </p>
        </div>
      </section>
    </div>
  )
}
