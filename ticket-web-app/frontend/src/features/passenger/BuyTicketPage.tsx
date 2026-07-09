import { useEffect, useMemo, useState } from 'react';
import { Bus, CreditCard, Landmark, TrainFront, Wallet } from 'lucide-react';
import { Card } from '../../components/Card';
import { PurchaseSuccessModal } from '../../components/PurchaseSuccessModal';
import {
  calculateDiscountedPrice,
  getFareDiscounts,
  getSingleTripFareQuote,
  getTicketPackages,
  getTransitRoutes,
  getTransitStations,
  purchasePassTicket,
  purchaseSingleTripTicket
} from '../../services/passengerApi';
import type { TicketPurchaseResponse } from '../../services/passengerApi';
import type { FareDiscount, SingleTripFareQuote } from '../../services/passengerApi';
import type { TicketPackage, TransitRoute, TransitStation } from '../../types';
import { currency } from '../../utils/format';

type PurchaseMode = 'single' | 'pass';
type PaymentMethod = 'VNPAY' | 'CARD' | 'BANK_TRANSFER' | 'WALLET';
type PassDurationType = 'DAILY' | 'WEEKLY' | 'MONTHLY';

const purchaseModeLabels: Record<PurchaseMode, string> = {
  single: 'Vé lượt',
  pass: 'Vé gói'
};

