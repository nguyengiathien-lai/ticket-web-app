import { useEffect, useMemo, useState } from 'react';
import { Bus, CreditCard, Landmark, TrainFront, Wallet } from 'lucide-react';
import { Card } from '../../components/Card';
import {
  getTicketPackages,
  getTransitRoutes,
  getTransitStations,
  purchaseMonthlyPassTicket,
  purchaseSingleTripTicket
} from '../../services/passengerApi';
import type { TicketPackage, TransitRoute, TransitStation } from '../../types';
import { currency } from '../../utils/format';

type PurchaseMode = 'single' | 'pass';
type PaymentMethod = 'VNPAY' | 'CARD' | 'BANK_TRANSFER' | 'WALLET';

const purchaseModeLabels: Record<PurchaseMode, string> = {
  single: 'Vé lượt',
  pass: 'Vé gói'
};

const paymentMethods: Array<{ value: PaymentMethod; label: string; helper: string; icon: typeof CreditCard }> = [
  { value: 'VNPAY', label: 'VNPay', helper: 'Cổng thanh toán thử nghiệm', icon: Wallet },
  { value: 'CARD', label: 'Thẻ', helper: 'Thẻ ATM, Visa, Mastercard', icon: CreditCard },
  { value: 'BANK_TRANSFER', label: 'Chuyển khoản', helper: 'Ngân hàng nội địa', icon: Landmark },
  { value: 'WALLET', label: 'Ví điện tử', helper: 'Ví mô phỏng', icon: Wallet }
];

