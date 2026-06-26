import { Link, useNavigate } from 'react-router-dom';

export function RegisterPage() {
  const navigate = useNavigate();
  return (
    <div className="auth-page compact">
      <form className="auth-card" onSubmit={(e) => { e.preventDefault(); navigate('/verify-otp'); }}>
        <h2>Tạo tài khoản hành khách</h2>
        <label>Họ và tên<input placeholder="Nguyễn Văn A"/></label>
        <label>Email<input type="email" placeholder="email@example.com"/></label>
        <label>Số điện thoại<input placeholder="0901 234 567"/></label>
        <label>Mật khẩu<input type="password"/></label>
        <label className="checkbox"><input type="checkbox"/> Tôi đồng ý với điều khoản sử dụng</label>
        <button className="primary-button">Đăng ký</button>
        <p>Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
      </form>
    </div>
  );
}
