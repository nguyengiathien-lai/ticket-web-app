import { Search } from 'lucide-react';
import { Card } from '../../components/Card';
import { routes } from '../../services/mockData';

export function RoutesPage() {
  return <Card title="Tuyến & phương tiện"><label className="search-box full"><Search size={18}/><input placeholder="Tìm tuyến hoặc phương tiện..."/></label><div className="route-list">{routes.map((r) => <div className="route-item" key={r.id}><b>{r.code}</b><span>{r.name}</span><small>{r.type}</small><em className={r.status === 'Đang hoạt động' ? 'success' : 'warning'}>{r.status}</em></div>)}</div></Card>;
}
