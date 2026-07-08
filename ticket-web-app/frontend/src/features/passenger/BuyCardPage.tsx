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

export function BuyCardPage() {
  const today = new Date().toISOString().slice(0, 10);
  const [packages, setPackages] = useState<TicketPackage[]>([]);
  const [routes, setRoutes] = useState<TransitRoute[]>([]);
  const [selectedPackageId, setSelectedPackageId] = useState('');
  const [transportMode, setTransportMode] = useState('METRO');
  const [routeId, setRouteId] = useState('');
  const [scope, setScope] = useState('SINGLE_ROUTE');
  const [passengerType, setPassengerType] = useState('ADULT');
  const [validFrom, setValidFrom] = useState(today);
  const [durationType, setDurationType] = useState('MONTHLY');
  const [durationMonths, setDurationMonths] = useState(1);
  const [cardUid, setCardUid] = useState('');
  const [supportsMetro, setSupportsMetro] = useState(true);
  const [supportsBus, setSupportsBus] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([getTicketPackages(), getTransitRoutes()])
      .then(([farePackages, routeList]) => {
        const monthlyPackages = farePackages.filter((farePackage) => farePackage.type !== 'single' && farePackage.mode !== 'TRAIN');
        setPackages(monthlyPackages);
        setRoutes(routeList);
        setSelectedPackageId(monthlyPackages[0]?.id ?? '');
        setRouteId(routeList[0]?.id ?? '');
      })
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Không thể tải dữ liệu mua thẻ.'))
      .finally(() => setIsLoading(false));
  }, []);

  const selectedPackage = useMemo(
    () => packages.find((farePackage) => farePackage.id === selectedPackageId),
    [packages, selectedPackageId]
  );
  const cardMode = supportsBus && !supportsMetro ? 'BUS' : 'METRO';
  const requiresRoute = cardMode === 'BUS' && scope === 'SINGLE_ROUTE';
  const total = selectedPackage?.price ?? 0;

  useEffect(() => {
    if (selectedPackage) {
      setTransportMode(selectedPackage.mode ?? transportMode);
      setScope(selectedPackage.scope ?? scope);
      setDurationType(selectedPackage.durationType ?? durationType);
      setDurationMonths(selectedPackage.durationMonths ?? durationMonths);
    }
  }, [selectedPackage]);

  async function handleSubmit() {
    setMessage('');
    setError('');
    setIsSubmitting(true);
    try {
      const issuance = await issueMonthlyPassCard({
        mode: cardMode,
        scope,
        routeId: requiresRoute ? routeId : undefined,
        passengerType,
        validFrom,
        durationType,
        durationMonths,
        cardUid: cardUid.trim(),
        supportsMetro,
        supportsBus
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
          <button className={supportsBus ? 'active' : ''} onClick={() => setSupportsBus((value) => !value)}><Bus />Xe buýt</button>
          <button className={supportsMetro ? 'active' : ''} onClick={() => setSupportsMetro((value) => !value)}><TrainFront />Metro</button>
        </div>

        {/* <label>
          Vé gói gắn với thẻ
          <select value={selectedPackageId} onChange={(event) => setSelectedPackageId(event.target.value)} disabled={isLoading || packages.length === 0}>
            {packages.map((farePackage) => <option key={farePackage.id} value={farePackage.id}>{farePackage.name}</option>)}
          </select>
        </label> */}

        <div className="form-grid compact-grid">
          <label>Tuyến<select value={routeId} onChange={(event) => setRouteId(event.target.value)}>{routes.map((route) => <option key={route.id} value={route.id}>{route.code} - {route.name}</option>)}</select></label>
          <label>Phạm vi<select value={scope} onChange={(event) => setScope(event.target.value)}><option value="SINGLE_ROUTE">Một tuyến</option><option value="MULTI_ROUTE">Nhiều tuyến</option></select></label>
          <label>Loại hành khách<select value={passengerType} onChange={(event) => setPassengerType(event.target.value)}><option value="NO">Không có</option><option value="STUDENT">Sinh viên</option><option value="PRIORITY">Ưu tiên</option></select></label>
          <label>Hiệu lực từ<input type="date" value={validFrom} onChange={(event) => setValidFrom(event.target.value)} /></label>
          <label>Loại thời hạn<select value={durationType} onChange={(event) => setDurationType(event.target.value)}><option value="MONTHLY">Theo tháng</option><option value="DAILY">Theo ngày</option></select></label>
          <label>Số tháng<input type="number" min="1" value={durationMonths} onChange={(event) => setDurationMonths(Number(event.target.value) || 1)} /></label>
          <label>UID thẻ<input value={cardUid} onChange={(event) => setCardUid(event.target.value)} placeholder="UID thẻ vật lý, có thể bỏ trống" /></label>
        </div>

        <div className="total"><span>Tổng tiền</span><b>{currency(total)}</b></div>
        <button className="primary-button" disabled={isLoading || isSubmitting || (requiresRoute && !routeId) || packages.length === 0 || (!supportsBus && !supportsMetro)} onClick={handleSubmit}>
          {isSubmitting ? 'Đang phát hành...' : 'Phát hành thẻ'}
        </button>
      </Card>

      <Card title="Thông tin thẻ">
        <div className="transit-card">
          <p>THẺ VÉ THÁNG</p>
          <h3>{cardUid || 'UID tự động'}</h3>
          <span>Gói vé đã chọn</span>
          <strong>{selectedPackage?.name ?? 'Chưa chọn gói vé'}</strong>
          <small>{supportsMetro ? 'Metro' : ''}{supportsMetro && supportsBus ? ' + ' : ''}{supportsBus ? 'Xe buýt' : ''}</small>
          <CreditCard size={28} />
        </div>
      </Card>
    </div>
  );
}
