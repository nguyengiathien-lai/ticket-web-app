import { getStoredToken, type ApiResponse } from './authApi';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export interface AdminLoginHistoryItem {
  id: number;
  user?: string;
  email?: string;
  createdAt: string;
  ipAddress?: string;
  userAgent?: string;
  result: 'SUCCESS' | 'FAILURE';
}

export async function getAdminLoginHistory(): Promise<AdminLoginHistoryItem[]> {
  const response = await fetch(`${API_BASE_URL}/admin/login-history`, {
    headers: authorizationHeader()
  });

  const payload = await response.json().catch(() => null) as ApiResponse<AdminLoginHistoryItem[]> | null;
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message || 'Không thể tải lịch sử đăng nhập.');
  }

  return payload.data;
}

function authorizationHeader(): Record<string, string> {
  const token = getStoredToken();
  const tokenType = sessionStorage.getItem('transitpass.tokenType') || 'Bearer';
  return token ? { Authorization: `${tokenType} ${token}` } : {};
}
