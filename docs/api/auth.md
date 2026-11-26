# Hợp đồng API: đăng ký và đăng nhập

> **Hiệu lực:** khi được merge vào `main`. Muốn thay đổi thì mở pull request theo mục 8 của [conventions.md](conventions.md).
> **Issue:** #8 Đăng ký, #9 Đăng nhập. **Use Case:** UC01, UC02. **Màn hình prototype:** 1.1 Đăng nhập, 1.2 Đăng ký.
> Các quy ước chung (định dạng lỗi, cách gửi token, định dạng thời điểm) nằm ở [conventions.md](conventions.md).

| API | Mục đích | Cần đăng nhập |
|---|---|---|
| `POST /api/auth/register` | Tạo tài khoản mới | Không |
| `POST /api/auth/login` | Đăng nhập, nhận token | Không |
| `GET /api/users/me` | Lấy thông tin người đang đăng nhập | Có |

## 1. Đăng ký: `POST /api/auth/register`

### Gửi lên

```json
{
  "fullName": "Nguyễn Văn A",
  "email": "a@example.com",
  "password": "matkhau123",
  "confirmPassword": "matkhau123"
}
```

| Trường | Bắt buộc | Quy tắc |
|---|---|---|
| `fullName` | Có | Không được chỉ có khoảng trắng; tối đa 100 ký tự. Backend bỏ khoảng trắng ở hai đầu |
| `email` | Có | Đúng định dạng email; tối đa 255 ký tự. Không phân biệt chữ hoa, chữ thường; backend lưu ở dạng chữ thường |
| `password` | Có | Từ 8 đến 64 ký tự, và không quá 72 byte khi mã hóa UTF-8 (xem ghi chú dưới bảng) |
| `confirmPassword` | Có | Phải giống `password` |

Giới hạn 72 byte là giới hạn của BCrypt, thuật toán backend dùng để băm mật khẩu. Mật khẩu chỉ gồm chữ không dấu, số và ký hiệu thì 64 ký tự luôn nằm trong giới hạn, vì mỗi ký tự chiếm 1 byte. Một ký tự có dấu như "ệ" chiếm 3 byte, nên mật khẩu dài có nhiều ký tự có dấu có thể vượt giới hạn. Khi đó backend trả lỗi `VALIDATION_ERROR` trên trường `password`, frontend hiện lỗi dưới ô mật khẩu như các lỗi khác.

Frontend nên kiểm tra trước các quy tắc này để báo lỗi ngay trên màn hình, nhưng backend vẫn kiểm tra lại và là bên quyết định cuối cùng.

### Thành công: `201 Created`

```json
{
  "id": 1,
  "fullName": "Nguyễn Văn A",
  "email": "a@example.com",
  "createdAt": "2026-11-13T08:30:00Z"
}
```

Đăng ký **không** trả token. Theo UC01 bước 6, sau khi đăng ký thành công, frontend chuyển người dùng sang trang Đăng nhập.

### Lỗi

| HTTP | `code` | Khi nào | Frontend hiển thị |
|---|---|---|---|
| 400 | `VALIDATION_ERROR` | Một hoặc nhiều trường sai quy tắc (UC01 kịch bản 4a) | Lỗi trong `errors` dưới đúng ô nhập |
| 409 | `EMAIL_ALREADY_EXISTS` | Email đã được dùng (UC01 kịch bản 4b) | `detail` dưới ô email |

Ví dụ lỗi `400`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Dữ liệu không hợp lệ",
  "instance": "/api/auth/register",
  "code": "VALIDATION_ERROR",
  "timestamp": "2026-11-13T08:30:00Z",
  "errors": [
    { "field": "fullName", "message": "Họ tên không được để trống" },
    { "field": "confirmPassword", "message": "Mật khẩu xác nhận không khớp" }
  ]
}
```

Ví dụ lỗi `409`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Email đã được sử dụng",
  "instance": "/api/auth/register",
  "code": "EMAIL_ALREADY_EXISTS",
  "timestamp": "2026-11-13T08:30:00Z"
}
```

## 2. Đăng nhập: `POST /api/auth/login`

### Gửi lên

```json
{
  "email": "a@example.com",
  "password": "matkhau123"
}
```

| Trường | Bắt buộc | Quy tắc |
|---|---|---|
| `email` | Có | Đúng định dạng email; không phân biệt chữ hoa, chữ thường |
| `password` | Có | Không được để trống |

