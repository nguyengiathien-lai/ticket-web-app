import { useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import { getPassengerTickets, getPassengerTrips, type PassengerTicket } from '../../services/passengerApi';
import type { TravelHistory } from '../../types';
import { currency } from '../../utils/format';

type HistoryTab = 'trips' | 'tickets';

export function HistoryPage() {
  const [histories, setHistories] = useState<TravelHistory[]>([]);
  const [tickets, setTickets] = useState<PassengerTicket[]>([]);
  const [tab, setTab] = useState<HistoryTab>('trips');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([getPassengerTrips(), getPassengerTickets()])
      .then(([tripList, ticketList]) => {
        setHistories(tripList);
        setTickets(ticketList);
      })
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Không thể tải lịch sử.'))
      .finally(() => setIsLoading(false));
  }, []);

  return (
    <Card title="Lịch sử">
      <div className="tabs">
        <button className={tab === 'trips' ? 'active' : ''} onClick={() => setTab('trips')}>Chuyến đi</button>
        <button className={tab === 'tickets' ? 'active' : ''} onClick={() => setTab('tickets')}>Vé</button>
      </div>
      {isLoading && <p>Đang tải lịch sử...</p>}
      {error && <p className="danger" role="alert">{error}</p>}
      {tab === 'trips' && !isLoading && !error && histories.length === 0 && <p>Chưa có chuyến đi.</p>}
      {tab === 'tickets' && !isLoading && !error && tickets.length === 0 && <p>Chưa có vé.</p>}
      <div className="table-wrap">
        {tab === 'trips' ? (
          <table>
            <thead><tr><th>Thời gian</th><th>Phương tiện</th><th>Tuyến</th><th>Ga/trạm</th><th>Trạng thái</th><th>Số tiền</th></tr></thead>
            <tbody>{histories.map((history) => <tr key={history.id}><td>{history.time}</td><td>{history.vehicle}</td><td>{history.route}</td><td>{history.station}</td><td className="success">{history.status}</td><td>{currency(history.amount)}</td></tr>)}</tbody>
          </table>
        ) : (
          <table>
            <thead><tr><th>Ngày mua</th><th>Vé</th><th>Loại</th><th>Thẻ</th><th>Trạng thái</th><th>Số tiền</th></tr></thead>
            <tbody>{tickets.map((ticket) => <tr key={ticket.id}><td>{formatDate(ticket.purchasedAt)}</td><td>{ticket.id}</td><td>{formatTicketType(ticket.type)}</td><td>{ticket.cardId || 'Chưa có'}</td><td className={ticket.status === 'ACTIVE' ? 'success' : 'warning'}>{formatStatus(ticket.status)}</td><td>{currency(ticket.fare)}</td></tr>)}</tbody>
          </table>
        )}
      </div>
    </Card>
  );
}

function formatDate(value?: string) {
  if (!value) return 'Chưa có';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(date);
}

function formatStatus(value?: string) {
  if (!value) return 'Chưa rõ';
  if (value.toUpperCase() === 'ACTIVE') return 'Đang hoạt động';
  return value;
}

function formatTicketType(value?: string) {
  if (!value) return 'Vé';
  const normalized = value.toUpperCase();
  if (normalized === 'SINGLE_TRIP') return 'Vé lượt';
  if (normalized === 'MONTHLY_PASS') return 'Vé gói';
  return value;
}
