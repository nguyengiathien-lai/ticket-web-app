const API_BASE_URL = import.meta.env.VITE_API_BASE_URL 
// ?? 'https://ticket-web-app-production.up.railway.app/api';

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
  passengerCode?: string;
  dateOfBirth?: string;
  gender?: string;
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

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface ProfileUpdateRequest {
  fullName?: string;
  phoneNumber?: string;
  personalId?: string;
  address?: string;
  dateOfBirth?: string | null;
  gender?: string;
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  const payload = await response.json().catch(() => null) as ApiResponse<LoginResponse> | null;
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(toVietnameseLoginError(payload?.message));
  }

  return payload.data;
}

function toVietnameseLoginError(message?: string) {
  if (message === 'Invalid credentials or account is not ready for login') {
    return 'Thông tin đăng nhập không đúng hoặc tài khoản chưa sẵn sàng để đăng nhập.';
  }

  return message || 'Không thể đăng nhập. Vui lòng kiểm tra thông tin đăng nhập.';
}

export async function registerAccount(request: RegisterRequest): Promise<AccountResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  });

  const payload = await response.json().catch(() => null) as ApiResponse<AccountResponse> | null;
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message || 'Không thể tạo tài khoản. Vui lòng kiểm tra thông tin đã nhập.');
  }

  return payload.data;
}

export async function verifyEmailOtp(email: string, code: string): Promise<AccountResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/verify-email`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, code })
  });

  const payload = await response.json().catch(() => null) as ApiResponse<AccountResponse> | null;
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message || 'Không thể xác thực email. Vui lòng kiểm tra mã OTP.');
  }

  return payload.data;
}

export async function resendEmailOtp(email: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/auth/resend-email-otp`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email })
  });

  const payload = await response.json().catch(() => null) as ApiResponse<void> | null;
  if (!response.ok || !payload?.success) {
    throw new Error(payload?.message || 'Không thể gửi lại mã xác thực.');
  }
}

export async function updateProfile(accountId: string, request: ProfileUpdateRequest): Promise<AccountResponse> {
  const response = await fetch(`${API_BASE_URL}/accounts/${accountId}/profile`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeader()
    },
    body: JSON.stringify(request)
  });

  const payload = await response.json().catch(() => null) as ApiResponse<AccountResponse> | null;
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message || 'Không thể cập nhật hồ sơ. Vui lòng thử lại.');
  }

  updateStoredAccount(payload.data);
  return payload.data;
}

export function storeSession(session: LoginResponse) {
  localStorage.setItem('transitpass.token', session.token);
  localStorage.setItem('transitpass.tokenType', session.tokenType);
  localStorage.setItem('transitpass.expiresAt', String(session.expiresAt));
  localStorage.setItem('transitpass.account', JSON.stringify(session.account));
}

export function updateStoredAccount(account: AccountResponse) {
  localStorage.setItem('transitpass.account', JSON.stringify(account));
}

export function getStoredAccount(): AccountResponse | null {
  const accountJson = localStorage.getItem('transitpass.account');
  if (!accountJson) {
    return null;
  }

  try {
    return JSON.parse(accountJson) as AccountResponse;
  } catch {
    clearSession();
    return null;
  }
}

export function getStoredToken(): string | null {
  return localStorage.getItem('transitpass.token');
}

function authorizationHeader(): Record<string, string> {
  const token = getStoredToken();
  const tokenType = localStorage.getItem('transitpass.tokenType') || 'Bearer';
  return token ? { Authorization: `${tokenType} ${token}` } : {};
}

export function isSessionValid() {
  const token = getStoredToken();
  const expiresAt = Number(localStorage.getItem('transitpass.expiresAt'));
  return Boolean(token) && Number.isFinite(expiresAt) && expiresAt > Math.floor(Date.now() / 1000);
}

export function clearSession() {
  localStorage.removeItem('transitpass.token');
  localStorage.removeItem('transitpass.tokenType');
  localStorage.removeItem('transitpass.expiresAt');
  localStorage.removeItem('transitpass.account');
}

export function isAdmin(account: AccountResponse | null) {
  return Boolean(account?.roles.includes('APP_ADMIN'));
}

export function nextRouteFor(account: AccountResponse) {
  return isAdmin(account) ? '/admin' : '/app';
}
