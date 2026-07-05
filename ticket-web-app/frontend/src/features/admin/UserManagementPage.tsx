import { useEffect, useMemo, useState } from 'react';
import { Card } from '../../components/Card';
import { useToast } from '../../components/ToastProvider';
import { getStoredAccount, type AccountResponse } from '../../services/authApi';
import {
  activateAdminAccount,
  deactivateAdminAccount,
  deleteAdminAccount,
  getAdminAccounts
} from '../../services/adminUserApi';

export function UserManagementPage() {
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [pendingDelete, setPendingDelete] = useState<AccountResponse | null>(null);
  const [busyAccountId, setBusyAccountId] = useState('');
  const currentAccount = getStoredAccount();
  const toast = useToast();

  useEffect(() => {
    loadAccounts();
  }, []);

  const sortedAccounts = useMemo(() => {
    return [...accounts].sort((first, second) => {
      return (second.createdAt || '').localeCompare(first.createdAt || '');
    });
  }, [accounts]);

  async function loadAccounts() {
    setLoading(true);
    setError('');

    try {
      setAccounts(await getAdminAccounts());
    } catch (exception) {
      const reason = toMessage(exception, 'Không thể tải danh sách người dùng.');
      setError(reason);
      toast.error(reason);
    } finally {
      setLoading(false);
    }
  }

  async function toggleAccountStatus(account: AccountResponse) {
    setBusyAccountId(account.id);
    setError('');
    setMessage('');

    try {
      const updatedAccount = account.isActive
        ? await deactivateAdminAccount(account.id)
        : await activateAdminAccount(account.id);
      const notice = account.isActive ? 'Đã khóa tài khoản.' : 'Đã kích hoạt tài khoản.';

      setAccounts((current) => current.map((item) => item.id === updatedAccount.id ? updatedAccount : item));
      setMessage(notice);
      toast.success(notice);
    } catch (exception) {
      const reason = toMessage(exception, 'Không thể cập nhật trạng thái tài khoản.');
      setError(reason);
      toast.error(reason);
    } finally {
      setBusyAccountId('');
    }
  }

  async function confirmDeleteAccount() {
    if (!pendingDelete) {
      return;
    }

    setBusyAccountId(pendingDelete.id);
    setError('');
    setMessage('');

    try {
      await deleteAdminAccount(pendingDelete.id);
      setAccounts((current) => current.filter((account) => account.id !== pendingDelete.id));
      setMessage('Đã xóa tài khoản.');
      toast.success('Đã xóa tài khoản.');
      setPendingDelete(null);
    } catch (exception) {
      const reason = toMessage(exception, 'Không thể xóa tài khoản.');
      setError(reason);
      toast.error(reason);
    } finally {
      setBusyAccountId('');
    }
  }

  return (
    <Card title="Quản lý người dùng">
      {message && <p className="success" role="status">{message}</p>}
      {error && <p className="danger" role="alert">{error}</p>}
      {pendingDelete && (
        <div className="confirm-notification" role="alertdialog" aria-labelledby="delete-account-title">
          <div>
            <strong id="delete-account-title">Xác nhận xóa tài khoản</strong>
            <p>Bạn có chắc muốn xóa tài khoản {displayName(pendingDelete)} không?</p>
          </div>
          <div className="table-actions">
            <button
              className="secondary-button fit"
              type="button"
              onClick={() => setPendingDelete(null)}
              disabled={busyAccountId === pendingDelete.id}
            >
              Hủy
            </button>
            <button
              className="danger-button fit"
              type="button"
              onClick={confirmDeleteAccount}
              disabled={busyAccountId === pendingDelete.id}
            >
              Xóa
            </button>
          </div>
        </div>
      )}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Họ tên</th>
              <th>Địa chỉ email</th>
              <th>SĐT</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={5}>Đang tải danh sách người dùng...</td>
              </tr>
            )}
            {!loading && sortedAccounts.length === 0 && (
              <tr>
                <td colSpan={5}>Chưa có người dùng nào.</td>
              </tr>
            )}
            {!loading && sortedAccounts.map((account) => (
              <tr key={account.id}>
                <td>{displayName(account)}</td>
                <td>{account.email}</td>
                <td>{account.phoneNumber || 'Chưa cập nhật'}</td>
                <td className={account.isActive ? 'success' : 'danger'}>
                  {account.isActive ? 'Hoạt động' : 'Khóa'}
                </td>
                <td>
                  {currentAccount?.id === account.id ? (
                    <span className="muted-text">Tài khoản hiện tại</span>
                  ) : (
                    <div className="table-actions">
                      <button
                        className={account.isActive ? 'warning-button fit' : 'secondary-button fit'}
                        type="button"
                        onClick={() => toggleAccountStatus(account)}
                        disabled={busyAccountId === account.id}
                      >
                        {account.isActive ? 'Khóa' : 'Kích hoạt'}
                      </button>
                      <button
                        className="danger-button fit"
                        type="button"
                        onClick={() => setPendingDelete(account)}
                        disabled={busyAccountId === account.id}
                      >
                        Xóa
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

function displayName(account: AccountResponse) {
  return account.fullName || account.email;
}

function toMessage(exception: unknown, fallback: string) {
  return exception instanceof Error ? exception.message : fallback;
}
