import {
  AlertTriangle,
  Ban,
  CheckCircle2,
  Clock3,
  LoaderCircle,
  XCircle
} from "lucide-react";

const resultView = {
  VALID: {
    label: "Access granted",
    className: "valid",
    icon: CheckCircle2
  },
  USED: {
    label: "Already used",
    className: "denied",
    icon: XCircle
  },
  EXPIRED: {
    label: "Expired",
    className: "warning",
    icon: Clock3
  },
  CANCELLED: {
    label: "Cancelled",
    className: "denied",
    icon: Ban
  },
  INVALID: {
    label: "Invalid ticket",
    className: "denied",
    icon: XCircle
  },
  ERROR: {
    label: "Scanner error",
    className: "warning",
    icon: AlertTriangle
  }
};

export default function ValidationResult({ loading, result }) {
  if (loading) {
    return (
      <section className="result-panel waiting">
        <LoaderCircle className="spin" size={42} aria-hidden="true" />
        <h1>Validating</h1>
        <p>Checking ticket with the gate service</p>
      </section>
    );
  }

  if (!result) {
    return (
      <section className="result-panel idle">
        <CheckCircle2 size={42} aria-hidden="true" />
        <h1>Ready</h1>
        <p>Scan a QR code or enter a ticket code</p>
      </section>
    );
  }

  const view = resultView[result.status] ?? resultView.ERROR;
  const Icon = view.icon;

  return (
    <section className={`result-panel ${view.className}`}>
      <Icon size={52} aria-hidden="true" />
      <h1>{view.label}</h1>
      <p>{result.message}</p>

      <dl className="result-meta">
        <div>
          <dt>Ticket</dt>
          <dd>{result.ticketCode}</dd>
        </div>
        <div>
          <dt>Gate</dt>
          <dd>{result.gateId}</dd>
        </div>
        <div>
          <dt>Station</dt>
          <dd>{result.stationId}</dd>
        </div>
      </dl>
    </section>
  );
}
