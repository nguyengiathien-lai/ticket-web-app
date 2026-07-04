import { Card } from '../../components/Card';

export function UserManagementPage() {
  const users = ['Nguyễn Văn A', 'Trần Thị B', 'Lê Văn C'];

  return (
    <Card title="Quản lý người dùng">
      <button className="primary-button fit">+ Thêm mới</button>
      <div className="table-wrap">
        <table>
          <thead><tr><th>Họ tên</th><th>Địa chỉ email</th><th>SĐT</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
          <tbody>
            {users.map((user, index) => (
              <tr key={user}>
                <td>{user}</td>
                <td>user{index + 1}@gmail.com</td>
                <td>09012345{index + 6}7</td>
                <td className={index === 2 ? 'danger' : 'success'}>{index === 2 ? 'Khóa' : 'Hoạt động'}</td>
                <td>Sửa / Xóa</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}
