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
      <section className="auth-hero" aria-label="About the staff console">
        <div>
          <div className="brand">
            <span className="brand-mark" aria-hidden="true">
              +
            </span>
            <div>
              <strong>CarePoint</strong>
              <span>Staff Console</span>
            </div>
          </div>
          <div style={{ marginTop: 46 }}>
            <h1>{heroTitle || 'Run your day, ward to ward.'}</h1>
            <p>
              {heroText ||
                'See today\'s schedule, check patients in, manage appointments and capture encounter notes — all in one place.'}
            </p>
            <ul className="auth-points">
              <li>Today’s appointments and check-in queue at a glance.</li>
              <li>Search and look up any patient on file.</li>
              <li>Record clinical notes tied to each encounter.</li>
            </ul>
          </div>
        </div>
        <p className="auth-hero-foot">CarePoint Hospital Platform · Staff access only</p>
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
