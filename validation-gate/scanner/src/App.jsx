import { useCallback, useEffect, useMemo, useState } from "react";
import { getScannerOptions, validateTicket } from "./api/gateApi";
import ScannerView from "./components/ScannerView";
import ValidationResult from "./components/ValidationResult";

export default function App() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [scannerOptions, setScannerOptions] = useState([]);
  const [configError, setConfigError] = useState("");
  const [scannerContext, setScannerContext] = useState({
    deviceCode: "",
    stationCode: "",
    eventType: "TAP_IN"
  });

  useEffect(() => {
    let active = true;

    getScannerOptions()
      .then(options => {
        if (!active) return;
        setScannerOptions(options);
        if (options.length > 0) {
          setScannerContext(current => ({
            ...current,
            stationCode: options[0].stationCode,
            deviceCode: options[0].deviceCode
          }));
        }
      })
      .catch(error => {
        if (active) setConfigError(error.message);
      });

    return () => {
      active = false;
    };
  }, []);

  const gateLabel = useMemo(
    () => `${scannerContext.stationCode} / ${scannerContext.deviceCode}`,
    [scannerContext.deviceCode, scannerContext.stationCode]
  );

  const handleScan = useCallback(async qrPayload => {
    setLoading(true);

    try {
      const validation = await validateTicket({
        qrPayload,
        deviceCode: scannerContext.deviceCode.trim(),
        stationCode: scannerContext.stationCode.trim(),
        eventType: scannerContext.eventType
      });

      setResult(validation);
      setHistory(items => [validation, ...items].slice(0, 6));
    } catch (error) {
      const failedResult = {
        status: "ERROR",
        ticketId: qrPayload,
        qrPayload,
        deviceCode: scannerContext.deviceCode.trim(),
        stationCode: scannerContext.stationCode.trim(),
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
        scannerOptions={scannerOptions}
        configError={configError}
        scannerContext={scannerContext}
        onScannerContextChange={setScannerContext}
        onScan={handleScan}
      />

      <aside className="gate-panel">
        <div className="gate-status">
          <span>Đang hoạt động</span>
          <strong>{gateLabel}</strong>
          <em>{scannerContext.eventType.replace("_", " ")}</em>
        </div>

        <ValidationResult loading={loading} result={result} />

        <section className="history-panel" aria-label="Recent scans">
          <h2>Lượt quét gần đây</h2>
          {history.length === 0 ? (
            <p>Chưa có lượt quét nào</p>
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
