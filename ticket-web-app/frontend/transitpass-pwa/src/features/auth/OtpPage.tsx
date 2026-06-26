import { useNavigate } from 'react-router-dom';

export function OtpPage() {
  const navigate = useNavigate();
  return (
    <div className="auth-page compact">
      <div className="auth-card center">
        <h2>Xác thực OTP</h2>
        <p>Mã OTP đã được gửi đến email của bạn.</p>
        <div className="otp-row">{['2','4','7','1','8','6'].map((n, i) => <input key={i} maxLength={1} defaultValue={n}/>)}</div>
        <small>Gửi lại mã sau 00:45</small>
        <button className="primary-button" onClick={() => navigate('/app')}>Xác nhận</button>
      </div>
    </div>
  );
}