### Thành công: `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc123",
  "tokenType": "Bearer",
  "expiresAt": "2026-11-14T08:30:00Z"
}
```

- Token hết hạn sau **1 ngày** (24 giờ), đúng thời điểm ghi trong `expiresAt`. Người dùng làm việc cả buổi không bị đăng xuất; lỡ lộ token thì kẻ gian cũng chỉ dùng được tối đa 1 ngày, vì JWT không thu hồi được trước hạn.
- Không có refresh token. Hết hạn thì người dùng đăng nhập lại.
- Gửi token ở mọi API cần đăng nhập bằng header `Authorization: Bearer <accessToken>`.
- Backend không đặt cookie. Frontend tự giữ token, cách làm gợi ý ở mục 5.

### Lỗi

| HTTP | `code` | Khi nào | Frontend hiển thị |
|---|---|---|---|
| 400 | `VALIDATION_ERROR` | Thiếu email hoặc mật khẩu, email sai định dạng | Lỗi trong `errors` dưới đúng ô nhập |
| 401 | `INVALID_CREDENTIALS` | Sai email **hoặc** sai mật khẩu (UC02 kịch bản 4a) | `detail`: "Email hoặc mật khẩu không chính xác" |

Sai email và sai mật khẩu trả **cùng một** thông điệp, để người ngoài không dò được email nào đã có tài khoản.

## 3. Người đang đăng nhập: `GET /api/users/me`

Gửi kèm header `Authorization: Bearer <accessToken>`, không có body.

### Thành công: `200 OK`

```json
{
  "id": 1,
  "fullName": "Nguyễn Văn A",
  "email": "a@example.com",
  "createdAt": "2026-11-13T08:30:00Z"
}
```

Issue #10 (xem và cập nhật thông tin cá nhân) có thể thêm trường vào response này, theo quy tắc "thêm trường không cần báo trước" ở mục 8 của [conventions.md](conventions.md).

### Lỗi

| HTTP | `code` | Khi nào |
|---|---|---|
| 401 | `UNAUTHORIZED` | Thiếu token, token sai hoặc hết hạn (UC02 kịch bản 4b) |

## 4. Luồng sử dụng phía frontend

1. **Đăng ký (màn hình 1.2):** gọi `POST /api/auth/register`.
   - `201`: báo đăng ký thành công, chuyển sang trang Đăng nhập.
   - `400`: hiện lỗi dưới từng ô theo `errors[].field`.
   - `409`: hiện `detail` dưới ô email.
2. **Đăng nhập (màn hình 1.1):** gọi `POST /api/auth/login`.
   - `200`: lưu token theo mục 5, gọi `GET /api/users/me` để lấy họ tên, rồi vào Dashboard.
   - `401` với mã `INVALID_CREDENTIALS`: hiện `detail` trên form, giữ nguyên email đã nhập.
3. **Các API khác:** luôn gửi header `Authorization`. Nhận `401` thì xóa token và chuyển về trang Đăng nhập.
4. **Đăng xuất:** frontend xóa token. Backend không có API đăng xuất, vì token không lưu trạng thái ở phía backend.

`401` ở API đăng nhập nghĩa là **sai thông tin đăng nhập**, nên hiện thông báo trên form. `401` ở mọi API khác nghĩa là **phiên đã hết**, nên chuyển về trang Đăng nhập. Frontend phân biệt hai trường hợp bằng `code`.

## 5. Giữ phiên phía frontend (gợi ý)

Phần này là gợi ý cho frontend, không ràng buộc backend. Backend chỉ cần nhận đúng header `Authorization: Bearer <accessToken>`.

1. **Lưu token vào cookie của frontend** ngay sau khi đăng nhập thành công: `Path=/`, `SameSite=Lax`, hết hạn đúng `expiresAt`, thêm `Secure` khi chạy HTTPS. Không đặt `HttpOnly`, vì JavaScript cần đọc token để gắn vào header và để kết nối WebSocket.
2. **Chặn trang chưa đăng nhập** bằng middleware (proxy) của Next.js: không có cookie, hoặc cookie đã hết hạn, thì chuyển về `/login`. Đoạn này chạy trước khi trả trang nên không bị nháy trang.
3. **Gắn token khi gọi API:** `lib/api-client.ts` đọc cookie và thêm header `Authorization`. Server Component cũng đọc được cookie này để gọi backend.
4. **Nhận `401` với mã `UNAUTHORIZED`** ở bất kỳ API nào: xóa cookie, chuyển về `/login`.
5. **Đăng xuất:** xóa cookie. Backend không có API đăng xuất.
6. **WebSocket (nhắn tin realtime, issue #17):** gửi token trong header `Authorization` của khung `CONNECT` khi kết nối STOMP. Chi tiết sẽ ghi trong hợp đồng của phân hệ nhắn tin.

Middleware chỉ kiểm tra cookie có tồn tại và còn hạn, không kiểm tra được token thật hay giả, vì khóa ký chỉ backend có. Việc chặn trang ở frontend là để trải nghiệm tốt hơn; việc bảo vệ dữ liệu là của backend, vì mọi API đều kiểm tra token.

**Vì sao không dùng cookie `httpOnly` do backend đặt:** cách đó an toàn hơn trước tấn công XSS, nhưng buộc frontend và backend khi triển khai phải chung một tên miền gốc, nếu không trình duyệt dễ chặn cookie. Backend cũng phải thêm API đăng xuất và cấu hình CORS cho phép gửi cookie. Nhóm chọn token để không bị ràng buộc cách triển khai. Bù lại, frontend giữ an toàn trước XSS bằng cách không chèn HTML lấy từ dữ liệu người dùng vào trang (không dùng `dangerouslySetInnerHTML` với dữ liệu đó).
