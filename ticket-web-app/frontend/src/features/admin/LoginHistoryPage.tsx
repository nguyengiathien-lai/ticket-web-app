import { useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import {
  getAdminLoginHistory,
  type AdminLoginHistoryItem
} from '../../services/adminLoginHistoryApi';

export function LoginHistoryPage() {
  const [items, setItems] = useState<AdminLoginHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    getAdminLoginHistory()
      .then((data) => {
        if (active) {
          setItems(data);
          setError('');
        }
      })
      .catch((exception: Error) => {
        if (active) {
          setError(exception.message);
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  return (
    <Card title="Lịch sử đăng nhập">
      {error && <p className="danger" role="alert">{error}</p>}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Người dùng</th>
              <th>Thời gian</th>
              <th>IP</th>
              <th>Thiết bị</th>
              <th>Kết quả</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={5}>Đang tải lịch sử đăng nhập...</td>
              </tr>
            )}
            {!loading && items.length === 0 && (
              <tr>
                <td colSpan={5}>Chưa có lịch sử đăng nhập.</td>
              </tr>
            )}
            {!loading && items.map((item) => (
              <tr key={item.id}>
                <td>{item.user || item.email || 'Không xác định'}</td>
                <td>{formatDateTime(item.createdAt)}</td>
                <td>{item.ipAddress || 'Không có'}</td>
                <td>{formatDevice(item.userAgent)}</td>
                <td className={item.result === 'SUCCESS' ? 'success' : 'danger'}>
                  {item.result === 'SUCCESS' ? 'Thành công' : 'Thất bại'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

function formatDevice(userAgent?: string) {
  if (!userAgent) {
    return 'Không có';
  }

  if (userAgent.includes('Edg/')) {
    return 'Microsoft Edge';
  }

  if (userAgent.includes('Chrome/')) {
    return 'Chrome';
  }

  if (userAgent.includes('Firefox/')) {
    return 'Firefox';
  }

  if (userAgent.includes('Safari/')) {
    return 'Safari';
  }

  return userAgent.length > 48 ? `${userAgent.slice(0, 48)}...` : userAgent;
}
