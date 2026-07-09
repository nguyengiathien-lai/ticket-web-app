import { useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import { getPassengerTrips } from '../../services/passengerApi';
import type { TravelHistory } from '../../types';

export function HistoryPage() {
  const [histories, setHistories] = useState<TravelHistory[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getPassengerTrips()
      .then(setHistories)
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Không thể tải lịch sử di chuyển.'))
      .finally(() => setIsLoading(false));
  }, []);

  return (
    <Card title="Lịch sử di chuyển">
      {isLoading && <p>Đang tải lịch sử di chuyển...</p>}
      {error && <p className="danger" role="alert">{error}</p>}
      {!isLoading && !error && histories.length === 0 && <p>Chưa có chuyến đi.</p>}
      {!isLoading && !error && histories.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Ticket ID</th>
                <th>Loại phương tiện</th>
                <th>Tap in station</th>
                <th>Tap out station</th>
                <th>Thời gian tap in</th>
                <th>Thời gian tap out</th>
              </tr>
            </thead>
            <tbody>
              {histories.map((history) => (
                <tr key={history.id}>
                  <td>{history.ticketId || 'Chưa có'}</td>
                  <td>{history.vehicle}</td>
                  <td>{history.tapInStation || 'Chưa có'}</td>
                  <td>{history.tapOutStation || 'Chưa có'}</td>
                  <td>{formatDateTime(history.tapInTime)}</td>
                  <td>{formatDateTime(history.tapOutTime)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function formatDateTime(value?: string) {
  if (!value) return 'Chưa có';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(date);
}
