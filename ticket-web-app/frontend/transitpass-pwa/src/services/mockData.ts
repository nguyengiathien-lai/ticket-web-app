import type { AdminMetric, TicketPackage, TransitRoute, TravelHistory, User } from '../types';

export const passenger: User = {
  id: 'u-001',
  name: 'Nguyễn Văn A',
  email: 'nguyenvana@gmail.com',
  phone: '0901 234 567',
  role: 'passenger',
  address: '123 Lê Lợi, Quận 1, TP.HCM',
  nationalId: '079098000123'
};

export const packages: TicketPackage[] = [
  { id: 'single', name: 'Vé lượt', description: 'Sử dụng 1 lượt đi', price: 7000, durationDays: 1, type: 'single' },
  { id: 'daily', name: 'Vé ngày', description: 'Không giới hạn lượt trong ngày', price: 30000, durationDays: 1, type: 'daily' },
  { id: 'weekly', name: 'Gói tuần', description: '7 ngày không giới hạn lượt', price: 100000, durationDays: 7, type: 'weekly' },
  { id: 'monthly', name: 'Gói tháng', description: '30 ngày không giới hạn lượt', price: 300000, durationDays: 30, type: 'monthly' },
  { id: 'yearly', name: 'Gói năm', description: '365 ngày không giới hạn lượt', price: 3000000, durationDays: 365, type: 'yearly' }
];

export const histories: TravelHistory[] = [
  { id: 'h1', time: '24/06/2026 08:15', vehicle: 'Metro', route: 'Tuyến 1', station: 'Bến Thành', status: 'Thành công', amount: 7000 },
  { id: 'h2', time: '23/06/2026 17:40', vehicle: 'Xe buýt', route: 'Tuyến 05', station: 'Đại học Sư phạm', status: 'Thành công', amount: 7000 },
  { id: 'h3', time: '23/06/2026 08:08', vehicle: 'Xe buýt', route: 'Tuyến 05', station: 'Chợ Lớn', status: 'Thành công', amount: 7000 },
  { id: 'h4', time: '22/06/2026 18:20', vehicle: 'Metro', route: 'Tuyến 2A', station: 'Thủ Đức', status: 'Thành công', amount: 7000 }
];

export const routes: TransitRoute[] = [
  { id: 'r1', code: '05', name: 'Bến Thành - Chợ Lớn', type: 'Bus', status: 'Đang hoạt động' },
  { id: 'r2', code: '12', name: 'Bến xe Miền Đông - Hiệp Bình Phước', type: 'Bus', status: 'Đang hoạt động' },
  { id: 'r3', code: '01', name: 'Metro Bến Thành - Suối Tiên', type: 'Metro', status: 'Đang hoạt động' },
  { id: 'r4', code: '2A', name: 'Metro Bến Thành - Tham Lương', type: 'Metro', status: 'Bảo trì' }
];

export const adminMetrics: AdminMetric[] = [
  { label: 'Tổng người dùng', value: '12.458', change: '+12.9%' },
  { label: 'Giao dịch hôm nay', value: '2.345', change: '+8.1%' },
  { label: 'Doanh thu tháng', value: '45.680.000đ', change: '+15.3%' }
];

export const revenueData = [
  { month: 'T1', value: 28 }, { month: 'T2', value: 35 }, { month: 'T3', value: 42 },
  { month: 'T4', value: 37 }, { month: 'T5', value: 32 }, { month: 'T6', value: 51 },
  { month: 'T7', value: 56 }, { month: 'T8', value: 34 }, { month: 'T9', value: 36 },
  { month: 'T10', value: 45 }, { month: 'T11', value: 38 }, { month: 'T12', value: 27 }
];
