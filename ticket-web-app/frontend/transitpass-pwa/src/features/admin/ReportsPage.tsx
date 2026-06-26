import { Card } from '../../components/Card';
import { AdminDashboardPage } from './AdminDashboardPage';
export function ReportsPage() { return <><Card title="Bộ lọc báo cáo"><div className="form-grid"><label>Loại báo cáo<select><option>Doanh thu</option><option>Giao dịch</option><option>Người dùng</option></select></label><label>Chu kỳ<select><option>Theo tháng</option><option>Theo quý</option></select></label></div></Card><AdminDashboardPage /></>; }
