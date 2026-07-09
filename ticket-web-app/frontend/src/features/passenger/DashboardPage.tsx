import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bell, Bus, CalendarClock, CreditCard, Route, Ticket } from 'lucide-react';
import { Card } from '../../components/Card';
import { getStoredAccount } from '../../services/authApi';
import { getTicketPackages } from '../../services/passengerApi';
import type { TicketPackage } from '../../types';
import { currency } from '../../utils/format';

export function DashboardPage() {
  const account = getStoredAccount();
  const todayLabel = new Intl.DateTimeFormat('vi-VN', {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  }).format(new Date());
  const [packages, setPackages] = useState<TicketPackage[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    getTicketPackages()
      .then(setPackages)
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Không thể tải gói vé.'));
  }, []);

  return (
    <div className="page-grid">
      <Card className="hero-card">
        <div>
          <h2>Xin chào, {account?.fullName || account?.email || 'Hành khách'}</h2>
          <p className="hero-date">{todayLabel}</p>
        </div>
      </Card>
      <div className="quick-actions">
        {[
          ['Mua vé', Ticket, '/app/buy-ticket'],
          ['Mua thẻ', CreditCard, '/app/buy-card'],
          ['Lịch sử', CalendarClock, '/app/history'],
          ['Tuyến đường', Route, '/app/routes'],
          ['Thông báo', Bell, '/app/notifications']
        ].map(([label, Icon, to]) => {
          const I = Icon as typeof Ticket;
          return <Link className="action-button" to={to as string} key={label as string}><I size={22}/><span>{label as string}</span></Link>;
        })}
      </div>
      <Card title="Gợi ý cho bạn" className="wide">
        {error && <p className="danger" role="alert">{error}</p>}
        {!error && packages.length === 0 && <p>Chưa có gói vé khả dụng.</p>}
        <div className="package-row">
          {packages.slice(1, 4).map((p) => <div className="mini-package" key={p.id}><Bus size={20}/><b>{p.name}</b><span>{currency(p.price)}</span><small>{p.description}</small></div>)}
        </div>
      </Card>
    </div>
  );
}
