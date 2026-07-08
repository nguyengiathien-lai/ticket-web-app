import { useEffect, useMemo, useState } from 'react';
import { QrCode } from 'lucide-react';
import QRCode from 'qrcode';
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
  const [qrImageUrl, setQrImageUrl] = useState('');
  const [selectedTicketId, setSelectedTicketId] = useState('');
  const [selectedCardIndex, setSelectedCardIndex] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isQrLoading, setIsQrLoading] = useState(false);
  const [error, setError] = useState('');
  const currentCard = cards[selectedCardIndex];

  const routeNames = useMemo(() => new Map(routes.map((route) => [route.id, `${route.code} - ${route.name}`])), [routes]);

  useEffect(() => {
    Promise.allSettled([getPassengerCards(), getPassengerTickets(), getTransitRoutes()])
      .then(([cardResult, ticketResult, routeResult]) => {
        if (cardResult.status === 'fulfilled') {
          setCards(cardResult.value);
        }
        if (ticketResult.status === 'fulfilled') {
          setTickets(ticketResult.value);
          setSelectedTicketId((current) => current || ticketResult.value[0]?.id || '');
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

  useEffect(() => {
    if (!qr?.qrCode) {
      setQrImageUrl('');
      return;
    }

    QRCode.toDataURL(qr.qrCode, {
      errorCorrectionLevel: 'M',
      margin: 2,
      scale: 8
    })
      .then(setQrImageUrl)
      .catch(() => setQrImageUrl(''));
  }, [qr]);

  useEffect(() => {
    if (selectedCardIndex >= cards.length) {
      setSelectedCardIndex(Math.max(cards.length - 1, 0));
    }
  }, [cards.length, selectedCardIndex]);

  async function handleQr(ticketId: string) {
    setSelectedTicketId(ticketId);
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

  async function handleSelectedTicketQr() {
    if (!selectedTicketId) {
      return;
    }
    await handleQr(selectedTicketId);
  }

  function handlePreviousCard() {
    setSelectedCardIndex((current) => (current === 0 ? cards.length - 1 : current - 1));
  }

  function handleNextCard() {
    setSelectedCardIndex((current) => (current + 1) % cards.length);
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
        {!isLoading && !error && !currentCard && <p>Chưa có thẻ.</p>}
        {currentCard && (
          <div style={{ display: 'grid', gridTemplateColumns: '44px 1fr 44px', gap: 12, alignItems: 'center' }}>
            <button className="icon-button inline-icon" disabled={cards.length <= 1} onClick={handlePreviousCard} aria-label="Thẻ trước" title="Thẻ trước">
              &lt;
            </button>
            <div className="transit-card">
              <p>THẺ CỦA TÔI</p>
              <h3>{currentCard.maskedCardNumber || currentCard.cardUid || currentCard.id}</h3>
              <span>Trạng thái</span>
              <strong>{formatStatus(currentCard.status)}</strong>
              <small>{formatCardType(currentCard.type)}</small>
            </div>
            <button className="icon-button inline-icon" disabled={cards.length <= 1} onClick={handleNextCard} aria-label="Thẻ tiếp theo" title="Thẻ tiếp theo">
              &gt;
            </button>
            {cards.length > 1 && (
              <small style={{ gridColumn: '1 / -1', textAlign: 'center', color: 'var(--muted)', fontWeight: 700 }}>
                {selectedCardIndex + 1} / {cards.length}
              </small>
            )}
          </div>
        )}
      </Card>

      <Card title="Thông tin thẻ">
        <dl className="info-list">
          <dt>Chủ thẻ</dt><dd>{account?.fullName || account?.email || 'Hành khách'}</dd>
          <dt>Số thẻ</dt><dd>{cards.length}</dd>
          <dt>Loại thẻ</dt><dd>{formatCardType(currentCard?.type)}</dd>
          <dt>Ngày phát hành</dt><dd>{formatDate(currentCard?.issuedAt ?? currentCard?.activatedAt ?? currentCard?.linkedAt)}</dd>
          <dt>Trạng thái</dt><dd className={currentCard?.status === 'ACTIVE' ? 'success' : 'warning'}>{formatStatus(currentCard?.status)}</dd>
        </dl>
      </Card>

      <Card title="Yêu cầu QR vé" className="wide">
        {!isLoading && tickets.length === 0 && <p>Chưa có vé để tạo mã QR.</p>}
        {tickets.length > 0 && (
          <div className="form-grid compact-grid">
            <label>
              Vé
              <select value={selectedTicketId} onChange={(event) => setSelectedTicketId(event.target.value)}>
                {tickets.map((ticket) => (
                  <option key={ticket.id} value={ticket.id}>
                    {formatTicketType(ticket.type)} - {formatMode(ticket.mode)} - {formatDate(ticket.validUntil)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Thao tác
              <button className="primary-button" disabled={isQrLoading || !selectedTicketId} onClick={handleSelectedTicketQr}>
                {isQrLoading ? 'Đang tải mã QR...' : 'Yêu cầu QR'}
              </button>
            </label>
          </div>
        )}
        {qr && (
          <div className="qr-panel">
            <b>{qr.ticketId}</b>
            {qrImageUrl && (
              <img
                src={qrImageUrl}
                alt={`QR vé ${qr.ticketId}`}
                style={{
                  width: 220,
                  height: 220,
                  maxWidth: '100%',
                  justifySelf: 'center',
                  background: 'white',
                  border: '1px solid var(--border)',
                  borderRadius: 8,
                  padding: 10,
                  imageRendering: 'pixelated'
                }}
              />
            )}
            <code>{qr.qrCode}</code>
          </div>
        )}
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
