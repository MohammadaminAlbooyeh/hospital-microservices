export interface Department {
  id?: string
  name?: string
  description?: string
  headDoctorId?: string
  headDoctorName?: string
  staffCount?: number
  [key: string]: unknown
}

export interface Doctor {
  id?: string
  name?: string
  fullName?: string
  firstName?: string
  lastName?: string
  specialty?: string
  speciality?: string
  department?: string
  departmentId?: string
  departmentName?: string
  email?: string
  phone?: string
  active?: boolean
  [key: string]: unknown
}

export interface AnalyticsSummary {
  averageWaitTimeMinutes?: number
  avgWaitTime?: number
  noShowRate?: number
  bedOccupancyRate?: number
  totalRevenue?: number
  revenueByDepartment?: Array<{ department?: string; name?: string; revenue?: number; amount?: number }>
  [key: string]: unknown
}
