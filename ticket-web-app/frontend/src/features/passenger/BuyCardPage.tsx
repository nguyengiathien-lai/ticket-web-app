import { useEffect, useMemo, useState } from 'react';
import { Bus, CreditCard, TrainFront } from 'lucide-react';
import { Card } from '../../components/Card';
import { PurchaseSuccessModal } from '../../components/PurchaseSuccessModal';
import {
  calculateDiscountedPrice,
  getFareDiscounts,
  getTicketPackages,
  getTransitRoutes,
  issueMonthlyPassCard
} from '../../services/passengerApi';
import type { FareDiscount } from '../../services/passengerApi';
import { getStoredAccount } from '../../services/authApi';
import type { TicketPackage, TransitRoute } from '../../types';
import { currency } from '../../utils/format';

type TransportMode = 'BUS' | 'METRO';

export function BuyCardPage() {
  const today = new Date().toISOString().slice(0, 10);
  const [packages, setPackages] = useState<TicketPackage[]>([]);
  const [discounts, setDiscounts] = useState<FareDiscount[]>([]);
  const [routes, setRoutes] = useState<TransitRoute[]>([]);
  const [transportMode, setTransportMode] = useState<TransportMode>('METRO');
  const [routeId, setRouteId] = useState('');
  const [scope, setScope] = useState('SINGLE_ROUTE');
  const [passengerType, setPassengerType] = useState('NO');
  const [validFrom, setValidFrom] = useState(today);
  const [durationType, setDurationType] = useState('MONTHLY');
  const [durationMonths, setDurationMonths] = useState(1);
  const [generatedCardUid, setGeneratedCardUid] = useState(() => generateCardUid());
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [successfulIssuance, setSuccessfulIssuance] =
    useState<Awaited<ReturnType<typeof issueMonthlyPassCard>> | null>(null);

  useEffect(() => {
    Promise.all([getTicketPackages(), getTransitRoutes(), getFareDiscounts()])
      .then(([farePackages, routeList, fareDiscounts]) => {
        setPackages(farePackages.filter((farePackage) => farePackage.type !== 'single' && farePackage.mode !== 'TRAIN'));
        setDiscounts(fareDiscounts);
        setRoutes(routeList);
        setRouteId(routeList.find((route) => sameRouteMode(route, transportMode))?.id ?? '');
      })
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Không thể tải dữ liệu mua thẻ.'))
      .finally(() => setIsLoading(false));
  }, []);

  const routesForMode = routes.filter((route) => sameRouteMode(route, transportMode));
  const isBusMultiRoute = transportMode === 'BUS' && scope === 'MULTI_ROUTE';
  const requiresRoute = !isBusMultiRoute;
  const selectedPackage = useMemo(
    () => packages.find((farePackage) =>
      samePackageMode(farePackage.mode, transportMode)
      && samePackageScope(farePackage.scope, scope)
      && sameDurationType(farePackage.durationType, durationType)
      && sameDurationMonths(farePackage.durationMonths, durationMonths)
    ) ?? packages.find((farePackage) => samePackageMode(farePackage.mode, transportMode)),
    [durationMonths, durationType, packages, scope, transportMode]
  );
  const originalTotal = selectedPackage?.price ?? 0;
  const total = calculateDiscountedPrice(originalTotal, passengerType, discounts, validFrom);
  const hasDiscount = total < originalTotal;

  useEffect(() => {
    if (transportMode === 'METRO' && scope !== 'SINGLE_ROUTE') {
      setScope('SINGLE_ROUTE');
    }

    if (routesForMode.length > 0 && !routesForMode.some((route) => route.id === routeId)) {
      setRouteId(routesForMode[0].id);
    }
  }, [routeId, routesForMode, scope, transportMode]);

  function handleTransportModeChange(nextMode: TransportMode) {
    setTransportMode(nextMode);
    if (nextMode === 'METRO') {
      setScope('SINGLE_ROUTE');
    }
    setMessage('');
    setError('');
  }

  async function handleSubmit() {
    setMessage('');
    setError('');
    setIsSubmitting(true);
    try {
      const issuance = await issueMonthlyPassCard({
        mode: transportMode,
        scope,
        routeId: requiresRoute ? routeId : undefined,
        passengerType,
        validFrom,
        durationType,
        durationMonths,
        cardUid: generatedCardUid,
        supportsMetro: transportMode === 'METRO',
        supportsBus: transportMode === 'BUS'
      });
      setSuccessfulIssuance(issuance);
      setMessage(`Đã phát hành thẻ ${issuance.card?.cardUid ?? issuance.card?.id ?? ''} với vé ${issuance.ticket?.ticketId ?? ''}.`);
      setGeneratedCardUid(generateCardUid());
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Không thể phát hành thẻ.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="two-column">
      <Card title="Mua thẻ">
        {isLoading && <p>Đang tải lựa chọn thẻ...</p>}
        {error && <p className="danger" role="alert">{error}</p>}
        {message && <p className="success" role="status">{message}</p>}

        <p className="field-title">Phương tiện hỗ trợ</p>
        <div className="transport-options" style={{ gridTemplateColumns: 'repeat(2, 1fr)' }}>
          <button className={transportMode === 'BUS' ? 'active' : ''} onClick={() => handleTransportModeChange('BUS')}><Bus />Xe buýt</button>
          <button className={transportMode === 'METRO' ? 'active' : ''} onClick={() => handleTransportModeChange('METRO')}><TrainFront />Metro</button>
        </div>

        <div className="form-grid compact-grid">
          <label style={{ gridColumn: '1 / -1' }}>Phạm vi<select value={scope} onChange={(event) => setScope(event.target.value)}><option value="SINGLE_ROUTE">Một tuyến</option>{transportMode === 'BUS' && <option value="MULTI_ROUTE">Liên tuyến</option>}</select></label>
          {!isBusMultiRoute && (
            <label style={{ gridColumn: '1 / -1' }}>Tuyến<select value={routeId} onChange={(event) => setRouteId(event.target.value)}>{routesForMode.map((route) => <option key={route.id} value={route.id}>{route.code} - {route.name}</option>)}</select></label>
          )}
          <label>Loại hành khách<select value={passengerType} onChange={(event) => setPassengerType(event.target.value)}><option value="NO">Không có</option><option value="STUDENT">Sinh viên</option><option value="PRIORITY">Ưu tiên</option></select></label>
          <label>Hiệu lực từ<input type="date" value={validFrom} onChange={(event) => setValidFrom(event.target.value)} /></label>
          <label>Loại thời hạn<select value={durationType} onChange={(event) => setDurationType(event.target.value)}><option value="MONTHLY">Theo tháng</option><option value="DAILY">Theo ngày</option></select></label>
          <label>Số tháng<input type="number" min="1" max="12"value={durationMonths} onChange={(event) => setDurationMonths(Number(event.target.value) || 1)} /></label>
        </div>

        <div className="total">
          <span>Tổng tiền</span>
          <span>
            {hasDiscount && <del style={{ marginRight: 10, color: 'var(--muted)' }}>{currency(originalTotal)}</del>}
            <b>{currency(total)}</b>
          </span>
        </div>
        <button className="primary-button" disabled={isLoading || isSubmitting || (requiresRoute && !routeId) || packages.length === 0} onClick={handleSubmit}>
          {isSubmitting ? 'Đang phát hành...' : 'Phát hành thẻ'}
        </button>
      </Card>

      <Card title="Thông tin thẻ">
        <div className="transit-card">
          <p>THẺ VÉ THÁNG</p>
          <h3>{generatedCardUid}</h3>
          <span>Gói vé đã chọn</span>
          <strong>{selectedPackage?.name ?? 'Chưa chọn gói vé'}</strong>
          <small>{transportMode === 'METRO' ? 'Metro' : 'Xe buýt'}</small>
          <CreditCard size={28} />
        </div>
      </Card>
      {successfulIssuance && (
        <PurchaseSuccessModal
          title="Mua thẻ thành công"
          message="Thẻ và vé gói đi kèm đã được phát hành thành công."
          onClose={() => setSuccessfulIssuance(null)}
          details={[
            { label: 'Mã thẻ', value: successfulIssuance.card?.cardUid ?? successfulIssuance.card?.id },
            { label: 'Trạng thái thẻ', value: successfulIssuance.card?.status },
            { label: 'Mã vé', value: successfulIssuance.ticket?.ticketId },
            { label: 'Gói vé', value: selectedPackage?.name },
            { label: 'Phương tiện', value: transportMode === 'METRO' ? 'Metro' : 'Xe buýt' },
            { label: 'Giá vé', value: currency(Number(successfulIssuance.ticket?.price ?? total)) },
            { label: 'Hiệu lực từ', value: successfulIssuance.ticket?.validFrom ?? validFrom },
            { label: 'Hiệu lực đến', value: successfulIssuance.ticket?.validTo },
            { label: 'Trạng thái vé', value: successfulIssuance.ticket?.status }
          ]}
        />
      )}
    </div>
  );
}

function generateCardUid() {
  const account = getStoredAccount();
  const displayName = account?.fullName || account?.email?.split('@')[0] || 'USER';
  const initials = toAscii(displayName)
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 4)
    .padEnd(2, 'X');
  const seed = `${displayName}|${account?.id ?? ''}|${account?.email ?? ''}|${Date.now()}|${Math.random()}`;
  return `CARD-${initials}-${hashToBase36(seed).slice(0, 6)}`.toUpperCase();
}

