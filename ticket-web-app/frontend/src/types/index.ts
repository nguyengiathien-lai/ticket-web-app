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
  mode?: string;
  scope?: string;
  durationType?: string;
  durationMonths?: number;
}

export interface TravelHistory {
  id: string;
  ticketId?: string;
  time: string;
  vehicle: string;
  route: string;
  station: string;
  tapInStation?: string;
  tapOutStation?: string;
  tapInTime?: string;
  tapOutTime?: string;
  status: string;
  amount: number;
}

export interface TransitRoute {
  id: string;
  code: string;
  name: string;
  type: string;
  status: string;
}

export interface TransitStation {
  id: string;
  code: string;
  name: string;
  routeId?: string;
  sequence?: number;
  kmMarker?: number;
}

export interface AdminMetric {
  label: string;
  value: string;
  change: string;
}
