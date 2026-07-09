import { useEffect, useState } from 'react';
import { CreditCard, TicketCheck } from 'lucide-react';
import { Card } from '../../components/Card';
import {
  getPurchaseNotifications,
  type PurchaseNotification
} from '../../services/passengerApi';
import { currency } from '../../utils/format';

export function NotificationsPage() {
  const [notifications, setNotifications] = useState<PurchaseNotification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getPurchaseNotifications()
      .then(setNotifications)
      .catch((exception) => setError(
        exception instanceof Error ? exception.message : 'Không thể tải thông báo.'
      ))
      .finally(() => setLoading(false));
  }, []);

  return (
    <Card title="Thông báo giao dịch">
      {loading && <p>Đang tải thông báo...</p>}
      {error && <p className="danger" role="alert">{error}</p>}
      <div className="notice-list">
        {!loading && !error && notifications.length === 0 && (
          <p>Bạn chưa có thông báo giao dịch nào.</p>
        )}
        {notifications.map((notification) => {
          const isCard = notification.category === 'CARD_PURCHASE';
          const Icon = isCard ? CreditCard : TicketCheck;
          return (
            <div key={notification.orderId} className="purchase-notification">
              <Icon size={24} aria-hidden="true" />
              <span>
                <b>{isCard ? 'Mua thẻ thành công' : 'Mua vé thành công'}</b>
                <p>
                  {isCard
                    ? `Thẻ ${notification.itemCode ?? ''} đã được đặt mua thành công.`
                    : `${ticketLabel(notification.itemCode)} đã được mua thành công.`}
                </p>
                <small>
                  Mã đơn: {notification.orderCode} · {currency(notification.totalAmount)}
                  {' · '}{formatDateTime(notification.orderedAt)}
                </small>
              </span>
            </div>
          );
        })}
      </div>
    </Card>
  );
}

function ticketLabel(itemCode?: string) {
  if (itemCode === 'SINGLE_TRIP') return 'Vé lượt';
  if (itemCode === 'MONTHLY_PASS') return 'Vé gói';
  return itemCode ? `Vé ${itemCode}` : 'Vé';
}

function formatDateTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('vi-VN');
}
