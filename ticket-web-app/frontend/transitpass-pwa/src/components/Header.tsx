import { Bell, Menu, Search } from 'lucide-react';
import { passenger } from '../services/mockData';

interface HeaderProps { title: string; subtitle?: string; onMenuClick: () => void; }

export function Header({ title, subtitle, onMenuClick }: HeaderProps) {
  return (
    <header className="topbar">
      <button className="icon-button mobile-only" onClick={onMenuClick}><Menu size={22}/></button>
      <div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>
      <div className="topbar-actions">
        <label className="search-box"><Search size={18}/><input placeholder="Tìm tuyến, vé, giao dịch..."/></label>
        <button className="icon-button"><Bell size={20}/></button>
        <div className="user-chip"><div className="avatar">NA</div><span>{passenger.name}</span></div>
      </div>
    </header>
  );
}
