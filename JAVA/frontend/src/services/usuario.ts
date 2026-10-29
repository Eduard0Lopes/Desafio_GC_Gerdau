export function currentUser(): string {
  return localStorage.getItem('user') || '403921'
}

export function currentRole(): string {
  return (localStorage.getItem('role') || 'USUARIO').toUpperCase()
}

export function isAdmin(): boolean {
  return currentRole() === 'ADMIN'
}

export function canManageRequest(request: { usuarioCriacaoId?: string | null }): boolean {
  return isAdmin() || (request.usuarioCriacaoId ?? '') === currentUser()
}
