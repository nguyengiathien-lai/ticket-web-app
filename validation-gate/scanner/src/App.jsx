import { useCallback, useMemo, useState } from "react";
import { validateTicket } from "./api/gateApi";
import ScannerView from "./components/ScannerView";
import ValidationResult from "./components/ValidationResult";

const DEVICE_CODE = import.meta.env.VITE_DEVICE_CODE ?? "gate-device-1";
const STATION_CODE = import.meta.env.VITE_STATION_CODE ?? "station-1";
const EVENT_TYPE = import.meta.env.VITE_GATE_EVENT_TYPE ?? "TAP_IN";

export default function App() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [scannerContext, setScannerContext] = useState({
    deviceCode: DEVICE_CODE,
    stationCode: STATION_CODE,
    eventType: EVENT_TYPE
  });

  const gateLabel = useMemo(
    () => `${scannerContext.stationCode} / ${scannerContext.deviceCode}`,
    [scannerContext.deviceCode, scannerContext.stationCode]
  );

  const handleScan = useCallback(async qrPayload => {
    setLoading(true);

    try {
      const validation = await validateTicket({
        qrPayload,
        deviceCode: scannerContext.deviceCode,
        stationCode: scannerContext.stationCode,
        eventType: scannerContext.eventType
      });

      setResult(validation);
      setHistory(items => [validation, ...items].slice(0, 6));
    } catch (error) {
      const failedResult = {
        status: "ERROR",
        ticketId: qrPayload,
        qrPayload,
        deviceCode: scannerContext.deviceCode,
        stationCode: scannerContext.stationCode,
        eventType: scannerContext.eventType,
        message: error.message
      };

      setResult(failedResult);
      setHistory(items => [failedResult, ...items].slice(0, 6));
    } finally {
      setLoading(false);
    }
  }, [scannerContext.deviceCode, scannerContext.eventType, scannerContext.stationCode]);

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
          <span>Active scanner</span>
          <strong>{gateLabel}</strong>
          <em>{scannerContext.eventType.replace("_", " ")}</em>
        </div>

        <ValidationResult loading={loading} result={result} />

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
