import { NavLink, useNavigate } from 'react-router-dom';
import {
  Bell,
  Bus,
  ChartNoAxesColumn,
  CreditCard,
  History,
  Home,
  LogOut,
  Map,
  Settings,
  Shield,
  Ticket,
  User,
  Users
} from 'lucide-react';
import { clearSession } from '../services/authApi';

const passengerLinks = [
  { to: '/app', label: 'Trang chủ', icon: Home },
  { to: '/app/buy-ticket', label: 'Mua vé', icon: Ticket },
  { to: '/app/buy-card', label: 'Mua thẻ', icon: CreditCard },
  { to: '/app/my-card', label: 'Thẻ của tôi', icon: CreditCard },
  { to: '/app/history', label: 'Lịch sử di chuyển', icon: History },
  { to: '/app/routes', label: 'Tuyến đường', icon: Bus },
  { to: '/app/notifications', label: 'Thông báo', icon: Bell },
  { to: '/app/profile', label: 'Hồ sơ', icon: User }
];

const adminLinks = [
  { to: '/admin', label: 'Tổng quan', icon: ChartNoAxesColumn },
  { to: '/admin/users', label: 'Người dùng', icon: Users },
  { to: '/admin/login-history', label: 'Lịch sử đăng nhập', icon: Shield },
  { to: '/admin/reports', label: 'Báo cáo', icon: Map },
  { to: '/admin/settings', label: 'Cài đặt', icon: Settings }
];

interface SidebarProps {
  role: 'admin' | 'passenger';
}

export function Sidebar({ role }: SidebarProps) {
  const navigate = useNavigate();
  const links = role === 'admin' ? adminLinks : passengerLinks;

  function handleLogout() {
    clearSession();
    navigate('/login', { replace: true });
  }

  return (
    <aside className="sidebar">
      <div className="brand"><Bus size={28}/><span>TransitPass</span></div>
      <div className="hero-copy">
        <h1>{role === 'admin' ? 'Bảng quản trị TransitPass' : 'Cổng hành khách TransitPass'}</h1>
        <p>{role === 'admin' ? 'Vận hành - Báo cáo - Kiểm soát' : 'Tiện lợi - Nhanh chóng - An toàn'}</p>
      </div>
      <div className="city-art"><div className="train"/><div className="bus"/></div>
      <nav>
        <p className="nav-title">{role === 'admin' ? 'QUẢN TRỊ' : 'HÀNH KHÁCH'}</p>
        {links.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} end className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            <Icon size={18}/><span>{label}</span>
          </NavLink>
        ))}
      </nav>
      <div className="support-card"><b>Cần hỗ trợ?</b><span>Đường dây nóng: 1900 1234</span><span>Hộp thư: support@transitpass.vn</span></div>
      <button type="button" className="logout logout-button" onClick={handleLogout}>
        <LogOut size={17}/> Đăng xuất
      </button>
    </aside>
  );
}
