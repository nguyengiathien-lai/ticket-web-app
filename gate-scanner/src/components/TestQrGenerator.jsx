import QRCode from "qrcode";
import { Download, QrCode } from "lucide-react";
import { useEffect, useState } from "react";

const presets = ["TICKET-123", "USED-123", "EXPIRED-123", "CANCELLED-123", "BAD-123"];

export default function TestQrGenerator() {
  const [payload, setPayload] = useState("TICKET-123");
  const [qrDataUrl, setQrDataUrl] = useState("");

  useEffect(() => {
    let active = true;

    QRCode.toDataURL(payload, {
      errorCorrectionLevel: "M",
      margin: 2,
      scale: 8,
      color: {
        dark: "#020617",
        light: "#ffffff"
      }
    }).then(dataUrl => {
      if (active) {
        setQrDataUrl(dataUrl);
      }
    });

    return () => {
      active = false;
    };
  }, [payload]);

  return (
    <section className="test-qr-panel" aria-label="Test QR generator">
      <div className="panel-heading">
        <QrCode size={18} aria-hidden="true" />
        <h2>Test QR</h2>
      </div>

      <div className="qr-preview">
        {qrDataUrl && <img src={qrDataUrl} alt={`QR code for ${payload}`} />}
      </div>

      <input
        value={payload}
        onChange={event => setPayload(event.target.value)}
        aria-label="QR payload"
      />

      <div className="preset-row">
        {presets.map(value => (
          <button key={value} type="button" onClick={() => setPayload(value)}>
            {value}
          </button>
        ))}
      </div>

      {qrDataUrl && (
        <a className="download-link" href={qrDataUrl} download={`${payload}.png`}>
          <Download size={16} aria-hidden="true" />
          Download PNG
        </a>
      )}
    </section>
  );
}
