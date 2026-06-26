import { Bell, Bus, CalendarClock, CreditCard, Route, Ticket } from 'lucide-react';
import { Card } from '../../components/Card';
import { packages } from '../../services/mockData';
import { currency } from '../../utils/format';

export function DashboardPage() {
  return (
    <div className="page-grid">
      <Card className="hero-card">
        <div><p>Xin chào, Nguyễn Văn A 👋</p><h2>Di chuyển thông minh<br/>Kết nối mọi hành trình</h2></div>
      </Card>
      <div className="quick-actions">
        {[['Mua vé', Ticket], ['Thẻ của tôi', CreditCard], ['Lịch sử', CalendarClock], ['Tuyến đường', Route], ['Thông báo', Bell]].map(([label, Icon]) => {
          const I = Icon as typeof Ticket;
          return <button className="action-button" key={label as string}><I size={22}/><span>{label as string}</span></button>;
        })}
      </div>
      <Card title="Gợi ý cho bạn" className="wide">
        <div className="package-row">
          {packages.slice(0, 3).map((p) => <div className="mini-package" key={p.id}><Bus size={20}/><b>{p.name}</b><span>{currency(p.price)}</span><small>{p.description}</small></div>)}
        </div>
      </Card>
    </div>
  );
}
