import { BrowserMultiFormatReader } from "@zxing/browser";
import { Camera, Keyboard, Pause, Play, TrainFront } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { extractTicketCode } from "../api/ticketPayload";

export default function ScannerView({
  disabled,
  scannerOptions,
  configError,
  scannerContext,
  onScannerContextChange,
  onScan
}) {
  const videoRef = useRef(null);
  const controlsRef = useRef(null);
  const lastScanRef = useRef({ code: "", scannedAt: 0 });
  const [cameraError, setCameraError] = useState("");
  const [cameraStatus, setCameraStatus] = useState("Starting camera");
  const [lastPayload, setLastPayload] = useState("");
  const [manualCode, setManualCode] = useState("");
  const [running, setRunning] = useState(true);
  const hasScannerContext =
    scannerContext.deviceCode.trim() !== "" &&
    scannerContext.stationCode.trim() !== "" &&
    scannerContext.eventType !== "";

  const emitScan = useCallback(
    rawPayload => {
      setLastPayload(rawPayload.trim());
      const normalizedCode = extractTicketCode(rawPayload);
      const now = Date.now();

      if (!normalizedCode || disabled || !hasScannerContext) {
        return;
      }

      if (
        normalizedCode === lastScanRef.current.code &&
        now - lastScanRef.current.scannedAt < 2500
      ) {
        return;
      }

      lastScanRef.current = {
        code: normalizedCode,
        scannedAt: now
      };
      // onScan(normalizedCode);
      onScan(rawPayload);
    },
    [disabled, hasScannerContext, onScan]
  );

  useEffect(() => {
    if (!running) {
      controlsRef.current?.stop();
      return undefined;
    }

    let mounted = true;
    const reader = new BrowserMultiFormatReader();

    async function startCamera() {
      try {
        setCameraStatus("Point the rear camera at a QR code");

        controlsRef.current = await reader.decodeFromConstraints(
          {
            audio: false,
            video: {
              facingMode: { ideal: "environment" },
              width: { ideal: 1280 },
              height: { ideal: 720 }
            }
          },
          videoRef.current,
          result => {
            if (result) {
              setCameraStatus("QR code captured");
              emitScan(result.getText());
            }
          }
        );

        if (mounted) {
          setCameraError("");
        }
      } catch {
        try {
          controlsRef.current = await reader.decodeFromVideoDevice(
            undefined,
            videoRef.current,
            result => {
              if (result) {
                setCameraStatus("QR code captured");
                emitScan(result.getText());
              }
            }
          );

          if (mounted) {
            setCameraError("");
            setCameraStatus("Point the camera at a QR code");
          }
        } catch {
          if (mounted) {
            setCameraError(
              "Camera unavailable. Allow camera permission or use manual entry."
            );
            setCameraStatus("Camera unavailable");
          }
        }
      }
    }

    startCamera();

    return () => {
      mounted = false;
      controlsRef.current?.stop();
    };
  }, [emitScan, running]);

  function handleManualSubmit(event) {
    event.preventDefault();
    emitScan(manualCode);
    setManualCode("");
  }

  function updateScannerContext(field, value) {
    onScannerContextChange(current => {
      if (field === "stationCode") {
        const firstDevice = scannerOptions.find(
          option => option.stationCode === value
        );
        return {
          ...current,
          stationCode: value,
          deviceCode: firstDevice?.deviceCode ?? ""
        };
      }
      return { ...current, [field]: value };
    });
  }

  const stationCodes = [
    ...new Set(scannerOptions.map(option => option.stationCode))
  ];
  const deviceCodes = scannerOptions
    .filter(option => option.stationCode === scannerContext.stationCode)
    .map(option => option.deviceCode);

  return (
    <section className="scanner-shell">
      <header className="scanner-bar">
        <div className="scanner-heading">
          <Camera size={20} aria-hidden="true" />
          <span>Ticket scanner</span>
        </div>

        <button
          type="button"
          className="icon-button"
          onClick={() => setRunning(value => !value)}
          aria-label={running ? "Pause scanner" : "Resume scanner"}
          title={running ? "Pause scanner" : "Resume scanner"}
        >
          {running ? <Pause size={20} /> : <Play size={20} />}
        </button>
      </header>

      <section className="scanner-context" aria-label="Scanner context">
        <label>
          <span>Mã thiết bị</span>
          <select
            value={scannerContext.deviceCode}
            onChange={event => updateScannerContext("deviceCode", event.target.value)}
            disabled={disabled || deviceCodes.length === 0}
            required
          >
            {deviceCodes.length === 0 && <option value="">Không có thiết bị</option>}
            {deviceCodes.map(deviceCode => (
              <option key={deviceCode} value={deviceCode}>
                {deviceCode}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Mã trạm</span>
          <select
            value={scannerContext.stationCode}
            onChange={event => updateScannerContext("stationCode", event.target.value)}
            disabled={disabled || stationCodes.length === 0}
            required
          >
            {stationCodes.length === 0 && <option value="">Không có trạm</option>}
            {stationCodes.map(stationCode => (
              <option key={stationCode} value={stationCode}>
                {stationCode}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Tap mode</span>
          <select
            value={scannerContext.eventType}
            onChange={event => updateScannerContext("eventType", event.target.value)}
            disabled={disabled}
          >
            <option value="TAP_IN">Tap in</option>
            <option value="TAP_OUT">Tap out</option>
          </select>
        </label>

        <div className="scanner-context-badge">
          <TrainFront size={18} aria-hidden="true" />
          <strong>{scannerContext.stationCode}</strong>
          <span>{scannerContext.deviceCode}</span>
        </div>
      </section>
      {configError && <div className="camera-error">{configError}</div>}

      <div className="camera-frame">
        <video ref={videoRef} className="camera-feed" muted playsInline />
        <div className="scan-target" aria-hidden="true" />
        <div className="camera-status">{cameraStatus}</div>
        {cameraError && <div className="camera-error">{cameraError}</div>}
      </div>

      {lastPayload && (
        <div className="decoded-payload">
          <span>Mã QR</span>
          <code>{lastPayload}</code>
        </div>
      )}

      <form className="manual-entry" onSubmit={handleManualSubmit}>
        <Keyboard size={18} aria-hidden="true" />
        <input
          value={manualCode}
          onChange={event => setManualCode(event.target.value)}
          placeholder="Nhập mã vé"
          disabled={disabled}
        />
        <button
          type="submit"
          disabled={disabled || !hasScannerContext || !manualCode.trim()}
        >
          Kiểm tra
        </button>
      </form>
    </section>
  );
}
