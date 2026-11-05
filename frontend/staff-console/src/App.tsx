import type { ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { Layout } from './components/Layout'
import type { NavItem } from './components/Layout'
import Appointments from './pages/Appointments'
import Dashboard from './pages/Dashboard'
import Encounter from './pages/Encounter'
import Login from './pages/Login'
import NotFound from './pages/NotFound'
import PatientSearch from './pages/PatientSearch'

const NAV: NavItem[] = [
  { to: '/dashboard', label: 'Schedule' },
  { to: '/patients', label: 'Patient search' },
  { to: '/appointments', label: 'Appointments' },
  { to: '/encounters/new', label: 'New encounter' },
]

function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Layout nav={NAV}>{children}</Layout>
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/patients"
            element={
              <ProtectedRoute>
                <PatientSearch />
              </ProtectedRoute>
            }
          />
          <Route
            path="/appointments"
            element={
              <ProtectedRoute>
                <Appointments />
              </ProtectedRoute>
            }
          />
          <Route
            path="/encounters/new"
            element={
              <ProtectedRoute>
                <Encounter />
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route
            path="*"
            element={
              <ProtectedRoute>
                <NotFound />
              </ProtectedRoute>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
