import { ChangeEvent, FormEvent, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { resendEmailOtp, verifyEmailOtp } from '../../services/authApi';

export function OtpPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const initialEmail = useMemo(() => {
    const state = location.state as { email?: string } | null;
    return state?.email ?? '';
  }, [location.state]);
  const [email, setEmail] = useState(initialEmail);
  const [digits, setDigits] = useState(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResending, setIsResending] = useState(false);

  function handleDigitChange(index: number, event: ChangeEvent<HTMLInputElement>) {
    const value = event.target.value.replace(/\D/g, '').slice(-1);
    const nextDigits = [...digits];
    nextDigits[index] = value;
    setDigits(nextDigits);

    if (value && index < digits.length - 1) {
      const nextInput = event.target.form?.elements.namedItem(`otp-${index + 1}`) as HTMLInputElement | null;
      nextInput?.focus();
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setMessage('');

    const code = digits.join('');
    if (code.length !== 6) {
      setError('Vui lòng nhập đủ mã xác thực gồm 6 chữ số.');
      return;
    }

    setIsSubmitting(true);
    try {
      await verifyEmailOtp(email.trim(), code);
      navigate('/login', {
        replace: true,
        state: { message: 'Xác thực email thành công. Bạn có thể đăng nhập ngay.' }
      });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Không thể xác thực email. Vui lòng thử lại.');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleResend() {
    setError('');
    setMessage('');
    setIsResending(true);
    try {
      await resendEmailOtp(email.trim());
      setMessage('Mã xác thực mới đã được gửi.');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Không thể gửi lại mã xác thực.');
    } finally {
      setIsResending(false);
    }
  }

  return (
    <div className="auth-page compact">
      <form className="auth-card center" onSubmit={handleSubmit}>
        <h2>Xác thực email</h2>
        <p>Nhập mã OTP gồm 6 chữ số đã gửi đến email của bạn.</p>
        <label>
          Địa chỉ email
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            disabled={isSubmitting}
            required
            autoComplete="email"
          />
        </label>
        <div className="otp-row">
          {digits.map((digit, index) => (
            <input
              key={index}
              name={`otp-${index}`}
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={1}
              value={digit}
              onChange={(event) => handleDigitChange(index, event)}
              disabled={isSubmitting}
              aria-label={`Chữ số OTP thứ ${index + 1}`}
              required
            />
          ))}
        </div>
        {message && <p className="success" role="status">{message}</p>}
        {error && <p className="danger" role="alert">{error}</p>}
        <button className="primary-button" disabled={isSubmitting}>
          {isSubmitting ? 'Đang xác thực...' : 'Xác thực'}
        </button>
        <button
          type="button"
          className="primary-button secondary-button"
          onClick={handleResend}
          disabled={isSubmitting || isResending || !email.trim()}
        >
          {isResending ? 'Đang gửi...' : 'Gửi lại mã'}
        </button>
        <Link to="/login">Quay lại đăng nhập</Link>
      </form>
    </div>
  );
}
