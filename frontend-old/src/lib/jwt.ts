export interface JwtClaims {
  sub: string
  email?: string
  userId?: number
  roles?: string[]
  providers?: string[]
  enabled?: boolean
  exp?: number
  [key: string]: unknown
}

/** Decodes a JWT payload without verifying its signature -- the backend is the source of
 * truth for auth; this is only used to read claims (e.g. roles) for client-side UI routing. */
export function decodeJwt(token: string): JwtClaims | null {
  try {
    const payload = token.split('.')[1]
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join('')
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}
