import { Card } from '../../components/Card';
import { passenger } from '../../services/mockData';

export function ProfilePage() {
  return <Card title="Hồ sơ cá nhân"><div className="profile-head"><div className="avatar large-avatar">NA</div><div><h2>{passenger.name}</h2><p>{passenger.email}</p></div></div><div className="form-grid"><label>Số điện thoại<input defaultValue={passenger.phone}/></label><label>Ngày sinh<input defaultValue="15/08/1998"/></label><label>Địa chỉ<input defaultValue={passenger.address}/></label><label>CMND/CCCD<input defaultValue={passenger.nationalId}/></label><label>Ảnh chân dung<input type="file"/></label><label>Thẻ học sinh/sinh viên<input type="file"/></label></div><button className="primary-button fit">Cập nhật hồ sơ</button></Card>;
}
