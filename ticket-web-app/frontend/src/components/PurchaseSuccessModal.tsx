import { useEffect } from 'react';
import { CheckCircle2, X } from 'lucide-react';

export interface PurchaseDetail {
  label: string;
  value?: string | number | null;
}

interface Props {
  title: string;
  message: string;
  details: PurchaseDetail[];
  onClose: () => void;
}

export function PurchaseSuccessModal({ title, message, details, onClose }: Props) {
  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', closeOnEscape);
    return () => document.removeEventListener('keydown', closeOnEscape);
  }, [onClose]);

  return (
    <div className="purchase-modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="purchase-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="purchase-success-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button className="purchase-modal-close" type="button" onClick={onClose} aria-label="Đóng">
          <X size={22} />
        </button>
        <CheckCircle2 className="purchase-modal-icon" size={64} aria-hidden="true" />
        <h2 id="purchase-success-title">{title}</h2>
        <p>{message}</p>
        <dl className="purchase-modal-details">
          {details
            .filter(({ value }) => value !== undefined && value !== null && value !== '')
            .map(({ label, value }) => (
              <div key={label}>
                <dt>{label}</dt>
                <dd>{value}</dd>
              </div>
            ))}
        </dl>
        <button className="primary-button" type="button" onClick={onClose}>Hoàn tất</button>
      </section>
    </div>
  );
}
