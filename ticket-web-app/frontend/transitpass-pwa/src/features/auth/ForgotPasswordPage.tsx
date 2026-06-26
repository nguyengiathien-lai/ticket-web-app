import { Link } from 'react-router-dom';
export function ForgotPasswordPage() {
  return <div className="auth-page compact"><form className="auth-card"><h2>Quên mật khẩu</h2><p>Nhập email để nhận liên kết đặt lại mật khẩu.</p><label>Email<input type="email" placeholder="email@example.com"/></label><button className="primary-button">Gửi liên kết</button><Link to="/login">Quay lại đăng nhập</Link></form></div>;
}
