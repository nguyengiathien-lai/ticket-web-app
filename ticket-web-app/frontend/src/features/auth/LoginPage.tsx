import { FormEvent, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Bus, MapPinned, ShieldCheck, Ticket, TrainFront } from 'lucide-react';
import { useToast } from '../../components/ToastProvider';
import { getStoredAccount, isSessionValid, login, nextRouteFor, storeSession } from '../../services/authApi';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as { from?: string; message?: string } | null;
  const [email, setEmail] = useState('nguyenvana@gmail.com');
  const [password, setPassword] = useState('12345678');
  const [error, setError] = useState('');
  const [message, setMessage] = useState(state?.message ?? '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const toast = useToast();

  useEffect(() => {
    const account = getStoredAccount();
    if (account && isSessionValid()) {
      navigate(nextRouteFor(account), { replace: true });
    }
  }, [navigate]);

  useEffect(() => {
    if (message) {
      toast.success(message);
    }
  }, [message, toast]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setMessage('');
    setIsSubmitting(true);

    try {
      const session = await login(email.trim(), password);
      storeSession(session);
      toast.success('Đăng nhập thành công.');
      navigate(state?.from ?? nextRouteFor(session.account), { replace: true });
    } catch (exception) {
      const reason = exception instanceof Error ? exception.message : 'Không thể đăng nhập. Vui lòng thử lại.';
      setError(reason);
      toast.error(reason);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-panel intro-panel">
        <div className="intro-overlay">
          <div className="brand large auth-hero-brand"><Bus size={34}/> TransitPass</div>
          {/* <div className="metro-pill"><TrainFront size={18}/> Metro City Line</div> */}
          <h1>Di chuyển thông minh<br/>Kết nối mọi hành trình</h1>
          {/* <p>Ứng dụng mua vé và thẻ giao thông công cộng cho hành khách và quản trị viên.</p>
          <div className="auth-feature-row" aria-label="TransitPass highlights">
            <span><Ticket size={18}/> Vé điện tử</span>
            <span><MapPinned size={18}/> Tuyến metro</span>
            <span><ShieldCheck size={18}/> Bảo mật</span>
          </div> */}
        </div>
      </div>
      <form className="auth-card" onSubmit={handleSubmit}>
        <div className="auth-card-badge"><TrainFront size={18}/> TransitPass</div>
        <h2>Đăng nhập</h2>
        <label>
          Địa chỉ email
          <input
            type="email"
            placeholder="Nhập email"
            value={email}
            autoComplete="email"
            onChange={(event) => setEmail(event.target.value)}
            disabled={isSubmitting}
            required
          />
        </label>
        <label>
          Mật khẩu
          <input
            type="password"
            placeholder="Nhập mật khẩu"
            value={password}
            autoComplete="current-password"
            onChange={(event) => setPassword(event.target.value)}
            disabled={isSubmitting}
            required
          />
        </label>
        {message && <p className="success" role="status">{message}</p>}
        {error && <p className="danger" role="alert">{error}</p>}
        <button className="primary-button" disabled={isSubmitting}>
          {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
        <p>Chưa có tài khoản? <Link to="/register">Đăng ký</Link></p>
        <Link to="/forgot-password">Quên mật khẩu?</Link>
      </form>
    </div>
  );
}
