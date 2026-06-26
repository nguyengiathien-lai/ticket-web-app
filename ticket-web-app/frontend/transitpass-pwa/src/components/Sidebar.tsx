import { NavLink } from 'react-router-dom';
import { Bell, Bus, ChartNoAxesColumn, CreditCard, History, Home, LogOut, Map, Settings, Shield, Ticket, User, Users } from 'lucide-react';

const passengerLinks = [
  { to: '/app', label: 'Trang chủ', icon: Home },
  { to: '/app/buy-ticket', label: 'Mua vé', icon: Ticket },
  { to: '/app/my-card', label: 'Thẻ của tôi', icon: CreditCard },
  { to: '/app/history', label: 'Lịch sử di chuyển', icon: History },
  { to: '/app/routes', label: 'Tuyến & phương tiện', icon: Bus },
  { to: '/app/notifications', label: 'Thông báo', icon: Bell },
  { to: '/app/profile', label: 'Hồ sơ cá nhân', icon: User }
];

const adminLinks = [
  { to: '/admin', label: 'Dashboard', icon: ChartNoAxesColumn },
  { to: '/admin/users', label: 'Quản lý người dùng', icon: Users },
  { to: '/admin/login-history', label: 'Lịch sử đăng nhập', icon: Shield },
  { to: '/admin/reports', label: 'Báo cáo thống kê', icon: Map },
  { to: '/admin/settings', label: 'Cài đặt', icon: Settings }
];

export function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="brand"><Bus size={28}/><span>TransitPass</span></div>
      <div className="hero-copy">
        <h1>Web App mua bán thẻ vé phương tiện công cộng</h1>
        <p>Thuận tiện – Nhanh chóng – An toàn</p>
      </div>
      <div className="city-art"><div className="train"/><div className="bus"/></div>
      <nav>
        <p className="nav-title">DÀNH CHO HÀNH KHÁCH</p>
        {passengerLinks.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} end className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            <Icon size={18}/><span>{label}</span>
          </NavLink>
        ))}
        <p className="nav-title">DÀNH CHO QUẢN TRỊ VIÊN</p>
        {adminLinks.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} end className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
            <Icon size={18}/><span>{label}</span>
          </NavLink>
        ))}
      </nav>
      <div className="support-card"><b>Cần hỗ trợ?</b><span>Hotline: 1900 1234</span><span>Email: support@transitpass.vn</span></div>
      <NavLink to="/login" className="logout"><LogOut size={17}/> Đăng xuất</NavLink>
    </aside>
  );
}
