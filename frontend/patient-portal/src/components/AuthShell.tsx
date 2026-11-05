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
      <section className="auth-hero" aria-label="About the patient portal">
        <div>
          <div className="brand">
            <span className="brand-mark" aria-hidden="true">
              +
            </span>
            <div>
              <strong>CarePoint</strong>
              <span>Patient Portal</span>
            </div>
          </div>
          <div style={{ marginTop: 46 }}>
            <h1>{heroTitle || 'Your care, in one calm place.'}</h1>
            <p>
              {heroText ||
                'Book appointments, find the right specialist, review your bills and keep your contact details current — securely.'}
            </p>
            <ul className="auth-points">
              <li>Book with live doctor availability and cancel when plans change.</li>
              <li>Download itemised receipts for every visit.</li>
              <li>Your data is protected and every access is audited.</li>
            </ul>
          </div>
        </div>
        <p className="auth-hero-foot">CarePoint Hospital Platform · Secure patient access</p>
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
