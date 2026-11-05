import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import api, { extractApiMessage, unwrapList } from '../api/client'
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  Loading,
  PageHeader,
  Table,
} from '../components/ui'
import type { Patient } from '../types'
import { asString, formatDate, fullName } from '../utils/format'

export default function PatientSearch() {
  const [query, setQuery] = useState('')
  const [patients, setPatients] = useState<Patient[]>([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const search = useCallback(async (term: string) => {
    setLoading(true)
    setError(null)
    try {
      const response = await api.get('/api/v1/patients', {
        params: { search: term || undefined, q: term || undefined },
      })
      setPatients(unwrapList<Patient>(response.data))
    } catch (err) {
      setError(extractApiMessage(err))
      setPatients([])
    } finally {
      setLoading(false)
      setSearched(true)
    }
  }, [])

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void search(query.trim())
  }

  return (
    <>
      <PageHeader
        title="Patient search"
        description="Look up a patient by name, MRN, email or phone to view their record and encounter history."
      />

      <Card>
        <form className="toolbar" onSubmit={handleSubmit}>
          <Field label="Search" htmlFor="patient-search">
            <Input
              id="patient-search"
              placeholder="Name, MRN, email or phone"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </Field>
          <Button type="submit" loading={loading}>
            Search
          </Button>
        </form>
      </Card>

      <div style={{ height: 18 }} />

      {error && <Alert tone="danger">{error}</Alert>}

      {loading ? (
        <Loading label="Searching patients…" />
      ) : !searched ? (
        <EmptyState message="Search by name, MRN, email or phone to find a patient record." />
      ) : (
        <Card title="Results">
          <Table
            columns={[
              { header: 'Name', cell: (patient) => fullName(patient) },
              { header: 'MRN', cell: (patient) => asString(patient.mrn, '—') },
              { header: 'Date of birth', cell: (patient) => formatDate(patient.dateOfBirth) },
              { header: 'Phone', cell: (patient) => asString(patient.phone, '—') },
              {
                header: 'Actions',
                cell: (patient) => (
                  <Link className="btn btn-secondary" to={`/encounters/new?patientId=${encodeURIComponent(asString(patient.id))}`}>
                    New encounter
                  </Link>
                ),
              },
            ]}
            rows={patients}
            rowKey={(patient) => asString(patient.id) || fullName(patient)}
            empty="No patients match that search."
          />
        </Card>
      )}
    </>
  )
}
