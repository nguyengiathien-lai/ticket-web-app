const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  timestamp?: string;
}

export interface AccountResponse {
  id: string;
  email: string;
  fullName?: string;
  phoneNumber?: string;
  personalId?: string;
  address?: string;
  isActive: boolean;
  isEmailVerified: boolean;
  mustChangePassword: boolean;
  roles: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresAt: number;
  mustChangePassword: boolean;
  account: AccountResponse;
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  const payload = await response.json().catch(() => null) as ApiResponse<LoginResponse> | null;
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message || 'Unable to sign in. Please check your credentials.');
  }

  return payload.data;
}

export function storeSession(session: LoginResponse) {
  localStorage.setItem('transitpass.token', session.token);
  localStorage.setItem('transitpass.tokenType', session.tokenType);
  localStorage.setItem('transitpass.expiresAt', String(session.expiresAt));
  localStorage.setItem('transitpass.account', JSON.stringify(session.account));
}

export function nextRouteFor(account: AccountResponse) {
  return account.roles.includes('APP_ADMIN') ? '/admin' : '/app';
}
