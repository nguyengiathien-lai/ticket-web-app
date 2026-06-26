import { Outlet, useLocation } from 'react-router-dom';
import { useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { Header } from '../components/Header';

const titles: Record<string, string> = {
  '/app': 'Trang chủ hành khách',
  '/app/buy-ticket': 'Mua vé & gói vé',
  '/app/my-card': 'Thẻ của tôi',
  '/app/history': 'Lịch sử di chuyển',
  '/app/routes': 'Tuyến & phương tiện',
  '/app/notifications': 'Thông báo',
  '/app/profile': 'Hồ sơ cá nhân',
  '/admin': 'Dashboard quản trị',
  '/admin/users': 'Quản lý người dùng',
  '/admin/login-history': 'Lịch sử đăng nhập',
  '/admin/reports': 'Báo cáo thống kê',
  '/admin/settings': 'Cài đặt hệ thống'
};

export function AppLayout() {
  const [open, setOpen] = useState(false);
  const location = useLocation();
  return (
    <div className="app-shell">
      <div className={open ? 'sidebar-wrap open' : 'sidebar-wrap'} onClick={() => setOpen(false)}><Sidebar /></div>
      <main className="main-content">
        <Header title={titles[location.pathname] ?? 'TransitPass'} subtitle="Quản lý thẻ, vé, lịch sử và giao dịch trên một nền tảng PWA" onMenuClick={() => setOpen(true)} />
        <Outlet />
      </main>
    </div>
  );
}
