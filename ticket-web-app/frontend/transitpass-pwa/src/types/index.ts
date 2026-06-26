export type Role = 'passenger' | 'admin';

export interface User {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: Role;
  avatarUrl?: string;
  address?: string;
  nationalId?: string;
}

export interface TicketPackage {
  id: string;
  name: string;
  description: string;
  price: number;
  durationDays: number;
  type: 'single' | 'daily' | 'weekly' | 'monthly' | 'yearly';
}

export interface TravelHistory {
  id: string;
  time: string;
  vehicle: 'Xe buýt' | 'Metro' | 'Tàu điện';
  route: string;
  station: string;
  status: 'Thành công' | 'Thất bại';
  amount: number;
}

export interface TransitRoute {
  id: string;
  code: string;
  name: string;
  type: 'Bus' | 'Metro' | 'Tram';
  status: 'Đang hoạt động' | 'Bảo trì';
}

export interface AdminMetric {
  label: string;
  value: string;
  change: string;
}
