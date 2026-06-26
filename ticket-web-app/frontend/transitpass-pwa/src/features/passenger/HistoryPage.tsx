import { Card } from '../../components/Card';
import { histories } from '../../services/mockData';
import { currency } from '../../utils/format';

export function HistoryPage() {
  return <Card title="Lịch sử di chuyển"><div className="table-wrap"><table><thead><tr><th>Thời gian</th><th>Phương tiện</th><th>Tuyến</th><th>Trạm</th><th>Trạng thái</th><th>Số tiền</th></tr></thead><tbody>{histories.map((h) => <tr key={h.id}><td>{h.time}</td><td>{h.vehicle}</td><td>{h.route}</td><td>{h.station}</td><td className="success">{h.status}</td><td>{currency(h.amount)}</td></tr>)}</tbody></table></div></Card>;
}
