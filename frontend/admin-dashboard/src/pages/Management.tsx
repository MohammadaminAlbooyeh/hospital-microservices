import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import api, { extractApiMessage, unwrapList } from '../api/client'
import {
  Alert,
  Badge,
  Button,
  Card,
  Field,
  Input,
  Loading,
  PageHeader,
  Table,
} from '../components/ui'
import type { Department, Doctor } from '../types'
import { asString, doctorName } from '../utils/format'

export default function Management() {
  const [departments, setDepartments] = useState<Department[]>([])
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [newDeptName, setNewDeptName] = useState('')
  const [newDeptDescription, setNewDeptDescription] = useState('')
  const [creating, setCreating] = useState(false)
  const [busyId, setBusyId] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    const results = await Promise.allSettled([
      api.get('/api/v1/departments'),
      api.get('/api/v1/doctors'),
    ])
    const failures: string[] = []
    if (results[0].status === 'fulfilled') {
      setDepartments(unwrapList<Department>(results[0].value.data))
    } else {
      failures.push(extractApiMessage(results[0].reason))
    }
    if (results[1].status === 'fulfilled') {
      setDoctors(unwrapList<Doctor>(results[1].value.data))
    } else {
      failures.push(extractApiMessage(results[1].reason))
    }
    setError(failures[0] || null)
    setLoading(false)
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const createDepartment = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!newDeptName.trim()) return
    setCreating(true)
    try {
      await api.post('/api/v1/departments', {
        name: newDeptName.trim(),
        description: newDeptDescription.trim(),
      })
      setNewDeptName('')
      setNewDeptDescription('')
      await load()
    } catch (err) {
      setError(extractApiMessage(err))
    } finally {
      setCreating(false)
    }
  }

  const toggleDoctorActive = async (doctor: Doctor) => {
    const id = asString(doctor.id)
    if (!id) return
    setBusyId(id)
    try {
      await api.patch(`/api/v1/doctors/${encodeURIComponent(id)}`, { active: !doctor.active })
      await load()
    } catch (err) {
      setError(extractApiMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  const removeDepartment = async (department: Department) => {
    const id = asString(department.id)
    if (!id) return
    setBusyId(id)
    try {
      await api.delete(`/api/v1/departments/${encodeURIComponent(id)}`)
      await load()
    } catch (err) {
      setError(extractApiMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <PageHeader
        title="Departments & doctors"
        description="Manage hospital departments and doctor records via department-service and doctor-service."
      />

      {error && <Alert tone="danger">{error}</Alert>}

      {loading ? (
        <Loading label="Loading departments and doctors…" />
      ) : (
        <>
          <Card
            title="Departments"
            subtitle="Add a department or remove one that is no longer active."
          >
            <form className="toolbar" onSubmit={createDepartment} style={{ marginBottom: 16 }}>
              <Field label="Name" htmlFor="dept-name" required>
                <Input
                  id="dept-name"
                  value={newDeptName}
                  onChange={(event) => setNewDeptName(event.target.value)}
                  placeholder="e.g. Cardiology"
                  required
                />
              </Field>
              <Field label="Description" htmlFor="dept-description">
                <Input
                  id="dept-description"
                  value={newDeptDescription}
                  onChange={(event) => setNewDeptDescription(event.target.value)}
                  placeholder="Optional"
                />
              </Field>
              <Button type="submit" loading={creating}>
                Add department
              </Button>
            </form>

            <Table
              columns={[
                { header: 'Name', cell: (d) => asString(d.name, '—') },
                { header: 'Description', cell: (d) => asString(d.description, '—') },
                { header: 'Staff', cell: (d) => asString(d.staffCount, '—') },
                {
                  header: 'Actions',
                  cell: (department) => (
                    <Button
                      variant="danger"
                      loading={busyId === asString(department.id)}
                      onClick={() => void removeDepartment(department)}
                    >
                      Remove
                    </Button>
                  ),
                },
              ]}
              rows={departments}
              rowKey={(d) => asString(d.id) || asString(d.name)}
              empty="No departments on file yet."
            />
          </Card>

          <div style={{ height: 18 }} />

          <Card title="Doctors" subtitle="Toggle active status for each doctor's directory listing.">
            <Table
              columns={[
                { header: 'Name', cell: (d) => doctorName(d) },
                { header: 'Specialty', cell: (d) => asString(d.specialty || d.speciality, '—') },
                {
                  header: 'Department',
                  cell: (d) => asString(d.department || d.departmentName, '—'),
                },
                {
                  header: 'Status',
                  cell: (d) => (
                    <Badge tone={d.active === false ? 'neutral' : 'success'}>
                      {d.active === false ? 'Inactive' : 'Active'}
                    </Badge>
                  ),
                },
                {
                  header: 'Actions',
                  cell: (doctor) => (
                    <Button
                      variant="secondary"
                      loading={busyId === asString(doctor.id)}
                      onClick={() => void toggleDoctorActive(doctor)}
                    >
                      {doctor.active === false ? 'Activate' : 'Deactivate'}
                    </Button>
                  ),
                },
              ]}
              rows={doctors}
              rowKey={(d) => asString(d.id) || doctorName(d)}
              empty="No doctors on file yet."
            />
          </Card>
        </>
      )}
    </>
  )
}
