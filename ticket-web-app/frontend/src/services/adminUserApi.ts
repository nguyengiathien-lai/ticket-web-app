import { getStoredToken, type AccountResponse, type ApiResponse } from './authApi';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export async function getAdminAccounts(): Promise<AccountResponse[]> {
  return request<AccountResponse[]>('/accounts');
}

export async function activateAdminAccount(accountId: string): Promise<AccountResponse> {
  return request<AccountResponse>(`/accounts/${accountId}/activate`, {
    method: 'PUT'
  });
}

export async function deactivateAdminAccount(accountId: string): Promise<AccountResponse> {
  return request<AccountResponse>(`/accounts/${accountId}/deactivate`, {
    method: 'PUT'
  });
}

export async function deleteAdminAccount(accountId: string): Promise<void> {
  await request<void>(`/accounts/${accountId}`, {
    method: 'DELETE'
  });
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      ...authorizationHeader(),
      ...init.headers
    }
  });

  const payload = await response.json().catch(() => null) as ApiResponse<T> | null;
  if (!response.ok || !payload?.success) {
    throw new Error(payload?.message || 'Không thể xử lý yêu cầu quản trị.');
  }

  return payload.data as T;
}

function authorizationHeader(): Record<string, string> {
  const token = getStoredToken();
  const tokenType = localStorage.getItem('transitpass.tokenType') || 'Bearer';
  return token ? { Authorization: `${tokenType} ${token}` } : {};
}
