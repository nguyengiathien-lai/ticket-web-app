import { Outlet, useLocation } from 'react-router-dom';
import { useState } from 'react';
import { Sidebar } from '../components/Sidebar';
import { Header } from '../components/Header';
import { getStoredAccount } from '../services/authApi';

const titles: Record<string, string> = {
  '/app': 'Bảng điều khiển của hành khách',
  '/app/buy-ticket': 'Mua vé',
  '/app/buy-card': 'Mua thẻ',
  '/app/my-card': 'Thẻ vé của tôi',
  '/app/history': 'Lịch sử di chuyển',
  '/app/routes': 'Tuyến và nhà ga',
  '/app/notifications': 'Thông báo',
  '/app/profile': 'Hồ sơ',
  '/admin': 'Bảng điều khiển quản trị',
  '/admin/users': 'Quản lý người dùng',
  '/admin/login-history': 'Lịch sử đăng nhập',
  '/admin/settings': 'Cài đặt hệ thống'
};

interface AppLayoutProps {
  role: 'admin' | 'passenger';
}

export function AppLayout({ role }: AppLayoutProps) {
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const account = getStoredAccount();

  return (
    <div className="app-shell">
      <div className={open ? 'sidebar-wrap open' : 'sidebar-wrap'} onClick={() => setOpen(false)}>
        <Sidebar role={role} />
      </div>
      <main className="main-content">
        <Header
          title={titles[location.pathname] ?? 'TransitPass'}
          subtitle={role === 'admin'
            ? 'Quản lý người dùng và cài đặt hệ thống'
            : 'Di chuyển thông minh - Kết nối mọi hành trình'}
          account={account}
          role={role}
          onMenuClick={() => setOpen(true)}
        />
        <Outlet />
      </main>
    </div>
  );
}
