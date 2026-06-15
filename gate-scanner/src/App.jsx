import { useCallback, useMemo, useState } from "react";
import { validateTicket } from "./api/gateApi";
import ScannerView from "./components/ScannerView";
import TestQrGenerator from "./components/TestQrGenerator";
import ValidationResult from "./components/ValidationResult";

const GATE_ID = import.meta.env.VITE_GATE_ID ?? "GATE-01";
const STATION_ID = import.meta.env.VITE_STATION_ID ?? "STATION-01";

export default function App() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);

  const gateLabel = useMemo(() => `${STATION_ID} / ${GATE_ID}`, []);

  const handleScan = useCallback(async ticketCode => {
    setLoading(true);

    try {
      const validation = await validateTicket({
        ticketCode,
        gateId: GATE_ID,
        stationId: STATION_ID
      });

      setResult(validation);
      setHistory(items => [validation, ...items].slice(0, 6));
    } catch (error) {
      const failedResult = {
        status: "ERROR",
        ticketCode,
        gateId: GATE_ID,
        stationId: STATION_ID,
        message: error.message
      };

      setResult(failedResult);
      setHistory(items => [failedResult, ...items].slice(0, 6));
    } finally {
      setLoading(false);
    }
  }, []);

  return (
    <main className="app-shell">
      <ScannerView disabled={loading} onScan={handleScan} />

      <aside className="gate-panel">
        <div className="gate-status">
          <span>Active gate</span>
          <strong>{gateLabel}</strong>
        </div>

        <ValidationResult loading={loading} result={result} />

        <TestQrGenerator />

        <section className="history-panel" aria-label="Recent scans">
          <h2>Recent scans</h2>
          {history.length === 0 ? (
            <p>No scans yet</p>
          ) : (
            <ol>
              {history.map((item, index) => (
                <li key={`${item.ticketCode}-${index}`}>
                  <span className={`history-status ${item.status.toLowerCase()}`}>
                    {item.status}
                  </span>
                  <span>{item.ticketCode}</span>
                </li>
              ))}
            </ol>
          )}
        </section>
      </aside>
    </main>
  );
}
