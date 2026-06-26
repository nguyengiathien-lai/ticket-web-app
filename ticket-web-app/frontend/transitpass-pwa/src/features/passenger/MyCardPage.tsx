import { Card } from '../../components/Card';

export function MyCardPage() {
  return (
    <div className="two-column">
      <Card title="Thẻ của tôi">
        <div className="transit-card"><p>THẺ CỦA TÔI</p><h3>1234 5678 9012 3456</h3><span>Số dư hiện tại</span><strong>150.000đ</strong><button>Nạp tiền</button></div>
      </Card>
      <Card title="Thông tin thẻ">
        <dl className="info-list"><dt>Chủ thẻ</dt><dd>Nguyễn Văn A</dd><dt>Loại thẻ</dt><dd>Thẻ định danh</dd><dt>Ngày phát hành</dt><dd>01/06/2026</dd><dt>Trạng thái</dt><dd className="success">Đang hoạt động</dd></dl>
      </Card>
      <Card title="Đặt mua thẻ vật lý" className="wide"><p>Phí phát hành: <b>20.000đ</b>. Thẻ hỗ trợ nạp gói tuần/tháng/năm và truy xuất lịch sử sử dụng.</p><button className="primary-button fit">Đặt mua ngay</button></Card>
    </div>
  );
}
