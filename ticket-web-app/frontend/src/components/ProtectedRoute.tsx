import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getStoredAccount, isAdmin, isProfileComplete, isSessionValid, nextRouteFor } from '../services/authApi';

interface ProtectedRouteProps {
  role: 'admin' | 'passenger';
}

export function ProtectedRoute({ role }: ProtectedRouteProps) {
  const location = useLocation();
  const account = getStoredAccount();

  if (!account || !isSessionValid()) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  const accountIsAdmin = isAdmin(account);
  if (role === 'admin' && !accountIsAdmin) {
    return <Navigate to="/app" replace />;
  }

  if (role === 'passenger' && accountIsAdmin) {
    return <Navigate to={nextRouteFor(account)} replace />;
  }

  if (role === 'passenger' && !isProfileComplete(account) && location.pathname !== '/app/profile') {
    return <Navigate to="/app/profile" replace state={{ message: 'Vui lòng cập nhật đầy đủ hồ sơ trước khi sử dụng ứng dụng.' }} />;
  }

  return <Outlet />;
}
