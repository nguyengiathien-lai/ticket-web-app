import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './layouts/AppLayout';
import { LoginPage } from './features/auth/LoginPage';
import { RegisterPage } from './features/auth/RegisterPage';
import { OtpPage } from './features/auth/OtpPage';
import { ForgotPasswordPage } from './features/auth/ForgotPasswordPage';
import { DashboardPage } from './features/passenger/DashboardPage';
import { BuyTicketPage } from './features/passenger/BuyTicketPage';
import { MyCardPage } from './features/passenger/MyCardPage';
import { HistoryPage } from './features/passenger/HistoryPage';
import { RoutesPage } from './features/passenger/RoutesPage';
import { ProfilePage } from './features/passenger/ProfilePage';
import { NotificationsPage } from './features/passenger/NotificationsPage';
import { AdminDashboardPage } from './features/admin/AdminDashboardPage';
import { UserManagementPage } from './features/admin/UserManagementPage';
import { LoginHistoryPage } from './features/admin/LoginHistoryPage';
import { ReportsPage } from './features/admin/ReportsPage';
import { SettingsPage } from './features/admin/SettingsPage';

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-otp" element={<OtpPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route element={<AppLayout />}>
        <Route path="/app" element={<DashboardPage />} />
        <Route path="/app/buy-ticket" element={<BuyTicketPage />} />
        <Route path="/app/my-card" element={<MyCardPage />} />
        <Route path="/app/history" element={<HistoryPage />} />
        <Route path="/app/routes" element={<RoutesPage />} />
        <Route path="/app/notifications" element={<NotificationsPage />} />
        <Route path="/app/profile" element={<ProfilePage />} />
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/users" element={<UserManagementPage />} />
        <Route path="/admin/login-history" element={<LoginHistoryPage />} />
        <Route path="/admin/reports" element={<ReportsPage />} />
        <Route path="/admin/settings" element={<SettingsPage />} />
      </Route>
    </Routes>
  );
}
