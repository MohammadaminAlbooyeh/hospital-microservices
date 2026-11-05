import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import api, { extractApiMessage, unwrapList } from '../api/client'
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  LinkButton,
  Loading,
  PageHeader,
  Select,
} from '../components/ui'
import type { Doctor } from '../types'
import { asString, doctorName, initials } from '../utils/format'

export default function FindDoctor() {
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [search, setSearch] = useState('')
  const [specialty, setSpecialty] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async (params?: { search?: string; specialty?: string }) => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.get('/api/v1/doctors', {
        params: {
          search: params?.search || undefined,
          q: params?.search || undefined,
          specialty: params?.specialty || undefined,
        },
      })
      setDoctors(unwrapList<Doctor>(response.data))
    } catch (err) {
      setError(extractApiMessage(err))
      setDoctors([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const specialities = useMemo(() => {
    const values = new Set<string>()
    doctors.forEach((doctor) => {
      const value = asString(doctor.specialty || doctor.speciality)
      if (value) values.add(value)
    })
    return Array.from(values).sort()
  }, [doctors])

  const visible = useMemo(() => {
    const term = search.trim().toLowerCase()
    return doctors.filter((doctor) => {
      const matchesSpecialty =
        !specialty ||
        asString(doctor.specialty || doctor.speciality)
          .toLowerCase()
          .includes(specialty.toLowerCase())
      const haystack = [
        doctorName(doctor),
        doctor.specialty,
        doctor.speciality,
        doctor.department,
        doctor.departmentName,
        doctor.bio,
      ]
        .map((value) => asString(value).toLowerCase())
        .join(' ')
      return matchesSpecialty && (!term || haystack.includes(term))
    })
  }, [doctors, search, specialty])

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void load({ search, specialty })
  }

  const reset = () => {
    setSearch('')
    setSpecialty('')
    void load()
  }

  return (
    <>
      <PageHeader
        title="Find a doctor"
        description="Browse our specialists and choose the right care team for your needs."
      />

      <Card>
        <form className="toolbar" onSubmit={handleSubmit}>
          <Field label="Search" htmlFor="doctor-search">
            <Input
              id="doctor-search"
              placeholder="Name, specialty or department"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </Field>
          <Field label="Specialty" htmlFor="doctor-specialty">
            <Select
              id="doctor-specialty"
              value={specialty}
              onChange={(event) => setSpecialty(event.target.value)}
            >
              <option value="">All specialties</option>
              {specialities.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </Select>
          </Field>
          <Button type="submit" loading={loading}>
            Search
          </Button>
          <Button type="button" variant="ghost" onClick={reset}>
            Clear
          </Button>
        </form>
      </Card>

      <div style={{ height: 18 }} />

      {error && <Alert tone="danger">{error}</Alert>}

      {loading ? (
        <Loading label="Loading doctors…" />
      ) : visible.length === 0 ? (
        <EmptyState
          message="No doctors match your filters. Try a different search or clear the filters."
          action={
            <Button variant="secondary" onClick={reset}>
              Clear filters
            </Button>
          }
        />
      ) : (
        <div className="doctor-grid">
          {visible.map((doctor, index) => {
            const name = doctorName(doctor)
            const id = asString(doctor.id) || String(index)
            return (
              <article className="doctor-card" key={id}>
                <div className="doctor-card-head">
                  <span className="doctor-avatar" aria-hidden="true">
                    {initials(name) || 'DR'}
                  </span>
                  <div>
                    <h3>{name}</h3>
                    <p className="muted small">
                      {asString(doctor.specialty || doctor.speciality, 'General practice')}
                    </p>
                  </div>
                </div>
                <div className="row" style={{ gap: 6 }}>
                  {asString(doctor.department || doctor.departmentName) && (
                    <span className="badge badge-info">
                      {asString(doctor.department || doctor.departmentName)}
                    </span>
                  )}
                  {doctor.yearsExperience !== undefined && (
                    <span className="badge badge-neutral">
                      {asString(doctor.yearsExperience)} yrs experience
                    </span>
                  )}
                </div>
                <p className="bio">
                  {asString(
                    doctor.bio,
                    'Book a consultation to discuss your symptoms and care plan with this clinician.',
                  )}
                </p>
                <div className="doctor-card-foot">
                  <span className="muted small">{asString(doctor.phone)}</span>
                  <LinkButton to={`/book?doctorId=${encodeURIComponent(id)}`} variant="primary">
                    Book appointment
                  </LinkButton>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </>
  )
}
