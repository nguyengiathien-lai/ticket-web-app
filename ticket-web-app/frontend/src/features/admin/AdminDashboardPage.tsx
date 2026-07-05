import { useEffect, useMemo, useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Card } from '../../components/Card';
import { getAdminDashboardSummary, type AdminDashboardSummary } from '../../services/adminApi';

export function AdminDashboardPage() {
  const [summary, setSummary] = useState<AdminDashboardSummary | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    getAdminDashboardSummary()
      .then((data) => {
        if (active) {
          setSummary(data);
          setError('');
        }
      })
      .catch((exception: Error) => {
        if (active) {
          setError(exception.message);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const metrics = useMemo(() => [
    {
      label: 'Tổng người dùng',
      value: formatNumber(summary?.totalAccounts)
    },
    {
      label: 'Đăng ký mới hôm nay',
      value: formatNumber(summary?.newRegistrationsToday)
    }
  ], [summary]);

  return (
    <div className="admin-grid">
      {error && <p className="danger" role="alert">{error}</p>}
      <div className="metric-row">
        {metrics.map((metric) => (
          <Card key={metric.label}>
            <p>{metric.label}</p>
            <h2>{metric.value}</h2>
          </Card>
        ))}
      </div>
      <Card title="Lượt đăng nhập hôm nay" className="wide chart-card">
        <ResponsiveContainer width="100%" height={280}>
          <BarChart data={summary?.loginTrafficToday ?? []}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Bar dataKey="logins" name="Lượt đăng nhập" radius={[8, 8, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </Card>
    </div>
  );
}

function formatNumber(value?: number) {
  if (value === undefined) {
    return '...';
  }

  return new Intl.NumberFormat('vi-VN').format(value);
}
