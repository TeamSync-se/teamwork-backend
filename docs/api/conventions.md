# Quy ước API chung

> **Hiệu lực:** khi được merge vào `main`. Muốn thay đổi thì mở pull request theo mục 8.
> Áp dụng cho **mọi** API của backend. Hợp đồng của từng phân hệ nằm ở các file khác trong thư mục này, ví dụ [auth.md](auth.md).
> Hợp đồng API chỉ ghi ở thư mục `docs/api/` của repo backend. Issue của backend và frontend dẫn link tới đây, không chép lại nội dung, để chỉ có một nguồn.

## 1. Địa chỉ và đường dẫn

- Khi phát triển: backend chạy ở `http://localhost:8080`, frontend chạy ở `http://localhost:3000`. Backend cho phép origin này gọi vào (CORS, issue #1).
- Mọi đường dẫn bắt đầu bằng `/api`, dùng danh từ số nhiều, chữ thường, nhiều từ thì nối bằng dấu gạch ngang. Ví dụ `/api/groups`, `/api/groups/{groupId}/tasks`.
- Dữ liệu thuộc một nhóm nằm dưới `/api/groups/{groupId}/...`.
- Kiểm tra backend còn chạy: `GET /api/health` trả `200` với `{"status": "UP"}`, không cần đăng nhập.

| Phương thức | Dùng để |
|---|---|
| `GET` | Đọc dữ liệu |
| `POST` | Tạo mới, hoặc thực hiện một hành động (đăng nhập, bình chọn...) |
| `PATCH` | Sửa một phần |
| `DELETE` | Xóa |

## 2. Dữ liệu gửi và nhận

- Định dạng JSON, mã hóa UTF-8. Khi gửi dữ liệu, kèm header `Content-Type: application/json`.
- Tên trường viết `camelCase`, ví dụ `fullName`, `createdAt`.
- `id` là số nguyên.
- Thời điểm là chuỗi ISO 8601 theo giờ UTC, có chữ `Z` ở cuối: `"2026-11-13T08:30:00Z"`. Ngày không kèm giờ: `"2026-11-13"`. Frontend tự đổi sang giờ Việt Nam khi hiển thị.
- Giá trị liệt kê (vai trò, trạng thái...) là chuỗi chữ in hoa, nối bằng dấu gạch dưới: `"LEADER"`, `"IN_PROGRESS"`.
- Trường không có giá trị thì trả `null`, không bỏ trường đó đi. **Ngoại lệ:** trường `errors` trong response lỗi chỉ có khi `code` là `VALIDATION_ERROR`; lỗi khác không có trường này (mục 5), nên frontend phải xử lý được trường hợp không có `errors`.
- Response **không bao giờ** chứa mật khẩu, mã băm mật khẩu hay khóa bí mật.

## 3. Xác thực

- Người dùng đăng nhập bằng `POST /api/auth/login` để nhận `accessToken` (JWT). Chi tiết ở [auth.md](auth.md).
- Mọi API, **trừ** `/api/auth/**` và `/api/health`, phải gửi kèm header:

  ```
  Authorization: Bearer <accessToken>
  ```

- Thiếu token, token sai hoặc đã hết hạn: backend trả `401` với mã `UNAUTHORIZED`. Frontend xóa token đang lưu và chuyển về trang Đăng nhập.
- `/api/auth/**` và `/api/health` bỏ qua header `Authorization` nếu frontend có gửi kèm, nên token cũ hay hỏng không làm hỏng việc đăng nhập lại.
- Backend không dùng cookie để giữ phiên. Cách frontend lưu token được gợi ý ở mục 5 của [auth.md](auth.md).

## 4. Mã HTTP

| Mã | Khi nào |
|---|---|
| `200 OK` | Thành công, có dữ liệu trả về |
| `201 Created` | Tạo mới thành công |
| `204 No Content` | Thành công, không có dữ liệu trả về (ví dụ xóa) |
| `400 Bad Request` | Dữ liệu gửi lên không hợp lệ |
| `401 Unauthorized` | Chưa đăng nhập, token sai hoặc hết hạn; hoặc đăng nhập sai thông tin |
| `403 Forbidden` | Đã đăng nhập, thuộc nhóm, nhưng vai trò không đủ quyền |
| `404 Not Found` | Không tìm thấy, hoặc người gọi không thuộc nhóm (xem mục 6) |
| `405 Method Not Allowed` | Gọi sai phương thức, ví dụ `GET` vào API chỉ nhận `POST` |
| `409 Conflict` | Xung đột với dữ liệu đang có, ví dụ email đã được dùng |
| `413 Payload Too Large` | Tệp tải lên vượt giới hạn (phân hệ tài nguyên) |
| `415 Unsupported Media Type` | Gửi body mà thiếu header `Content-Type: application/json` hoặc dùng kiểu dữ liệu khác |
| `500 Internal Server Error` | Lỗi không lường trước ở backend |
| `502 Bad Gateway`, `503 Service Unavailable` | Dịch vụ bên ngoài (lưu tệp, AI, email) bị lỗi hoặc không phản hồi |

Backend **không dùng** `422` (dữ liệu không hợp lệ luôn là `400`) và chưa dùng `429` (chưa giới hạn tần suất gọi).

## 5. Định dạng lỗi

Mọi lỗi trả về cùng một dạng, theo chuẩn Problem Details (RFC 9457) mà Spring hỗ trợ sẵn, có thêm các trường `code`, `timestamp` và `errors`. Header của response là `Content-Type: application/problem+json`.

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
    { "field": "email", "message": "Email không đúng định dạng" },
    { "field": "password", "message": "Mật khẩu phải có ít nhất 8 ký tự" }
  ]
}
```

| Trường | Ý nghĩa | Frontend dùng để |
|---|---|---|
| `status` | Mã HTTP, lặp lại cho tiện | Phân loại lỗi |
| `code` | Mã lỗi dạng chữ, cố định | **Quyết định cách xử lý.** Dựa vào `code`, không dựa vào câu chữ |
| `detail` | Thông điệp tiếng Việt, dễ hiểu | Hiển thị cho người dùng |
| `instance` | Đường dẫn API gây lỗi | Ghi log, tìm lỗi |
| `timestamp` | Thời điểm xảy ra lỗi (UTC) | Ghi log, tìm lỗi |
| `errors` | Danh sách lỗi theo từng trường; chỉ có khi `code` là `VALIDATION_ERROR`, lỗi khác không có trường này (kể cả `null`) | Hiện lỗi ngay dưới ô nhập tương ứng |
| `type`, `title` | Trường chuẩn của RFC 9457 | Có thể bỏ qua |

Lỗi `500` chỉ có thông điệp chung, không chứa chi tiết nội bộ như tên lớp hay câu lệnh SQL; chi tiết chỉ ghi vào log của backend.

### Mã lỗi chung

| `code` | HTTP | Khi nào |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Một hoặc nhiều trường không hợp lệ; chi tiết trong `errors` |
| `BAD_REQUEST` | 400 | Body không phải JSON hợp lệ, hoặc thiếu tham số bắt buộc trên đường dẫn |
| `UNAUTHORIZED` | 401 | Thiếu token, token sai hoặc hết hạn |
| `FORBIDDEN` | 403 | Vai trò không đủ quyền |
| `NOT_FOUND` | 404 | Không tìm thấy, hoặc không thuộc nhóm |
| `METHOD_NOT_ALLOWED` | 405 | Gọi sai phương thức HTTP, ví dụ `GET` vào API chỉ nhận `POST` |
| `PAYLOAD_TOO_LARGE` | 413 | Tệp tải lên vượt giới hạn |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Gửi body mà thiếu header `Content-Type: application/json` hoặc dùng kiểu dữ liệu khác |
| `EXTERNAL_SERVICE_ERROR` | 502, 503 | Dịch vụ bên ngoài lỗi |
| `INTERNAL_ERROR` | 500 | Lỗi không lường trước |

Mã lỗi riêng của từng phân hệ (ví dụ `EMAIL_ALREADY_EXISTS`) được ghi trong file hợp đồng của phân hệ đó.

### Gợi ý cho frontend khi đọc lỗi

- Khi response lỗi có body dạng `application/problem+json`: dùng `detail` làm thông báo hiển thị, giữ `code` để xử lý theo từng trường hợp, và `errors` để hiện lỗi dưới từng ô nhập.
- Khi không có body (mất mạng, proxy lỗi, backend chưa chạy): dùng thông báo mặc định theo mã HTTP, như `lib/api-client.ts` của frontend đang làm.
- Gợi ý mở rộng lớp `ApiError` của frontend thêm hai thuộc tính `code` và `fieldErrors` (lấy từ `errors`) để màn hình dùng.

## 6. Quyền theo nhóm

- Người **không thuộc nhóm** gọi bất kỳ API nào của nhóm đó: trả `404` với mã `NOT_FOUND`, như thể nhóm không tồn tại. Cách này không để lộ cho người ngoài biết nhóm có tồn tại hay không.
- Người **thuộc nhóm** nhưng vai trò không đủ quyền (ví dụ thành viên thường làm việc dành cho trưởng nhóm): trả `403` với mã `FORBIDDEN`.

## 7. Danh sách và phân trang

- Danh sách có thể dài (tin nhắn, tài nguyên...) dùng phân trang qua tham số `?page=0&size=20`. `page` đếm từ 0; `size` mặc định 20, tối đa 100.
- Response của danh sách có phân trang:

  ```json
  {
    "items": [],
    "page": 0,
    "size": 20,
    "totalItems": 57,
    "totalPages": 3
  }
  ```

- Danh sách ngắn (nhóm của tôi, thành viên của nhóm...) trả thẳng một mảng JSON, trừ khi hợp đồng của phân hệ ghi khác.

## 8. Thay đổi hợp đồng

- Mọi thay đổi hợp đồng đi qua pull request sửa file trong `docs/api/`. Người làm frontend của phần đó là người approve.
- **Thêm** trường mới vào response: được phép mà không cần báo trước. Frontend bỏ qua những trường mình không dùng.
- **Đổi tên, xóa trường, đổi kiểu dữ liệu, đổi mã lỗi:** phải sửa hợp đồng trước, báo bên kia, rồi mới sửa code.
