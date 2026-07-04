import { useEffect, useMemo, useState } from 'react';
import { QrCode } from 'lucide-react';
import { Card } from '../../components/Card';
import { getStoredAccount } from '../../services/authApi';
import {
  getPassengerCards,
  getPassengerTickets,
  getTicketQr,
  getTransitRoutes,
  type PassengerCard,
  type PassengerTicket,
  type TicketQr
} from '../../services/passengerApi';
import type { TransitRoute } from '../../types';
import { currency } from '../../utils/format';

export function MyCardPage() {
  const account = getStoredAccount();
  const [cards, setCards] = useState<PassengerCard[]>([]);
  const [tickets, setTickets] = useState<PassengerTicket[]>([]);
  const [routes, setRoutes] = useState<TransitRoute[]>([]);
  const [qr, setQr] = useState<TicketQr | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isQrLoading, setIsQrLoading] = useState(false);
  const [error, setError] = useState('');
  const primaryCard = cards[0];

  const routeNames = useMemo(() => new Map(routes.map((route) => [route.id, `${route.code} - ${route.name}`])), [routes]);

  useEffect(() => {
    Promise.allSettled([getPassengerCards(), getPassengerTickets(), getTransitRoutes()])
      .then(([cardResult, ticketResult, routeResult]) => {
        if (cardResult.status === 'fulfilled') {
          setCards(cardResult.value);
        }
        if (ticketResult.status === 'fulfilled') {
          setTickets(ticketResult.value);
        }
        if (routeResult.status === 'fulfilled') {
          setRoutes(routeResult.value);
        }
        if (cardResult.status === 'rejected' && ticketResult.status === 'rejected') {
          const reason = cardResult.reason instanceof Error ? cardResult.reason.message : 'Không thể tải thông tin thẻ và vé.';
          setError(reason);
        }
      })
      .finally(() => setIsLoading(false));
  }, []);

  async function handleQr(ticketId: string) {
    setQr(null);
    setError('');
    setIsQrLoading(true);
    try {
      setQr(await getTicketQr(ticketId));
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Không thể tải mã QR của vé.');
    } finally {
      setIsQrLoading(false);
    }
  }

  function formatRoute(routeId?: string) {
    if (!routeId) return 'Chưa có';
    return routeNames.get(routeId) ?? routeId;
  }

  return (
    <div className="two-column">
      <Card title="Thẻ của tôi">
        {isLoading && <p>Đang tải thẻ...</p>}
        {error && <p className="danger" role="alert">{error}</p>}
        {!isLoading && !error && !primaryCard && <p>Chưa có thẻ.</p>}
        {primaryCard && (
          <div className="transit-card">
            <p>THẺ CỦA TÔI</p>
            <h3>{primaryCard.maskedCardNumber || primaryCard.cardUid || primaryCard.id}</h3>
            <span>Trạng thái</span>
            <strong>{formatStatus(primaryCard.status)}</strong>
            <small>{formatCardType(primaryCard.type)}</small>
          </div>
        )}
      </Card>

      <Card title="Thông tin thẻ">
        <dl className="info-list">
          <dt>Chủ thẻ</dt><dd>{account?.fullName || account?.email || 'Hành khách'}</dd>
          <dt>Số thẻ</dt><dd>{cards.length}</dd>
          <dt>Loại thẻ</dt><dd>{formatCardType(primaryCard?.type)}</dd>
          <dt>Ngày phát hành</dt><dd>{formatDate(primaryCard?.issuedAt ?? primaryCard?.activatedAt ?? primaryCard?.linkedAt)}</dd>
          <dt>Trạng thái</dt><dd className={primaryCard?.status === 'ACTIVE' ? 'success' : 'warning'}>{formatStatus(primaryCard?.status)}</dd>
        </dl>
      </Card>

      <Card title="Vé và gói tháng" className="wide">
        {!isLoading && tickets.length === 0 && <p>Chưa có vé.</p>}
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Vé</th>
                <th>Loại</th>
                <th>Phương tiện</th>
                <th>Tuyến di chuyển</th>
                <th>Ngày mua</th>
                <th>Trạng thái</th>
                <th>Hiệu lực đến</th>
                <th>Giá vé</th>
                <th>QR</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((ticket) => (
                <tr key={ticket.id}>
                  <td>{ticket.id}</td>
                  <td>{formatTicketType(ticket.type)}</td>
                  <td>{formatMode(ticket.mode)}</td>
                  <td>{formatRoute(ticket.routeId)}</td>
                  <td>{formatDate(ticket.purchasedAt)}</td>
                  <td className={ticket.status === 'ACTIVE' ? 'success' : 'warning'}>{formatStatus(ticket.status)}</td>
                  <td>{formatDate(ticket.validUntil)}</td>
                  <td>{currency(ticket.fare)}</td>
                  <td><button className="icon-button inline-icon" onClick={() => handleQr(ticket.id)} disabled={isQrLoading} title="Tải mã QR"><QrCode size={18} /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {qr && (
          <div className="qr-panel">
            <b>{qr.ticketId}</b>
            <code>{qr.qrCode}</code>
          </div>
        )}
      </Card>
    </div>
  );
}

function formatDate(value?: string) {
  if (!value) return 'Chưa có';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short' }).format(date);
}

function formatStatus(value?: string) {
  if (!value) return 'Chưa rõ';
  const normalized = value.toUpperCase();
  if (normalized === 'ACTIVE') return 'Đang hoạt động';
  if (normalized === 'INACTIVE') return 'Chưa hoạt động';
  if (normalized === 'EXPIRED') return 'Hết hạn';
  if (normalized === 'BLOCKED') return 'Đã khóa';
  return value;
}

function formatMode(value?: string) {
  if (!value) return 'Chưa có';
  const normalized = value.toUpperCase();
  if (normalized === 'BUS') return 'Xe buýt';
  if (normalized === 'METRO') return 'Metro';
  if (normalized === 'TRAIN') return 'Tàu';
  return value;
}

function formatTicketType(value?: string) {
  if (!value) return 'Vé';
  const normalized = value.toUpperCase();
  if (normalized === 'SINGLE_TRIP') return 'Vé lượt';
  if (normalized === 'MONTHLY_PASS') return 'Vé gói';
  return value;
}

function formatCardType(value?: string) {
  if (!value) return 'Thẻ vé gói';
  const normalized = value.toUpperCase();
  if (normalized === 'MONTHLY_PASS') return 'Thẻ vé gói';
  return value;
}
