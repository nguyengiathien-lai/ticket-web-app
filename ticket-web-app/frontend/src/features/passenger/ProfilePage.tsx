import { FormEvent, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card } from '../../components/Card';
import { useToast } from '../../components/ToastProvider';
import { getStoredAccount, isProfileComplete, updateProfile } from '../../services/authApi';

interface RequiredProfileInput {
  fullName: string;
  phoneNumber: string;
  dateOfBirth: string;
  gender: string;
  address: string;
  personalId: string;
}

export function ProfilePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as { message?: string } | null;
  const account = getStoredAccount();
  const [fullName, setFullName] = useState(account?.fullName ?? '');
  const [phoneNumber, setPhoneNumber] = useState(account?.phoneNumber ?? '');
  const [dateOfBirth, setDateOfBirth] = useState(account?.dateOfBirth ?? '');
  const [gender, setGender] = useState(account?.gender ?? '');
  const [address, setAddress] = useState(account?.address ?? '');
  const [personalId, setPersonalId] = useState(account?.personalId ?? '');
  const [message, setMessage] = useState(state?.message ?? '');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const toast = useToast();

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
      const reason = 'Bạn cần đăng nhập trước khi cập nhật hồ sơ.';
      setError(reason);
      toast.error(reason);
      return;
    }

    if (!isRequiredProfileInputComplete({ fullName, phoneNumber, dateOfBirth, gender, address, personalId })) {
      const reason = 'Vui lòng cập nhật đầy đủ thông tin hồ sơ trước khi sử dụng ứng dụng.';
      setError(reason);
      toast.error(reason);
      return;
    }

    setIsSubmitting(true);
    try {
      const updated = await updateProfile(account.id, {
        fullName: fullName.trim(),
        phoneNumber: phoneNumber.trim(),
        personalId: personalId.trim(),
        address: address.trim(),
        dateOfBirth,
        gender: gender.trim()
      });
      setFullName(updated.fullName ?? '');
      setPhoneNumber(updated.phoneNumber ?? '');
      setDateOfBirth(updated.dateOfBirth ?? '');
      setGender(updated.gender ?? '');
      setAddress(updated.address ?? '');
      setPersonalId(updated.personalId ?? '');
      toast.success('Cập nhật hồ sơ thành công.');

      if (isProfileComplete(updated)) {
        navigate('/app', { replace: true });
        return;
      }

      setMessage('Cập nhật hồ sơ thành công.');
    } catch (exception) {
      const reason = exception instanceof Error ? exception.message : 'Không thể cập nhật hồ sơ. Vui lòng thử lại.';
      setError(reason);
      toast.error(reason);
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
        {!isProfileComplete(account) && (
          <p className="warning" role="status">
            Vui lòng cập nhật đầy đủ hồ sơ trước khi sử dụng các chức năng khác.
          </p>
        )}
        <div className="form-grid">
          <label>Họ và tên<input value={fullName} onChange={(event) => setFullName(event.target.value)} disabled={isSubmitting} required /></label>
          <label>Số điện thoại<input value={phoneNumber} onChange={(event) => setPhoneNumber(event.target.value)} disabled={isSubmitting} required /></label>
          <label>Ngày sinh<input type="date" value={dateOfBirth} onChange={(event) => setDateOfBirth(event.target.value)} disabled={isSubmitting} required /></label>
          <label>Giới tính<input value={gender} onChange={(event) => setGender(event.target.value)} disabled={isSubmitting} required /></label>
          <label>Địa chỉ<input value={address} onChange={(event) => setAddress(event.target.value)} disabled={isSubmitting} required /></label>
          <label>Số giấy tờ cá nhân<input value={personalId} onChange={(event) => setPersonalId(event.target.value)} disabled={isSubmitting} required /></label>
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

function isRequiredProfileInputComplete(input: RequiredProfileInput) {
  return [
    input.fullName,
    input.phoneNumber,
    input.dateOfBirth,
    input.gender,
    input.address,
    input.personalId
  ].every((value) => value.trim().length > 0);
}
