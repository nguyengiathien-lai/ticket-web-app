import {
  AlertTriangle,
  Ban,
  CheckCircle2,
  Clock3,
  LoaderCircle,
  XCircle
} from "lucide-react";

const resultView = {
  SENT: {
    label: "Hợp lệ",
    className: "valid",
    icon: CheckCircle2
  },
  DELIVERY_FAILED: {
    label: "Gửi thất bại",
    className: "warning",
    icon: AlertTriangle
  },
  USED: {
    label: "Đã sử dụng",
    className: "denied",
    icon: XCircle
  },
  EXPIRED: {
    label: "Hết hạn",
    className: "warning",
    icon: Clock3
  },
  CANCELLED: {
    label: "Đã hủy",
    className: "denied",
    icon: Ban
  },
  INVALID: {
    label: "Không hợp lệ",
    className: "denied",
    icon: XCircle
  },
  ERROR: {
    label: "Lỗi máy quét",
    className: "warning",
    icon: AlertTriangle
  }
};

export default function ValidationResult({ loading, result }) {
  if (loading) {
    return (
      <section className="result-panel waiting">
        <LoaderCircle className="spin" size={42} aria-hidden="true" />
        <h1>Đang kiểm tra...</h1>
        <p>Soát vé tại thiết bị</p>
      </section>
    );
  }

  if (!result) {
    return (
      <section className="result-panel idle">
        <CheckCircle2 size={42} aria-hidden="true" />
        <h1>Sẵn sàng</h1>
        <p>Quét mã QR hoặc nhập mã vé</p>
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
          <dt>Mã vé</dt>
          <dd>{result.ticketId}</dd>
        </div>
        <div>
          <dt>Mã thiết bị</dt>
          <dd>{result.deviceCode}</dd>
        </div>
        <div>
          <dt>Mã trạm</dt>
          <dd>{result.stationCode}</dd>
        </div>
        <div>
          <dt>Loại thao tác</dt>
          <dd>{result.eventType?.replace("_", " ")}</dd>
        </div>
      </dl>
    </section>
  );
}
