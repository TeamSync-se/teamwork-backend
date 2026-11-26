# teamwork-backend
Backend for Online Team Collaboration System – Software Engineering Project

> **Thành viên mới:** đọc [docs/huong-dan-dong-gop.md](docs/huong-dan-dong-gop.md) trước khi làm issue đầu tiên. File đó thống nhất quy trình làm việc, quy ước code và cách dùng AI cho cả nhóm.

## Yêu cầu
- JDK 21
- Docker Desktop

## Cấu hình biến môi trường

File `.env.example` liệt kê cấu hình DB, CORS, JWT và các dịch vụ ngoài (R2, AI, SMTP). Chuẩn bị cấu hình cá nhân:

1. Copy file mẫu: `cp .env.example .env`
2. Mở `.env` và điền giá trị thật (lấy từ kênh nội bộ nhóm).
3. **Không commit file `.env`** — file này đã nằm trong `.gitignore`.

Hiện backend đọc `DB_*` và `CORS_ALLOWED_ORIGINS` từ biến môi trường của tiến trình. Backend **chưa tự nạp `.env`**; cấu hình tự nạp file và ánh xạ các khóa JWT, R2, AI, SMTP thuộc issue #7. Chỉ sao chép file chưa làm các dịch vụ này hoạt động. Khi cần đổi cấu hình hiện tại, đặt biến môi trường trong terminal hoặc cấu hình chạy của IDE trước khi khởi động backend.

Ví dụ PowerShell: `$env:CORS_ALLOWED_ORIGINS = 'http://localhost:3000,http://localhost:3001'`, rồi chạy `.\mvnw spring-boot:run`.

Mẫu dùng Google AI Studio với `gemini-3.6-flash`, model thay thế `gemini-2.0-flash` theo [lịch ngừng hỗ trợ của Google](https://ai.google.dev/gemini-api/docs/deprecations). Khi cập nhật mẫu, người giữ khóa cần cập nhật `AI_MODEL` trong `.env` chia sẻ nội bộ và thống nhất với người làm issue #25; kiểm tra quyền sử dụng và hạn mức của tài khoản trước khi gọi dịch vụ.

Để chạy backend với cấu hình DB mặc định, thực hiện cả bước bật PostgreSQL và bước chạy backend bên dưới.

## Chạy dự án
1. Bật CSDL: `docker compose up -d`
2. Chạy backend: `.\mvnw spring-boot:run` (Windows) hoặc `./mvnw spring-boot:run`
3. Backend chạy ở http://localhost:8080. Flyway tự tạo bảng khi khởi động.
4. Kiểm tra: mở http://localhost:8080/api/health, thấy `{"status":"UP"}` là backend đã chạy.

Mặc định backend chỉ cho trang ở `http://localhost:3000` gọi API từ trình duyệt (CORS). Frontend chạy ở địa chỉ khác thì đặt biến môi trường `CORS_ALLOWED_ORIGINS` trước khi chạy backend, nhiều địa chỉ cách nhau bằng dấu phẩy, ví dụ `CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001`.

## Chạy test
- Lệnh: `.\mvnw test` (Windows) hoặc `./mvnw test`.
- **Cần Docker Desktop đang chạy.** Testcontainers tự bật một PostgreSQL 17 tạm cho mỗi lần chạy test và xóa nó khi xong, nên không cần `docker compose up`.
- Test cần cơ sở dữ liệu thì thêm `@Import(TestcontainersConfiguration.class)`, xem ví dụ ở `DatabaseMigrationTests`.

## Quy trình làm việc với issue
- Danh sách yêu cầu nằm ở [docs/requirements/features.md](docs/requirements/features.md); mỗi dòng là một issue.
- **Milestone 1:** yêu cầu bắt buộc `[B]`. **Milestone 3:** yêu cầu nâng cao `[N]`.
- Issue có nhãn `nen-tang` được ưu tiên làm trước.
- Issue có nhãn `chua-co-AC` chưa được bắt đầu: người nhận việc điền Acceptance Criteria, nhờ một thành viên khác đọc lại, rồi gỡ nhãn.
- Mục **Phụ thuộc** trong issue: *cứng* là phải xong trước mới chạy thử được; *mềm* là làm song song được, ghép sau.
- Issue mới tạo theo mẫu "Yêu cầu / tính năng" để giữ cùng một form.

## Quy trình pull request và review
- Nhánh `main` được bảo vệ: không push thẳng; mọi thay đổi đi qua pull request và cần **1 approve** của một thành viên khác.
- Mỗi issue làm trên một nhánh riêng, ví dụ `feature/8-dang-ky`. Mô tả PR ghi `Closes #số` để issue tự đóng khi merge.
- Người review đọc theo [docs/review-checklist.md](docs/review-checklist.md).
- Ai dùng Claude Code có thể gõ `/review-pr <số PR>` để được hỗ trợ đối chiếu Acceptance Criteria và checklist. Approve vẫn phải do người bấm.
