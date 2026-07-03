import { useEffect, useMemo, useState } from 'react';
import { Search } from 'lucide-react';
import { Card } from '../../components/Card';
import { getTransitRoutes, getTransitStations } from '../../services/passengerApi';
import type { TransitRoute, TransitStation } from '../../types';

type TransitTab = 'routes' | 'stations';

export function RoutesPage() {
  const [routes, setRoutes] = useState<TransitRoute[]>([]);
  const [stations, setStations] = useState<TransitStation[]>([]);
  const [tab, setTab] = useState<TransitTab>('routes');
  const [search, setSearch] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([getTransitRoutes(), getTransitStations()])
      .then(([routeList, stationList]) => {
        setRoutes(routeList);
        setStations(stationList);
      })
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Không thể tải dữ liệu tuyến.'))
      .finally(() => setIsLoading(false));
  }, []);

  const query = search.trim().toLowerCase();
  const filteredRoutes = useMemo(() => {
    if (!query) return routes;
    return routes.filter((route) => [route.code, route.name, route.type].some((value) => value.toLowerCase().includes(query)));
  }, [routes, query]);
  const filteredStations = useMemo(() => {
    if (!query) return stations;
    return stations.filter((station) => [station.code, station.name, station.routeId ?? ''].some((value) => value.toLowerCase().includes(query)));
  }, [stations, query]);

  return (
    <Card title="Tuyến và nhà ga">
      <div className="tabs">
        <button className={tab === 'routes' ? 'active' : ''} onClick={() => setTab('routes')}>Tuyến</button>
        <button className={tab === 'stations' ? 'active' : ''} onClick={() => setTab('stations')}>Nhà ga</button>
      </div>
      <label className="search-box full"><Search size={18}/><input placeholder="Tìm tuyến, nhà ga hoặc phương tiện..." value={search} onChange={(event) => setSearch(event.target.value)}/></label>
      {isLoading && <p>Đang tải dữ liệu tuyến...</p>}
      {error && <p className="danger" role="alert">{error}</p>}
      {tab === 'routes' && !isLoading && !error && filteredRoutes.length === 0 && <p>Không tìm thấy tuyến.</p>}
      {tab === 'stations' && !isLoading && !error && filteredStations.length === 0 && <p>Không tìm thấy nhà ga.</p>}
      <div className="route-list transit-list">
        {tab === 'routes'
          ? filteredRoutes.map((route) => (
            <div className="route-item transit-row" key={route.id}>
              <b className="route-code">{route.code}</b>
              <span className="route-name">{route.name}</span>
              <small className="route-mode">{route.type}</small>
              <em className={route.status === 'Đang hoạt động' ? 'success route-status' : 'warning route-status'}>{route.status}</em>
            </div>
          ))
          : filteredStations.map((station) => (
            <div className="route-item transit-row" key={station.id}>
              <b className="route-code">{station.code}</b>
              <span className="route-name">{station.name}</span>
              <small className="route-mode">{station.routeId || 'Chưa gắn tuyến'}</small>
              <em className="route-status">{station.sequence ? `Điểm dừng ${station.sequence}` : 'Nhà ga'}</em>
            </div>
          ))}
      </div>
    </Card>
  );
}
