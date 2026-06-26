import { useState } from 'react';
import { Bus, CreditCard, TrainFront } from 'lucide-react';
import { Card } from '../../components/Card';
import { packages } from '../../services/mockData';
import { currency } from '../../utils/format';

export function BuyTicketPage() {
  const [selected, setSelected] = useState(packages[0].id);
  const current = packages.find((p) => p.id === selected) ?? packages[0];
  return (
    <div className="two-column">
      <Card title="Mua vé điện tử">
        <div className="tabs"><button className="active">Vé lượt</button><button>Vé ngày</button><button>Gói vé</button></div>
        <p className="field-title">Chọn loại phương tiện</p>
        <div className="transport-options"><button className="active"><Bus/>Xe buýt</button><button><TrainFront/>Metro</button><button><TrainFront/>Tàu điện</button></div>
        <label>Chọn tuyến<select><option>Vui lòng chọn tuyến</option><option>Metro Bến Thành - Suối Tiên</option></select></label>
        <label>Số lượng<div className="quantity"><button>-</button><span>1</span><button>+</button></div></label>
        <div className="total"><span>Thanh toán</span><b>{currency(current.price)}</b></div>
        <button className="primary-button">Thanh toán bằng VNPay Sandbox</button>
      </Card>
      <Card title="Mua/nạp gói vé vào thẻ">
        <label>Chọn thẻ<select><option>Thẻ của tôi • 1234 5678 9012 3456</option></select></label>
        <div className="package-list">
          {packages.slice(2).map((p) => (
            <label className={selected === p.id ? 'package-option selected' : 'package-option'} key={p.id}>
              <input type="radio" checked={selected === p.id} onChange={() => setSelected(p.id)}/>
              <span><b>{p.name}</b><small>{p.description}</small></span><b>{currency(p.price)}</b>
            </label>
          ))}
        </div>
        <button className="primary-button">Thanh toán</button>
      </Card>
    </div>
  );
}
