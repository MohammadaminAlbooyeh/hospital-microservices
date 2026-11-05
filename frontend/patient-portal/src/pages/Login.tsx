import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { extractApiMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { AuthShell } from '../components/AuthShell'
import { Alert, Button, Field, Input } from '../components/ui'

export default function Login() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const registered = Boolean((location.state as { registered?: boolean } | null)?.registered)

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  const from = (location.state as { from?: string } | null)?.from || '/dashboard'

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(email.trim(), password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(extractApiMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      title="Sign in"
      subtitle="Use the email address and password registered with the hospital."
      footer={
        <>
          New patient? <Link to="/register">Create an account</Link>
        </>
      }
    >
      {registered && <Alert tone="success">Registration complete. Sign in to continue.</Alert>}
      {error && <Alert tone="danger">{error}</Alert>}
      <form onSubmit={handleSubmit} noValidate>
        <Field label="Email address" htmlFor="email" required>
          <Input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="you@example.com"
          />
        </Field>
        <Field label="Password" htmlFor="password" required>
          <Input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Your password"
          />
        </Field>
        <Button type="submit" loading={loading} className="full-width">
          Sign in securely
        </Button>
      </form>
    </AuthShell>
  )
}
