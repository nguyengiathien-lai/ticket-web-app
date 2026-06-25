import { BrowserMultiFormatReader } from "@zxing/browser";
import { Camera, Keyboard, Pause, Play } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { extractTicketCode } from "../api/ticketPayload";

export default function ScannerView({ disabled, onScan }) {
  const videoRef = useRef(null);
  const controlsRef = useRef(null);
  const lastScanRef = useRef({ code: "", scannedAt: 0 });
  const [cameraError, setCameraError] = useState("");
  const [cameraStatus, setCameraStatus] = useState("Starting camera");
  const [lastPayload, setLastPayload] = useState("");
  const [manualCode, setManualCode] = useState("");
  const [running, setRunning] = useState(true);

  const emitScan = useCallback(
    rawPayload => {
      setLastPayload(rawPayload.trim());
      const normalizedCode = extractTicketCode(rawPayload);
      const now = Date.now();

      if (!normalizedCode || disabled) {
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
    [disabled, onScan]
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

  return (
    <section className="scanner-shell">
      <header className="scanner-bar">
        <div className="scanner-heading">
          <Camera size={20} aria-hidden="true" />
          <span>Ticket gate</span>
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

      <div className="camera-frame">
        <video ref={videoRef} className="camera-feed" muted playsInline />
        <div className="scan-target" aria-hidden="true" />
        <div className="camera-status">{cameraStatus}</div>
        {cameraError && <div className="camera-error">{cameraError}</div>}
      </div>

      {lastPayload && (
        <div className="decoded-payload">
          <span>Decoded QR</span>
          <code>{lastPayload}</code>
        </div>
      )}

      <form className="manual-entry" onSubmit={handleManualSubmit}>
        <Keyboard size={18} aria-hidden="true" />
        <input
          value={manualCode}
          onChange={event => setManualCode(event.target.value)}
          placeholder="Manual ticket code"
          disabled={disabled}
        />
        <button type="submit" disabled={disabled || !manualCode.trim()}>
          Check
        </button>
      </form>
    </section>
  );
}
