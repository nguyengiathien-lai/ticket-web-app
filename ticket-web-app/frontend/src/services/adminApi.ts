import { getStoredToken, type ApiResponse } from './authApi';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export interface LoginTrafficPoint {
  time: string;
  logins: number;
}

export interface AdminDashboardSummary {
  totalAccounts: number;
  newRegistrationsToday: number;
  loginTrafficToday: LoginTrafficPoint[];
}

export async function getAdminDashboardSummary(): Promise<AdminDashboardSummary> {
  const response = await fetch(`${API_BASE_URL}/admin/dashboard/summary`, {
    headers: authorizationHeader()
  });

  const payload = await response.json().catch(() => null) as ApiResponse<AdminDashboardSummary> | null;
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message || 'Không thể tải dữ liệu bảng điều khiển.');
  }

  return payload.data;
}

function authorizationHeader(): Record<string, string> {
  const token = getStoredToken();
  const tokenType = sessionStorage.getItem('transitpass.tokenType') || 'Bearer';
  return token ? { Authorization: `${tokenType} ${token}` } : {};
}