const passDurationLabels: Record<PassDurationType, string> = {
  DAILY: 'Gói ngày',
  WEEKLY: 'Gói tuần',
  MONTHLY: 'Gói tháng'
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
  const [singleTripFareQuote, setSingleTripFareQuote] = useState<SingleTripFareQuote | null>(null);
  const [discounts, setDiscounts] = useState<FareDiscount[]>([]);
  const [routes, setRoutes] = useState<TransitRoute[]>([]);
  const [stations, setStations] = useState<TransitStation[]>([]);
  const [mode, setMode] = useState<PurchaseMode>('single');
  const [transportMode, setTransportMode] = useState('METRO');
  const [fromStationId, setFromStationId] = useState('');
  const [toStationId, setToStationId] = useState('');
  const [routeId, setRouteId] = useState('');
  const [scope, setScope] = useState('SINGLE_ROUTE');
  const [passengerType, setPassengerType] = useState('NO');
  const [validFrom, setValidFrom] = useState(today);
  const [durationType, setDurationType] = useState<PassDurationType>('MONTHLY');
  const [durationMonths, setDurationMonths] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPaymentOpen, setIsPaymentOpen] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('VNPAY');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [successfulPurchase, setSuccessfulPurchase] = useState<TicketPurchaseResponse | null>(null);

  useEffect(() => {
    Promise.all([getTicketPackages(), getTransitRoutes(), getTransitStations(), getFareDiscounts()])
      .then(([farePackages, routeList, stationList, fareDiscounts]) => {
        setPackages(farePackages);
        setDiscounts(fareDiscounts);
        setRoutes(routeList);
        setStations(stationList);
        setRouteId(routeList.find((route) => sameRouteMode(route, transportMode))?.id ?? routeList[0]?.id ?? '');
        setFromStationId(stationList[0]?.id ?? '');
        setToStationId(stationList[1]?.id ?? stationList[0]?.id ?? '');
      })
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Không thể tải dữ liệu mua vé.'))
      .finally(() => setIsLoading(false));
  }, []);

  const purchasablePackages = packages.filter((farePackage) => farePackage.mode !== 'TRAIN');
  const singlePackages = purchasablePackages.filter((farePackage) => farePackage.type === 'single');
  const passPackages = purchasablePackages.filter((farePackage) => farePackage.type !== 'single');
  const routesForMode = routes.filter((route) => sameRouteMode(route, transportMode));
  const selectedPackage = useMemo(() => {
    if (mode === 'single') {
      return singlePackages.find((farePackage) => sameMode(farePackage.mode, transportMode));
    }

    return passPackages.find((farePackage) =>
      sameMode(farePackage.mode, transportMode)
      && sameScope(farePackage.scope, scope)
      && sameDurationType(farePackage, durationType)
      && sameDurationValue(farePackage, durationType, durationMonths)
    );
  }, [durationMonths, durationType, mode, passPackages, scope, singlePackages, transportMode]);
  const singleTripFare = normalizeSingleTripFareQuote(singleTripFareQuote, selectedPackage?.price);
  const originalTotal = mode === 'single'
    ? singleTripFare.price
    : selectedPackage?.price ?? 0;
  const total = mode === 'pass'
    ? calculateDiscountedPrice(originalTotal, passengerType, discounts, validFrom)
    : originalTotal;
  const hasDiscount = total < originalTotal;

  useEffect(() => {
    if (mode !== 'pass' || routesForMode.length === 0) {
      return;
    }

    if (!routesForMode.some((route) => route.id === routeId)) {
      setRouteId(routesForMode[0].id);
    }
  }, [mode, routeId, routesForMode]);

  useEffect(() => {
    if (mode !== 'single' || !fromStationId || !toStationId) {
      setSingleTripFareQuote(null);
      return;
    }

    let active = true;
    getSingleTripFareQuote(transportMode, fromStationId, toStationId)
      .then((quote) => {
        if (active) {
          setSingleTripFareQuote(quote);
        }
      })
      .catch(() => {
        if (active) {
          setSingleTripFareQuote(null);
        }
      });

    return () => {
      active = false;
    };
  }, [fromStationId, mode, toStationId, transportMode]);

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
        const purchase = await purchaseSingleTripTicket({ mode: transportMode, fromStationId, toStationId, paymentMethod: method });
        setSuccessfulPurchase(purchase);
        setMessage('Mua vé lượt thành công.');
      } else {
        const purchase = await purchasePassTicket(passInput(method));
        setSuccessfulPurchase(purchase);
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
      ...(selectedPackage?.scope ? { scope: selectedPackage.scope } : {}),
      routeId,
      passengerType,
      validFrom,
      durationType,
      ...(durationType === 'MONTHLY' ? { durationMonths } : {}),
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
            <label style={{ gridColumn: '1 / -1' }}>Tuyến<select value={routeId} onChange={(event) => setRouteId(event.target.value)}>{routesForMode.map((route) => <option key={route.id} value={route.id}>{route.code} - {route.name}</option>)}</select></label>
            <label>Ga đi<select value={fromStationId} onChange={(event) => setFromStationId(event.target.value)}>{stations.filter((station) => station.routeId === routeId).map((station) => <option key={station.id} value={station.id}>{station.name}</option>)}</select></label>
            <label>Ga đến<select value={toStationId} onChange={(event) => setToStationId(event.target.value)}>{stations.filter((station) => station.routeId === routeId && station.id !== fromStationId).map((station) => <option key={station.id} value={station.id}>{station.name}</option>)}</select></label>
          </div>
        ) : (
          <div className="form-grid compact-grid">
            {transportMode !== 'METRO' && (
              <label style={{ gridColumn: '1 / -1' }}>Phạm vi<select value={scope} onChange={(event) => setScope(event.target.value)}><option value="SINGLE_ROUTE">Một tuyến</option><option value="MULTI_ROUTE">Liên tuyến</option></select></label>
            )}
            {transportMode == 'METRO' && (
              <label style={{ gridColumn: '1 / -1' }}>Phạm vi<select value={scope} onChange={(event) => setScope(event.target.value)}><option value="SINGLE_ROUTE">Một tuyến</option></select></label>
            )}
            {scope == "SINGLE_ROUTE" && (
              <label style={{ gridColumn: '1 / -1' }}>Tuyến<select value={routeId} onChange={(event) => setRouteId(event.target.value)}>{routesForMode.map((route) => <option key={route.id} value={route.id}>{route.code} - {route.name}</option>)}</select></label>
            )}
            <label>Loại gói<select value={durationType} onChange={(event) => setDurationType(event.target.value as PassDurationType)}>{(Object.keys(passDurationLabels) as PassDurationType[]).map((key) => <option key={key} value={key}>{passDurationLabels[key]}</option>)}</select></label>
            <label>Loại hành khách<select value={passengerType} onChange={(event) => setPassengerType(event.target.value)}><option value="NO">Không có</option><option value="STUDENT">Sinh viên</option><option value="PRIORITY">Ưu tiên</option></select></label>
            <label>Hiệu lực từ<input type="date" value={validFrom} onChange={(event) => setValidFrom(event.target.value)} /></label>
            {durationType === 'MONTHLY' && <label>Số tháng<input type="number" min="1" max="12"value={durationMonths} onChange={(event) => setDurationMonths(Number(event.target.value) || 1)} /></label>}
            {durationType !== 'MONTHLY' && <label>Thời hạn<input value={durationType === 'DAILY' ? '1 ngày' : '1 tuần'} readOnly /></label>}
          </div>
        )}

        <div className="total">
          <span>{mode === 'single' ? 'Giá vé tạm tính' : 'Tổng tiền'}</span>
          <span>
            {hasDiscount && <del style={{ marginRight: 10, color: 'var(--muted)' }}>{currency(originalTotal)}</del>}
            <b>{currency(total)}</b>
          </span>
        </div>
        {mode === 'single' && (
          <p className="muted-text" style={{ marginTop: -6 }}>
            {singleTripFare.distanceKm != null
              ? `Khoảng cách ${singleTripFare.distanceKm.toFixed(1)} km, tính theo biểu giá ${currency(singleTripFare.baseFare)} + ${currency(singleTripFare.ratePerKm)}/km.`
              : 'Chọn ga đi và ga đến để hệ thống tính giá vé lượt theo khoảng cách.'}
          </p>
        )}
        <button className="primary-button" disabled={isLoading || isSubmitting || !selectedPackage || (mode !== 'single' && !routeId) || (mode === 'single' && (!fromStationId || !toStationId))} onClick={handlePaymentStart}>
          Thanh toán
        </button>

        {isPaymentOpen && (
          <div style={{ marginTop: 16, border: '1px solid var(--border)', borderRadius: 14, padding: 14, background: '#f8fbff', display: 'grid', gap: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center' }}>
              <b>Thanh toán giả lập</b>
              <strong>{currency(total)}</strong>
            </div>
            {mode === 'single' && singleTripFare.distanceKm != null && (
              <small style={{ color: 'var(--muted)' }}>
                Vé lượt {singleTripFare.distanceKm.toFixed(1)} km, giá đã áp dụng min/max của biểu giá.
              </small>
            )}
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
              <span>
                {hasDiscount && <del style={{ display: 'block', color: 'var(--muted)' }}>{currency(originalTotal)}</del>}
                <b>{currency(total)}</b>
              </span>
            </div>
          ) : (
            !isLoading && <p>Chưa có gói vé phù hợp với lựa chọn hiện tại.</p>
          )}
        </div>
      </Card>
      {successfulPurchase && (
        <PurchaseSuccessModal
          title="Mua vé thành công"
          message="Vé đã được phát hành và sẵn sàng sử dụng."
          onClose={() => setSuccessfulPurchase(null)}
          details={[
            { label: 'Mã vé', value: successfulPurchase.ticketId },
            { label: 'Loại vé', value: mode === 'single' ? 'Vé lượt' : selectedPackage?.name },
            { label: 'Mã xác nhận', value: successfulPurchase.confirmationNumber },
            {
              label: 'Phạm vi',
              value: mode === 'pass' && scope === 'MULTI_ROUTE' ? 'Liên tuyến' : undefined
            },
            {
              label: 'Tuyến',
              value: mode === 'pass' && scope === 'SINGLE_ROUTE'
                ? routeName(routes, routeId)
                : undefined
            },
            {
              label: 'Ga đi',
              value: mode === 'single'
                ? stationName(stations, successfulPurchase.origin ?? fromStationId)
                : undefined
            },
            {
              label: 'Ga đến',
              value: mode === 'single'
                ? stationName(stations, successfulPurchase.destination ?? toStationId)
                : undefined
            },
            { label: 'Tổng tiền', value: currency(Number(successfulPurchase.totalPrice ?? total)) },
            { label: 'Phương thức thanh toán', value: paymentMethod },
            { label: 'Trạng thái', value: successfulPurchase.paymentStatus ?? successfulPurchase.status },
            { label: 'Thời gian mua', value: formatPurchaseTime(successfulPurchase.purchasedAt) }
          ]}
        />
      )}
    </div>
  );
}

