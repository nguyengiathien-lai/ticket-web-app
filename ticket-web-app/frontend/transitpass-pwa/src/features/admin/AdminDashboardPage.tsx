import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Card } from '../../components/Card';
import { adminMetrics, revenueData } from '../../services/mockData';

export function AdminDashboardPage() {
  return <div className="admin-grid"><div className="metric-row">{adminMetrics.map((m) => <Card key={m.label}><p>{m.label}</p><h2>{m.value}</h2><span className="success">{m.change}</span></Card>)}</div><Card title="Doanh thu 12 tháng" className="wide chart-card"><ResponsiveContainer width="100%" height={280}><BarChart data={revenueData}><CartesianGrid strokeDasharray="3 3"/><XAxis dataKey="month"/><YAxis/><Tooltip/><Bar dataKey="value" radius={[8,8,0,0]}/></BarChart></ResponsiveContainer></Card></div>;
}