function toAscii(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .replace(/[^a-zA-Z0-9\s]/g, ' ')
    .trim();
}

function hashToBase36(value: string) {
  let hash = 5381;
  for (let index = 0; index < value.length; index += 1) {
    hash = ((hash << 5) + hash + value.charCodeAt(index)) >>> 0;
  }
  return hash.toString(36).padStart(6, '0');
}

function sameRouteMode(route: TransitRoute, selectedMode: TransportMode) {
  const normalizedRouteType = route.type.toLowerCase();
  if (selectedMode === 'METRO') {
    return normalizedRouteType.includes('metro');
  }
  return normalizedRouteType.includes('buýt') || normalizedRouteType.includes('bus') || normalizedRouteType.includes('buÃ½t');
}

function samePackageMode(packageMode: string | undefined, selectedMode: TransportMode) {
  return (packageMode ?? '').toUpperCase() === selectedMode;
}

function samePackageScope(packageScope: string | undefined, selectedScope: string) {
  return (packageScope ?? 'SINGLE_ROUTE').toUpperCase() === selectedScope.toUpperCase();
}

function sameDurationType(packageDurationType: string | undefined, selectedDurationType: string) {
  return (packageDurationType ?? 'MONTHLY').toUpperCase() === selectedDurationType.toUpperCase();
}

function sameDurationMonths(packageDurationMonths: number | undefined, selectedDurationMonths: number) {
  return packageDurationMonths == null || packageDurationMonths === selectedDurationMonths;
}