export function BuyTicketPage() {
  const today = new Date().toISOString().slice(0, 10);
  const [packages, setPackages] = useState<TicketPackage[]>([]);
  const [routes, setRoutes] = useState<TransitRoute[]>([]);
  const [stations, setStations] = useState<TransitStation[]>([]);
  const [mode, setMode] = useState<PurchaseMode>('single');
  const [transportMode, setTransportMode] = useState('METRO');
  const [fromStationId, setFromStationId] = useState('');
  const [toStationId, setToStationId] = useState('');
  const [routeId, setRouteId] = useState('');
  const [scope, setScope] = useState('SINGLE_ROUTE');
  const [passengerType, setPassengerType] = useState('ADULT');
  const [validFrom, setValidFrom] = useState(today);
  const [durationMonths, setDurationMonths] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPaymentOpen, setIsPaymentOpen] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('VNPAY');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([getTicketPackages(), getTransitRoutes(), getTransitStations()])
      .then(([farePackages, routeList, stationList]) => {
        setPackages(farePackages);
        setRoutes(routeList);
        setStations(stationList);
        setRouteId(routeList[0]?.id ?? '');
        setFromStationId(stationList[0]?.id ?? '');
        setToStationId(stationList[1]?.id ?? stationList[0]?.id ?? '');
      })
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Không thể tải dữ liệu mua vé.'))
      .finally(() => setIsLoading(false));
  }, []);

  const purchasablePackages = packages.filter((farePackage) => farePackage.mode !== 'TRAIN');
  const singlePackages = purchasablePackages.filter((farePackage) => farePackage.type === 'single');
  const passPackages = purchasablePackages.filter((farePackage) => farePackage.type !== 'single');
  const selectedPackage = useMemo(() => {
    if (mode === 'single') {
      return singlePackages.find((farePackage) => sameMode(farePackage.mode, transportMode));
    }

    return passPackages.find((farePackage) =>
      sameMode(farePackage.mode, transportMode)
      && sameScope(farePackage.scope, scope)
      && sameDurationMonths(farePackage, durationMonths)
    );
  }, [durationMonths, mode, passPackages, scope, singlePackages, transportMode]);
  const total = selectedPackage?.price ?? 0;

  function handleModeChange(nextMode: PurchaseMode) {
    setMode(nextMode);
    setIsPaymentOpen(false);
    setMessage('');
    setError('');
  }

  function handlePaymentStart() {
    setMessage('');
    setError('');
    setIsPaymentOpen(true);
  }

  async function handleSubmit(method: PaymentMethod) {
    setMessage('');
    setError('');
    setIsSubmitting(true);
    try {
      if (mode === 'single') {
        await purchaseSingleTripTicket({ mode: transportMode, fromStationId, toStationId, paymentMethod: method });
        setMessage('Mua vé lượt thành công.');
      } else {
        await purchaseMonthlyPassTicket(passInput(method));
        setMessage('Mua vé gói thành công.');
      }
      setIsPaymentOpen(false);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Không thể hoàn tất giao dịch.');
    } finally {
      setIsSubmitting(false);
    }
  }

  function passInput(method: PaymentMethod) {
    return {
      mode: transportMode,
      scope,
      routeId,
      passengerType,
      validFrom,
      durationType: 'MONTHLY',
      durationMonths,
      paymentMethod: method
    };
  }

  return (
    <div className="two-column">
      <Card title="Mua vé">
        {isLoading && <p>Đang tải lựa chọn vé...</p>}
        {error && <p className="danger" role="alert">{error}</p>}
        {message && <p className="success" role="status">{message}</p>}

        <div className="tabs">
          {(Object.keys(purchaseModeLabels) as PurchaseMode[]).map((key) => (
            <button key={key} className={mode === key ? 'active' : ''} onClick={() => handleModeChange(key)}>
              {purchaseModeLabels[key]}
            </button>
          ))}
        </div>

        <p className="field-title">Phương tiện</p>
        <div className="transport-options" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }}>
          <button className={transportMode === 'BUS' ? 'active' : ''} onClick={() => setTransportMode('BUS')}><Bus />Xe buýt</button>
          <button className={transportMode === 'METRO' ? 'active' : ''} onClick={() => setTransportMode('METRO')}><TrainFront />Metro</button>
        </div>

        {mode === 'single' ? (
          <div className="form-grid compact-grid">
            <label>Ga đi<select value={fromStationId} onChange={(event) => setFromStationId(event.target.value)}>{stations.map((station) => <option key={station.id} value={station.id}>{station.name}</option>)}</select></label>
            <label>Ga đến<select value={toStationId} onChange={(event) => setToStationId(event.target.value)}>{stations.map((station) => <option key={station.id} value={station.id}>{station.name}</option>)}</select></label>
          </div>
        ) : (
          <div className="form-grid compact-grid">
            <label>Tuyến<select value={routeId} onChange={(event) => setRouteId(event.target.value)}>{routes.map((route) => <option key={route.id} value={route.id}>{route.code} - {route.name}</option>)}</select></label>
            <label>Phạm vi<select value={scope} onChange={(event) => setScope(event.target.value)}><option value="SINGLE_ROUTE">Một tuyến</option><option value="MULTI_ROUTE">Nhiều tuyến</option></select></label>
            <label>Loại hành khách<select value={passengerType} onChange={(event) => setPassengerType(event.target.value)}><option value="ADULT">Người lớn</option><option value="STUDENT">Sinh viên</option><option value="SENIOR">Người cao tuổi</option></select></label>
            <label>Hiệu lực từ<input type="date" value={validFrom} onChange={(event) => setValidFrom(event.target.value)} /></label>
            <label>Số tháng<input type="number" min="1" value={durationMonths} onChange={(event) => setDurationMonths(Number(event.target.value) || 1)} /></label>
          </div>
        )}

        <div className="total"><span>Tổng tiền</span><b>{currency(total)}</b></div>
        <button className="primary-button" disabled={isLoading || isSubmitting || !selectedPackage || (mode !== 'single' && !routeId) || (mode === 'single' && (!fromStationId || !toStationId))} onClick={handlePaymentStart}>
          Thanh toán
        </button>

        {isPaymentOpen && (
          <div style={{ marginTop: 16, border: '1px solid var(--border)', borderRadius: 14, padding: 14, background: '#f8fbff', display: 'grid', gap: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center' }}>
              <b>Thanh toán giả lập</b>
              <strong>{currency(total)}</strong>
            </div>
            <div className="transport-options" style={{ gridTemplateColumns: 'repeat(2, 1fr)', marginBottom: 0 }}>
              {paymentMethods.map((method) => {
                const Icon = method.icon;
                return (
                  <button key={method.value} className={paymentMethod === method.value ? 'active' : ''} onClick={() => setPaymentMethod(method.value)}>
                    <Icon />
                    <span>{method.label}</span>
                    <small>{method.helper}</small>
                  </button>
                );
              })}
            </div>
            <button className="primary-button" disabled={isSubmitting} onClick={() => handleSubmit(paymentMethod)}>
              {isSubmitting ? 'Đang xử lý...' : 'Xác nhận thanh toán'}
            </button>
          </div>
        )}
      </Card>

      <Card title="Gói vé được xác định">
        <div className="package-list">
          {selectedPackage ? (
            <div className="package-option selected" style={{ gridTemplateColumns: '1fr auto' }}>
              <span><b>{selectedPackage.name}</b><small>{selectedPackage.description}</small></span>
              <b>{currency(selectedPackage.price)}</b>
            </div>
          ) : (
            !isLoading && <p>Chưa có gói vé phù hợp với lựa chọn hiện tại.</p>
          )}
        </div>
      </Card>
    </div>
  );
}

function sameMode(packageMode: string | undefined, selectedMode: string) {
  return (packageMode ?? '').toUpperCase() === selectedMode.toUpperCase();
}

function sameScope(packageScope: string | undefined, selectedScope: string) {
  return (packageScope ?? 'SINGLE_ROUTE').toUpperCase() === selectedScope.toUpperCase();
}

function sameDurationMonths(farePackage: TicketPackage, selectedMonths: number) {
  if (farePackage.durationMonths != null) {
    return farePackage.durationMonths === selectedMonths;
  }
  return Math.round(farePackage.durationDays / 30) === selectedMonths;
}
