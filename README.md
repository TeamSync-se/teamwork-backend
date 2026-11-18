# teamwork-backend
Backend for Online Team Collaboration System – Software Engineering Project

> **Thành viên mới:** đọc [docs/huong-dan-dong-gop.md](docs/huong-dan-dong-gop.md) trước khi làm issue đầu tiên. File đó thống nhất quy trình làm việc, quy ước code và cách dùng AI cho cả nhóm.

## Yêu cầu
- JDK 21
- Docker Desktop

## Chạy dự án
1. Bật CSDL: `docker compose up -d`
2. Đặt biến môi trường `JWT_SECRET`, là khóa ký token đăng nhập: một chuỗi ngẫu nhiên dài ít nhất 32 byte, mỗi người tự tạo trên máy mình và không commit. Thiếu biến này thì backend báo lỗi và không khởi động. Ví dụ tạo khóa ngẫu nhiên 48 byte:
   - Windows (PowerShell): `$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); $env:JWT_SECRET = [Convert]::ToBase64String($b)`
   - macOS, Linux, Git Bash: `export JWT_SECRET=$(openssl rand -base64 48)`

   Biến chỉ có hiệu lực trong cửa sổ terminal vừa đặt; chạy bằng IDE thì đặt trong cấu hình chạy (Run Configuration). Đổi khóa thì mọi token đã phát hành hết hiệu lực, người dùng phải đăng nhập lại.
3. Chạy backend: `.\mvnw spring-boot:run` (Windows) hoặc `./mvnw spring-boot:run`
4. Backend chạy ở http://localhost:8080. Flyway tự tạo bảng khi khởi động.
5. Kiểm tra: mở http://localhost:8080/api/health, thấy `{"status":"UP"}` là backend đã chạy.

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
