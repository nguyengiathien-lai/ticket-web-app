import { Card } from '../../components/Card';

export function SettingsPage() {
  return (
    <Card title="Cài đặt hệ thống">
      <div className="form-grid">
        <label>Tên hệ thống<input defaultValue="TransitPass"/></label>
        <label>Địa chỉ email hỗ trợ<input defaultValue="support@transitpass.vn"/></label>
        <label>Thời gian hết hạn OTP<input defaultValue="5 phút"/></label>
        <label>Thời gian hết phiên<input defaultValue="30 phút"/></label>
      </div>
      <button className="primary-button fit">Lưu cài đặt</button>
    </Card>
  );
}
