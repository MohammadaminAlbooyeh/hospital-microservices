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
  bio?: string
  yearsExperience?: number
  [key: string]: unknown
}

export interface Appointment {
  id?: string
  patientId?: string
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
  [key: string]: unknown
}

export interface Invoice {
  id?: string
  invoiceNumber?: string
  number?: string
  patientId?: string
  totalAmount?: number
  amount?: number
  patientShare?: number
  balance?: number
  status?: string
  issuedAt?: string
  dueDate?: string
  currency?: string
  lineItems?: InvoiceLineItem[]
  [key: string]: unknown
}

export interface InvoiceLineItem {
  description?: string
  code?: string
  quantity?: number
  unitPrice?: number
  amount?: number
  [key: string]: unknown
}

export interface Notification {
  id?: string
  title?: string
  subject?: string
  message?: string
  body?: string
  status?: string
  createdAt?: string
  read?: boolean
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
  country?: string
  nationalId?: string
  allergies?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  insurancePolicy?: {
    provider?: string
    policyNumber?: string
    memberId?: string
    validFrom?: string
    validTo?: string
    coveragePercent?: number
  } | null
  [key: string]: unknown
}

export interface AvailabilitySlot {
  value: string
  label: string
  available: boolean
  raw: Record<string, unknown>
}
