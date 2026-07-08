import { useEffect, useMemo, useState } from 'react';
import { Bus, CreditCard, TrainFront } from 'lucide-react';
import { Card } from '../../components/Card';
import {
  getTicketPackages,
  getTransitRoutes,
  issueMonthlyPassCard
} from '../../services/passengerApi';
import type { TicketPackage, TransitRoute } from '../../types';
import { currency } from '../../utils/format';

type TransportMode = 'BUS' | 'METRO';

export function BuyCardPage() {
  const today = new Date().toISOString().slice(0, 10);
  const [packages, setPackages] = useState<TicketPackage[]>([]);
  const [routes, setRoutes] = useState<TransitRoute[]>([]);
  const [transportMode, setTransportMode] = useState<TransportMode>('METRO');
  const [routeId, setRouteId] = useState('');
  const [scope, setScope] = useState('SINGLE_ROUTE');
  const [passengerType, setPassengerType] = useState('NO');
  const [validFrom, setValidFrom] = useState(today);
  const [durationType, setDurationType] = useState('MONTHLY');
  const [durationMonths, setDurationMonths] = useState(1);
  const [cardUid, setCardUid] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([getTicketPackages(), getTransitRoutes()])
      .then(([farePackages, routeList]) => {
        setPackages(farePackages.filter((farePackage) => farePackage.type !== 'single' && farePackage.mode !== 'TRAIN'));
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
  const total = selectedPackage?.price ?? 0;

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
        cardUid: cardUid.trim(),
        supportsMetro: transportMode === 'METRO',
        supportsBus: transportMode === 'BUS'
      });
      setMessage(`Đã phát hành thẻ ${issuance.card?.cardUid ?? issuance.card?.id ?? ''} với vé ${issuance.ticket?.ticketId ?? ''}.`);
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
          <label>Phạm vi<select value={scope} onChange={(event) => setScope(event.target.value)}><option value="SINGLE_ROUTE">Một tuyến</option>{transportMode === 'BUS' && <option value="MULTI_ROUTE">Liên tuyến</option>}</select></label>
          {!isBusMultiRoute && (
            <label>Tuyến<select value={routeId} onChange={(event) => setRouteId(event.target.value)}>{routesForMode.map((route) => <option key={route.id} value={route.id}>{route.code} - {route.name}</option>)}</select></label>
          )}
          <label>Loại hành khách<select value={passengerType} onChange={(event) => setPassengerType(event.target.value)}><option value="NO">Không có</option><option value="STUDENT">Sinh viên</option><option value="PRIORITY">Ưu tiên</option></select></label>
          <label>Hiệu lực từ<input type="date" value={validFrom} onChange={(event) => setValidFrom(event.target.value)} /></label>
          <label>Loại thời hạn<select value={durationType} onChange={(event) => setDurationType(event.target.value)}><option value="MONTHLY">Theo tháng</option><option value="DAILY">Theo ngày</option></select></label>
          <label>Số tháng<input type="number" min="1" value={durationMonths} onChange={(event) => setDurationMonths(Number(event.target.value) || 1)} /></label>
          <label>UID thẻ<input value={cardUid} onChange={(event) => setCardUid(event.target.value)} placeholder="UID thẻ vật lý, có thể bỏ trống" /></label>
        </div>

        <div className="total"><span>Tổng tiền</span><b>{currency(total)}</b></div>
        <button className="primary-button" disabled={isLoading || isSubmitting || (requiresRoute && !routeId) || packages.length === 0} onClick={handleSubmit}>
          {isSubmitting ? 'Đang phát hành...' : 'Phát hành thẻ'}
        </button>
      </Card>

      <Card title="Thông tin thẻ">
        <div className="transit-card">
          <p>THẺ VÉ THÁNG</p>
          <h3>{cardUid || 'UID tự động'}</h3>
          <span>Gói vé đã chọn</span>
          <strong>{selectedPackage?.name ?? 'Chưa chọn gói vé'}</strong>
          <small>{transportMode === 'METRO' ? 'Metro' : 'Xe buýt'}</small>
          <CreditCard size={28} />
        </div>
      </Card>
    </div>
  );
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
