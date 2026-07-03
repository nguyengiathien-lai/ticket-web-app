import { useEffect, useMemo, useState } from 'react';
import { Bus, TrainFront } from 'lucide-react';
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

const purchaseModeLabels: Record<PurchaseMode, string> = {
  single: 'Vé lượt',
  pass: 'Vé tháng'
};

export function BuyTicketPage() {
  const today = new Date().toISOString().slice(0, 10);
  const [packages, setPackages] = useState<TicketPackage[]>([]);
  const [routes, setRoutes] = useState<TransitRoute[]>([]);
  const [stations, setStations] = useState<TransitStation[]>([]);
  const [mode, setMode] = useState<PurchaseMode>('single');
  const [selectedPackageId, setSelectedPackageId] = useState('');
  const [transportMode, setTransportMode] = useState('METRO');
  const [fromStationId, setFromStationId] = useState('');
  const [toStationId, setToStationId] = useState('');
  const [routeId, setRouteId] = useState('');
  const [scope, setScope] = useState('SINGLE_ROUTE');
  const [passengerType, setPassengerType] = useState('ADULT');
  const [validFrom, setValidFrom] = useState(today);
  const [durationType, setDurationType] = useState('MONTHLY');
  const [durationMonths, setDurationMonths] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([getTicketPackages(), getTransitRoutes(), getTransitStations()])
      .then(([farePackages, routeList, stationList]) => {
        const firstPurchasablePackage = farePackages.find((farePackage) => farePackage.mode !== 'TRAIN');
        setPackages(farePackages);
        setRoutes(routeList);
        setStations(stationList);
        setSelectedPackageId(firstPurchasablePackage?.id ?? '');
        setRouteId(routeList[0]?.id ?? '');
        setFromStationId(stationList[0]?.id ?? '');
        setToStationId(stationList[1]?.id ?? stationList[0]?.id ?? '');
      })
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Không thể tải dữ liệu mua vé.'))
      .finally(() => setIsLoading(false));
  }, []);

  const selectedPackage = useMemo(
    () => packages.find((farePackage) => farePackage.id === selectedPackageId),
    [packages, selectedPackageId]
  );
  const purchasablePackages = packages.filter((farePackage) => farePackage.mode !== 'TRAIN');
  const singlePackages = purchasablePackages.filter((farePackage) => farePackage.type === 'single');
  const passPackages = purchasablePackages.filter((farePackage) => farePackage.type !== 'single');
  const visiblePackages = mode === 'single' ? singlePackages : passPackages;
  const total = selectedPackage?.price ?? 0;

  useEffect(() => {
    if (selectedPackage) {
      setTransportMode(selectedPackage.mode ?? transportMode);
      setScope(selectedPackage.scope ?? scope);
      setDurationType(selectedPackage.durationType ?? durationType);
      setDurationMonths(selectedPackage.durationMonths ?? durationMonths);
    }
  }, [selectedPackage]);

  function handleModeChange(nextMode: PurchaseMode) {
    setMode(nextMode);
    const nextPackage = (nextMode === 'single' ? singlePackages : passPackages)[0];
    setSelectedPackageId(nextPackage?.id ?? '');
    setMessage('');
    setError('');
  }

  async function handleSubmit() {
    setMessage('');
    setError('');
    setIsSubmitting(true);
    try {
      if (mode === 'single') {
        await purchaseSingleTripTicket({ mode: transportMode, fromStationId, toStationId });
        setMessage('Mua vé lượt thành công.');
      } else {
        await purchaseMonthlyPassTicket(passInput());
        setMessage('Mua vé tháng thành công.');
      }
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Không thể hoàn tất giao dịch.');
    } finally {
      setIsSubmitting(false);
    }
  }

  function passInput() {
    return {
      mode: transportMode,
      scope,
      routeId,
      passengerType,
      validFrom,
      durationType,
      durationMonths
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

        <label>
          Gói vé
          <select value={selectedPackageId} onChange={(event) => setSelectedPackageId(event.target.value)} disabled={isLoading || visiblePackages.length === 0}>
            {visiblePackages.map((farePackage) => <option key={farePackage.id} value={farePackage.id}>{farePackage.name}</option>)}
          </select>
        </label>

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
            <label>Loại thời hạn<select value={durationType} onChange={(event) => setDurationType(event.target.value)}><option value="MONTHLY">Theo tháng</option><option value="DAILY">Theo ngày</option></select></label>
            <label>Số tháng<input type="number" min="1" value={durationMonths} onChange={(event) => setDurationMonths(Number(event.target.value) || 1)} /></label>
          </div>
        )}

        <div className="total"><span>Tổng tiền</span><b>{currency(total)}</b></div>
        <button className="primary-button" disabled={isLoading || isSubmitting || (mode !== 'single' && !routeId) || (mode === 'single' && (!fromStationId || !toStationId))} onClick={handleSubmit}>
          {isSubmitting ? 'Đang xử lý...' : 'Thanh toán bằng VNPay thử nghiệm'}
        </button>
      </Card>

      <Card title="Các gói vé hiện có">
        <div className="package-list">
          {visiblePackages.map((farePackage) => (
            <label className={selectedPackageId === farePackage.id ? 'package-option selected' : 'package-option'} key={farePackage.id}>
              <input type="radio" checked={selectedPackageId === farePackage.id} onChange={() => setSelectedPackageId(farePackage.id)} />
              <span><b>{farePackage.name}</b><small>{farePackage.description}</small></span>
              <b>{currency(farePackage.price)}</b>
            </label>
          ))}
          {!isLoading && visiblePackages.length === 0 && <p>Chưa có gói vé phù hợp với loại mua này.</p>}
        </div>
      </Card>
    </div>
  );
}