function formatPurchaseTime(value?: string) {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('vi-VN');
}

function normalizeSingleTripFareQuote(quote: SingleTripFareQuote | null, fallbackPrice = 0) {
  return {
    price: toNumber(quote?.price, fallbackPrice),
    distanceKm: toOptionalNumber(quote?.distanceKm),
    baseFare: toNumber(quote?.baseFare),
    ratePerKm: toNumber(quote?.ratePerKm)
  };
}

function toNumber(value: number | string | undefined, fallback = 0) {
  const numberValue = Number(value ?? fallback);
  return Number.isFinite(numberValue) ? numberValue : fallback;
}

function toOptionalNumber(value: number | string | undefined) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
}

function stationName(stations: TransitStation[], stationReference: string) {
  const station = stations.find((item) =>
    item.id === stationReference || item.code === stationReference
  );
  return station?.name ?? stationReference;
}

function routeName(routes: TransitRoute[], selectedRouteId: string) {
  const route = routes.find((item) => item.id === selectedRouteId || item.code === selectedRouteId);
  return route?.name ?? route?.code ?? selectedRouteId;
}

function sameMode(packageMode: string | undefined, selectedMode: string) {
  return (packageMode ?? '').toUpperCase() === selectedMode.toUpperCase();
}

function sameScope(packageScope: string | undefined, selectedScope: string) {
  return (packageScope ?? 'SINGLE_ROUTE').toUpperCase() === selectedScope.toUpperCase();
}

function sameRouteMode(route: TransitRoute, selectedMode: string) {
  const normalizedRouteType = route.type.toLowerCase();
  const normalizedMode = selectedMode.toLowerCase();
  if (normalizedMode === 'metro') {
    return normalizedRouteType.includes('metro');
  }
  if (normalizedMode === 'bus') {
    return normalizedRouteType.includes('buýt') || normalizedRouteType.includes('bus');
  }
  return false;
}

function sameDurationType(farePackage: TicketPackage, selectedDurationType: PassDurationType) {
  const normalized = farePackage.durationType?.toUpperCase();
  if (normalized) {
    return normalized === selectedDurationType;
  }

  if (selectedDurationType === 'DAILY') return farePackage.type === 'daily' || farePackage.durationDays <= 1;
  if (selectedDurationType === 'WEEKLY') return farePackage.type === 'weekly' || farePackage.durationDays === 7;
  return farePackage.type === 'monthly' || farePackage.durationDays >= 28;
}

function sameDurationValue(farePackage: TicketPackage, selectedDurationType: PassDurationType, selectedMonths: number) {
  if (selectedDurationType !== 'MONTHLY') {
    return true;
  }

  if (farePackage.durationMonths != null) {
    return farePackage.durationMonths === selectedMonths;
  }
  return Math.round(farePackage.durationDays / 30) === selectedMonths;
}
