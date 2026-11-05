import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Badge, Button } from './ui'

export interface NavItem {
  to: string
  label: string
  roles?: string[]
}

export function Layout({
  nav,
  children,
  title,
  brand = 'CarePoint',
  subtitle = 'Patient Portal',
}: {
  nav: NavItem[]
  children: ReactNode
  title?: string
  brand?: string
  subtitle?: string
}) {
  const { profile, logout, hasAnyRole } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  useEffect(() => {
    setOpen(false)
  }, [location.pathname])

  const visibleNav = nav.filter(
    (item) => !item.roles?.length || hasAnyRole(item.roles),
  )

  const matched = visibleNav
    .filter((item) =>
      item.to === '/'
        ? location.pathname === '/'
        : location.pathname === item.to || location.pathname.startsWith(`${item.to}/`),
    )
    .sort((a, b) => b.to.length - a.to.length)[0]

  const pageTitle = title || matched?.label || ''

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  const displayName =
    profile?.name ||
    [profile?.firstName, profile?.lastName].filter(Boolean).join(' ') ||
    profile?.email ||
    'Signed in'

  return (
    <div className={`app-shell ${open ? 'nav-open' : ''}`}>
      <aside className="sidebar" aria-label="Main navigation">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            +
          </span>
          <div>
            <strong>{brand}</strong>
            <span>{subtitle}</span>
          </div>
        </div>
        <nav className="nav">
          {visibleNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              onClick={() => setOpen(false)}
            >
              <span className="nav-dot" aria-hidden="true" />
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="user-block">
            <span className="avatar" aria-hidden="true">
              {displayName.slice(0, 1).toUpperCase()}
            </span>
            <div>
              <strong>{displayName}</strong>
              <span className="muted small">{profile?.email || 'Authenticated session'}</span>
            </div>
          </div>
          <div className="role-row">
            {(profile?.roles || []).slice(0, 4).map((role) => (
              <Badge key={role} tone="info">
                {role}
              </Badge>
            ))}
          </div>
          <Button variant="secondary" className="full-width" onClick={handleLogout}>
            Sign out
          </Button>
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <button
            className="menu-toggle"
            type="button"
            aria-label={open ? 'Close navigation' : 'Open navigation'}
            aria-expanded={open}
            onClick={() => setOpen((value) => !value)}
          >
            <span />
            <span />
            <span />
          </button>
          <div className="topbar-title">{pageTitle}</div>
          <div className="topbar-user">
            <span className="muted small hide-mobile">{displayName}</span>
            <span className="avatar avatar-small" aria-hidden="true">
              {displayName.slice(0, 1).toUpperCase()}
            </span>
          </div>
        </header>
        <main className="content">{children}</main>
        <footer className="app-footer">
          <span>CarePoint Hospital Platform</span>
          <span className="muted small">All access is logged for patient-data protection.</span>
        </footer>
      </div>
      {open && (
        <button
          className="nav-scrim"
          aria-label="Close navigation"
          onClick={() => setOpen(false)}
        />
      )}
    </div>
  )
}
