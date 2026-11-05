import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import api, { extractApiMessage, PROFILE_KEY, TOKEN_KEY, unwrapObject } from '../api/client'
import { decodeJwt, isExpired, normaliseRoles, rolesFromClaims } from './jwt'

export interface Profile {
  id?: string
  email?: string
  name?: string
  firstName?: string
  lastName?: string
  roles: string[]
  [key: string]: unknown
}

interface AuthContextValue {
  profile: Profile | null
  token: string | null
  isAuthenticated: boolean
  roles: string[]
  login: (email: string, password: string) => Promise<Profile>
  logout: () => void
  updateProfile: (patch: Partial<Profile>) => void
  hasRole: (role: string) => boolean
  hasAnyRole: (roles: string[]) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

function readStoredProfile(): Profile | null {
  try {
    const raw = localStorage.getItem(PROFILE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<Profile>
    return { ...parsed, roles: normaliseRoles(parsed.roles) }
  } catch {
    return null
  }
}

function profileFromClaims(token: string): Profile | null {
  const claims = decodeJwt(token)
  if (!claims) return null
  return {
    id: claims.sub ? String(claims.sub) : undefined,
    email: typeof claims.email === 'string' ? claims.email : undefined,
    name:
      typeof claims.name === 'string'
        ? claims.name
        : [claims.given_name, claims.family_name].filter(Boolean).join(' ') || undefined,
    firstName: typeof claims.given_name === 'string' ? claims.given_name : undefined,
    lastName: typeof claims.family_name === 'string' ? claims.family_name : undefined,
    roles: rolesFromClaims(claims),
  }
}

function initialToken(): string | null {
  const stored = localStorage.getItem(TOKEN_KEY)
  if (!stored) return null
  if (isExpired(stored)) {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(PROFILE_KEY)
    return null
  }
  return stored
}

interface LoginPayload {
  accessToken?: string
  access_token?: string
  token?: string
  jwt?: string
  user?: Record<string, unknown>
  profile?: Record<string, unknown>
  userId?: string
  email?: string
  roles?: unknown
  [key: string]: unknown
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => initialToken())
  const [profile, setProfile] = useState<Profile | null>(() => {
    const stored = readStoredProfile()
    if (stored) return stored
    const activeToken = localStorage.getItem(TOKEN_KEY)
    return activeToken ? profileFromClaims(activeToken) : null
  })

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(PROFILE_KEY)
    setToken(null)
    setProfile(null)
  }, [])

  const login = useCallback(async (email: string, password: string): Promise<Profile> => {
    const response = await api.post('/api/v1/auth/login', { email, password })
    const payload = unwrapObject<LoginPayload>(response.data)
    const accessToken = payload.accessToken || payload.access_token || payload.token || payload.jwt
    if (!accessToken) {
      throw new Error('The sign-in response did not include an access token.')
    }
    const claims = decodeJwt(accessToken)
    const user = (payload.user || payload.profile || {}) as Record<string, unknown>
    const roles = normaliseRoles(user.roles ?? payload.roles ?? rolesFromClaims(claims))
    const resolvedRoles = roles.length ? roles : ['ADMIN']

    const nextProfile: Profile = {
      ...user,
      id: String(user.id ?? payload.userId ?? claims?.sub ?? ''),
      email: String(user.email ?? payload.email ?? claims?.email ?? email),
      name: String(
        user.name ??
          [user.firstName, user.lastName].filter(Boolean).join(' ') ??
          claims?.name ??
          '',
      ),
      firstName: user.firstName ? String(user.firstName) : claims?.given_name,
      lastName: user.lastName ? String(user.lastName) : claims?.family_name,
      roles: resolvedRoles,
    }
    if (!nextProfile.id) delete nextProfile.id
    if (!nextProfile.name) {
      nextProfile.name =
        [nextProfile.firstName, nextProfile.lastName].filter(Boolean).join(' ') || nextProfile.email
    }

    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(PROFILE_KEY, JSON.stringify(nextProfile))
    setToken(accessToken)
    setProfile(nextProfile)
    return nextProfile
  }, [])

  const updateProfile = useCallback((patch: Partial<Profile>) => {
    setProfile((current) => {
      const next: Profile = {
        ...(current || { roles: [] }),
        ...patch,
        roles: patch.roles ? normaliseRoles(patch.roles) : current?.roles || [],
      }
      localStorage.setItem(PROFILE_KEY, JSON.stringify(next))
      return next
    })
  }, [])

  const roles = profile?.roles || []

  const value = useMemo<AuthContextValue>(
    () => ({
      profile,
      token,
      isAuthenticated: Boolean(token),
      roles,
      login,
      logout,
      updateProfile,
      hasRole: (role: string) => roles.includes(role.replace(/^ROLE_/i, '').toUpperCase()),
      hasAnyRole: (allowed: string[]) =>
        allowed.some((role) => roles.includes(role.replace(/^ROLE_/i, '').toUpperCase())),
    }),
    [profile, token, roles, login, logout, updateProfile],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}

export { extractApiMessage }
