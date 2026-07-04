import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { registerAccount } from '../../services/authApi';

export function RegisterPage() {
  const navigate = useNavigate();
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');

    if (!acceptedTerms) {
      setError('Vui lòng đồng ý với điều khoản sử dụng trước khi tạo tài khoản.');
      return;
    }

    setIsSubmitting(true);
    try {
      const trimmedEmail = email.trim();
      await registerAccount({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: trimmedEmail,
        password
      });
      navigate('/verify-otp', { replace: true, state: { email: trimmedEmail } });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Không thể tạo tài khoản. Vui lòng thử lại.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="auth-page compact">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h2>Tạo tài khoản hành khách</h2>
        <label>
          Tên
          <input
            placeholder="Văn A"
            value={firstName}
            onChange={(event) => setFirstName(event.target.value)}
            disabled={isSubmitting}
            required
            maxLength={50}
          />
        </label>
        <label>
          Họ
          <input
            placeholder="Nguyễn"
            value={lastName}
            onChange={(event) => setLastName(event.target.value)}
            disabled={isSubmitting}
            required
            maxLength={50}
          />
        </label>
        <label>
          Địa chỉ email
          <input
            type="email"
            placeholder="email@example.com"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            disabled={isSubmitting}
            required
            maxLength={50}
            autoComplete="email"
          />
        </label>
        <label>
          Mật khẩu
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            disabled={isSubmitting}
            required
            minLength={8}
            maxLength={72}
            autoComplete="new-password"
          />
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={acceptedTerms}
            onChange={(event) => setAcceptedTerms(event.target.checked)}
            disabled={isSubmitting}
          />
          Tôi đồng ý với điều khoản sử dụng
        </label>
        {error && <p className="danger" role="alert">{error}</p>}
        <button className="primary-button" disabled={isSubmitting}>
          {isSubmitting ? 'Đang tạo tài khoản...' : 'Đăng ký'}
        </button>
        <p>Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
      </form>
    </div>
  );
}
