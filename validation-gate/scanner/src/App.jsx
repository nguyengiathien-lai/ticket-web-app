import { useCallback, useMemo, useState } from "react";
import { validateTicket } from "./api/gateApi";
import ScannerView from "./components/ScannerView";
import TestQrGenerator from "./components/TestQrGenerator";
import ValidationResult from "./components/ValidationResult";

const GATE_ID = import.meta.env.VITE_GATE_ID ?? "GATE-01";
const STATION_ID = import.meta.env.VITE_STATION_ID ?? "STATION-01";
const EVENT_TYPE = import.meta.env.VITE_GATE_EVENT_TYPE ?? "CHECK_IN";

export default function App() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [scannerContext, setScannerContext] = useState({
    gateId: GATE_ID,
    stationId: STATION_ID,
    eventType: EVENT_TYPE
  });

  const gateLabel = useMemo(
    () => `${scannerContext.stationId} / ${scannerContext.gateId}`,
    [scannerContext.gateId, scannerContext.stationId]
  );

  const handleScan = useCallback(async qrPayload => {
    setLoading(true);

    try {
      const validation = await validateTicket({
        qrPayload,
        gateId: scannerContext.gateId,
        stationId: scannerContext.stationId,
        eventType: scannerContext.eventType
      });

      setResult(validation);
      setHistory(items => [validation, ...items].slice(0, 6));
    } catch (error) {
      const failedResult = {
        status: "ERROR",
        ticketId: qrPayload,
        gateId: scannerContext.gateId,
        stationId: scannerContext.stationId,
        eventType: scannerContext.eventType,
        message: error.message
      };

      setResult(failedResult);
      setHistory(items => [failedResult, ...items].slice(0, 6));
    } finally {
      setLoading(false);
    }
  }, [scannerContext.eventType, scannerContext.gateId, scannerContext.stationId]);

  return (
    <main className="app-shell">
      <ScannerView
        disabled={loading}
        scannerContext={scannerContext}
        onScannerContextChange={setScannerContext}
        onScan={handleScan}
      />

      <aside className="gate-panel">
        <div className="gate-status">
          <span>Active gate</span>
          <strong>{gateLabel}</strong>
          <em>{scannerContext.eventType.replace("_", " ")}</em>
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
                <li key={`${item.ticketId}-${index}`}>
                  <span className={`history-status ${item.status.toLowerCase()}`}>
                    {item.status}
                  </span>
                  <span>{item.ticketId}</span>
                </li>
              ))}
            </ol>
          )}
        </section>
      </aside>
    </main>
  );
}
