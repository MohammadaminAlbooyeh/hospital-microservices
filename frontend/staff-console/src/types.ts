export interface Doctor {
  id?: string
  name?: string
  fullName?: string
  firstName?: string
  lastName?: string
  specialty?: string
  speciality?: string
  department?: string
  departmentName?: string
  email?: string
  phone?: string
  [key: string]: unknown
}

export interface Patient {
  id?: string
  mrn?: string
  firstName?: string
  lastName?: string
  dateOfBirth?: string
  gender?: string
  email?: string
  phone?: string
  addressLine?: string
  city?: string
  allergies?: string
  [key: string]: unknown
}

export interface Appointment {
  id?: string
  patientId?: string
  patientName?: string
  patient?: Patient
  doctorId?: string
  doctorName?: string
  doctor?: Doctor
  scheduledAt?: string
  startTime?: string
  appointmentDate?: string
  dateTime?: string
  reason?: string
  status?: string
  notes?: string
  checkedInAt?: string
  [key: string]: unknown
}

export interface Encounter {
  id?: string
  patientId?: string
  appointmentId?: string
  doctorId?: string
  visitDate?: string
  chiefComplaint?: string
  notes?: string
  diagnosis?: string
  treatmentPlan?: string
  status?: string
  createdAt?: string
  [key: string]: unknown
}
