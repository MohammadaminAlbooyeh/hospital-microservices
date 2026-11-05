import { LinkButton } from '../components/ui'

export default function NotFound() {
  return (
    <div className="state" style={{ minHeight: '50vh' }}>
      <h1>Page not found</h1>
      <p className="muted">The page you were looking for does not exist or has moved.</p>
      <LinkButton to="/dashboard" variant="primary">
        Back to dashboard
      </LinkButton>
    </div>
  )
}
