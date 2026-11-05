import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api, { extractApiMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { AuthShell } from '../components/AuthShell'
import { Alert, Button, Field, Input, Select } from '../components/ui'

interface RegisterForm {
  firstName: string
  lastName: string
  email: string
  password: string
  confirmPassword: string
  phone: string
  dateOfBirth: string
  gender: string
  nationalId: string
  addressLine: string
  city: string
  country: string
}

const initialForm: RegisterForm = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  confirmPassword: '',
  phone: '',
  dateOfBirth: '',
  gender: 'FEMALE',
  nationalId: '',
  addressLine: '',
  city: '',
  country: '',
}

export default function Register() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState<RegisterForm>(initialForm)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const update = (field: keyof RegisterForm, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    if (form.password.length < 8) {
      setError('Choose a password with at least 8 characters.')
      return
    }
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    setLoading(true)
    try {
      await api.post('/api/v1/auth/register', {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        password: form.password,
        phone: form.phone.trim(),
        dateOfBirth: form.dateOfBirth,
        gender: form.gender,
        nationalId: form.nationalId.trim(),
        addressLine: form.addressLine.trim(),
        city: form.city.trim(),
        country: form.country.trim(),
        role: 'PATIENT',
      })
      try {
        await login(form.email.trim(), form.password)
        navigate('/dashboard', { replace: true })
      } catch {
        navigate('/login', { replace: true, state: { registered: true } })
      }
    } catch (err) {
      setError(extractApiMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      title="Create your account"
      subtitle="Register once to book appointments and manage your hospital visits."
      footer={
        <>
          Already registered? <Link to="/login">Sign in</Link>
        </>
      }
      heroTitle="Care starts before you arrive."
      heroText="Create a secure patient account to book, prepare for visits and keep track of everything in one place."
    >
      {error && <Alert tone="danger">{error}</Alert>}
      <form onSubmit={handleSubmit} noValidate>
        <div className="form-grid">
          <Field label="First name" htmlFor="firstName" required>
            <Input
              id="firstName"
              autoComplete="given-name"
              required
              value={form.firstName}
              onChange={(event) => update('firstName', event.target.value)}
            />
          </Field>
          <Field label="Last name" htmlFor="lastName" required>
            <Input
              id="lastName"
              autoComplete="family-name"
              required
              value={form.lastName}
              onChange={(event) => update('lastName', event.target.value)}
            />
          </Field>
          <Field label="Date of birth" htmlFor="dateOfBirth" required>
            <Input
              id="dateOfBirth"
              type="date"
              required
              max={new Date().toISOString().slice(0, 10)}
              value={form.dateOfBirth}
              onChange={(event) => update('dateOfBirth', event.target.value)}
            />
          </Field>
          <Field label="Gender" htmlFor="gender" required>
            <Select
              id="gender"
              value={form.gender}
              onChange={(event) => update('gender', event.target.value)}
            >
              <option value="FEMALE">Female</option>
              <option value="MALE">Male</option>
              <option value="OTHER">Other</option>
              <option value="UNKNOWN">Prefer not to say</option>
            </Select>
          </Field>
          <Field label="Email address" htmlFor="email" required>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              required
              value={form.email}
              onChange={(event) => update('email', event.target.value)}
            />
          </Field>
          <Field label="Phone" htmlFor="phone" required>
            <Input
              id="phone"
              type="tel"
              autoComplete="tel"
              required
              value={form.phone}
              onChange={(event) => update('phone', event.target.value)}
            />
          </Field>
          <Field label="National ID" htmlFor="nationalId" hint="Used to match your hospital record.">
            <Input
              id="nationalId"
              value={form.nationalId}
              onChange={(event) => update('nationalId', event.target.value)}
            />
          </Field>
          <Field label="City" htmlFor="city">
            <Input
              id="city"
              autoComplete="address-level2"
              value={form.city}
              onChange={(event) => update('city', event.target.value)}
            />
          </Field>
          <div className="span-2">
            <Field label="Address" htmlFor="addressLine">
              <Input
                id="addressLine"
                autoComplete="street-address"
                value={form.addressLine}
                onChange={(event) => update('addressLine', event.target.value)}
              />
            </Field>
          </div>
          <Field label="Country" htmlFor="country">
            <Input
              id="country"
              autoComplete="country-name"
              value={form.country}
              onChange={(event) => update('country', event.target.value)}
            />
          </Field>
          <Field label="Password" htmlFor="password" required hint="At least 8 characters.">
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              required
              value={form.password}
              onChange={(event) => update('password', event.target.value)}
            />
          </Field>
          <Field label="Confirm password" htmlFor="confirmPassword" required>
            <Input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              required
              value={form.confirmPassword}
              onChange={(event) => update('confirmPassword', event.target.value)}
            />
          </Field>
        </div>
        <Button type="submit" loading={loading} className="full-width">
          Create account
        </Button>
      </form>
    </AuthShell>
  )
}
