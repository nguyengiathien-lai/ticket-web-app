import { Bell, Menu, Search } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { AccountResponse } from '../services/authApi';

interface HeaderProps {
  title: string;
  subtitle?: string;
  account: AccountResponse | null;
  role: 'admin' | 'passenger';
  onMenuClick: () => void;
}

function displayName(account: AccountResponse | null) {
  return account?.fullName || account?.email || 'TransitPass';
}

function initials(account: AccountResponse | null) {
  const name = displayName(account).trim();
  const parts = name.split(/\s+/);
  if (parts.length > 1) {
    return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
}

export function Header({ title, subtitle, account, role, onMenuClick }: HeaderProps) {
  const navigate = useNavigate();
  return (
    <header className="topbar">
      <button className="icon-button mobile-only" onClick={onMenuClick}><Menu size={22}/></button>
      <div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>
      <div className="topbar-actions">
        <button
          className="icon-button"
          type="button"
          aria-label="Mở thông báo"
          title="Thông báo"
          onClick={() => navigate(role === 'passenger' ? '/app/notifications' : '/admin')}
        >
          <Bell size={20}/>
        </button>
        <div className="user-chip"><div className="avatar">{initials(account)}</div><span>{displayName(account)}</span></div>
      </div>
    </header>
  );
}
