import { FormEvent, useMemo, useState } from 'react';
import { Card } from '../../components/Card';
import { getStoredAccount, updateProfile } from '../../services/authApi';

export function ProfilePage() {
  const account = getStoredAccount();
  const [fullName, setFullName] = useState(account?.fullName ?? '');
  const [phoneNumber, setPhoneNumber] = useState(account?.phoneNumber ?? '');
  const [dateOfBirth, setDateOfBirth] = useState(account?.dateOfBirth ?? '');
  const [gender, setGender] = useState(account?.gender ?? '');
  const [address, setAddress] = useState(account?.address ?? '');
  const [personalId, setPersonalId] = useState(account?.personalId ?? '');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const initials = useMemo(() => {
    const source = fullName.trim() || account?.email || 'U';
    return source
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');
  }, [account?.email, fullName]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage('');
    setError('');

    if (!account) {
      setError('Bạn cần đăng nhập trước khi cập nhật hồ sơ.');
      return;
    }

    setIsSubmitting(true);
    try {
      const updated = await updateProfile(account.id, {
        fullName,
        phoneNumber,
        personalId,
        address,
        dateOfBirth: dateOfBirth || null,
        gender
      });
      setFullName(updated.fullName ?? '');
      setPhoneNumber(updated.phoneNumber ?? '');
      setDateOfBirth(updated.dateOfBirth ?? '');
      setGender(updated.gender ?? '');
      setAddress(updated.address ?? '');
      setPersonalId(updated.personalId ?? '');
      setMessage('Cập nhật hồ sơ thành công.');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Không thể cập nhật hồ sơ. Vui lòng thử lại.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Card title="Hồ sơ">
      <form onSubmit={handleSubmit}>
        <div className="profile-head">
          <div className="avatar large-avatar">{initials}</div>
          <div>
            <h2>{fullName || 'Hành khách'}</h2>
            <p>{account?.email}</p>
          </div>
        </div>
        <div className="form-grid">
          <label>
            Họ và tên
            <input value={fullName} onChange={(event) => setFullName(event.target.value)} disabled={isSubmitting} />
          </label>
          <label>
            Số điện thoại
            <input value={phoneNumber} onChange={(event) => setPhoneNumber(event.target.value)} disabled={isSubmitting} />
          </label>
          <label>
            Ngày sinh
            <input type="date" value={dateOfBirth} onChange={(event) => setDateOfBirth(event.target.value)} disabled={isSubmitting} />
          </label>
          <label>
            Giới tính
            <input value={gender} onChange={(event) => setGender(event.target.value)} disabled={isSubmitting} />
          </label>
          <label>
            Địa chỉ
            <input value={address} onChange={(event) => setAddress(event.target.value)} disabled={isSubmitting} />
          </label>
          <label>
            Số giấy tờ cá nhân
            <input value={personalId} onChange={(event) => setPersonalId(event.target.value)} disabled={isSubmitting} />
          </label>
        </div>
        {message && <p className="success" role="status">{message}</p>}
        {error && <p className="danger" role="alert">{error}</p>}
        <button className="primary-button fit" disabled={isSubmitting}>
          {isSubmitting ? 'Đang cập nhật...' : 'Cập nhật hồ sơ'}
        </button>
      </form>
    </Card>
  );
}
