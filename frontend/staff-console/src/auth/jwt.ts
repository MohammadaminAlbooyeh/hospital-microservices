export interface JwtClaims {
  sub?: string
  email?: string
  name?: string
  given_name?: string
  family_name?: string
  preferred_username?: string
  patientId?: string
  patient_id?: string
  roles?: unknown
  authorities?: unknown
  role?: unknown
  scope?: unknown
  scopes?: unknown
  exp?: number
  iat?: number
  realm_access?: { roles?: unknown }
  resource_access?: Record<string, { roles?: unknown }>
  [key: string]: unknown
}

function base64UrlDecode(value: string): string {
  const normalised = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalised + '='.repeat((4 - (normalised.length % 4)) % 4)
  const binary = atob(padded)
  const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

export function decodeJwt(token: string | null | undefined): JwtClaims | null {
  if (!token) return null
  const parts = token.split('.')
  if (parts.length < 2) return null
  try {
    const parsed = JSON.parse(base64UrlDecode(parts[1])) as unknown
    return parsed && typeof parsed === 'object' ? (parsed as JwtClaims) : null
  } catch {
    return null
  }
}

export function normaliseRoles(value: unknown): string[] {
  const roles = new Set<string>()
  const add = (candidate: unknown): void => {
    if (typeof candidate === 'string') {
      candidate
        .split(/[\s,]+/)
        .filter(Boolean)
        .forEach((role) => roles.add(role.replace(/^ROLE_/i, '').toUpperCase()))
      return
    }
    if (Array.isArray(candidate)) {
      candidate.forEach(add)
      return
    }
    if (candidate && typeof candidate === 'object') {
      const obj = candidate as Record<string, unknown>
      add(obj.roles)
      add(obj.authorities)
      add(obj.scope)
    }
  }
  add(value)
  return Array.from(roles)
}

export function rolesFromClaims(claims: JwtClaims | null): string[] {
  if (!claims) return []
  const collected: string[] = []
  for (const candidate of [
    claims.roles,
    claims.authorities,
    claims.role,
    claims.realm_access?.roles,
    claims.scopes,
    claims.scope,
  ]) {
    collected.push(...normaliseRoles(candidate))
  }
  if (claims.resource_access) {
    for (const resource of Object.values(claims.resource_access)) {
      collected.push(...normaliseRoles(resource?.roles))
    }
  }
  return Array.from(new Set(collected))
}

export function isExpired(token: string | null | undefined): boolean {
  const claims = decodeJwt(token)
  if (!claims || typeof claims.exp !== 'number') return false
  return claims.exp * 1000 <= Date.now()
}
