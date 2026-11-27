# teamwork-backend

[![CI](https://github.com/TeamSync-se/teamwork-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/TeamSync-se/teamwork-backend/actions/workflows/ci.yml)

Backend for Online Team Collaboration System – Software Engineering Project

> **Thành viên mới:** đọc [docs/huong-dan-dong-gop.md](docs/huong-dan-dong-gop.md) trước khi làm issue đầu tiên. File đó thống nhất quy trình làm việc, quy ước code và cách dùng AI cho cả nhóm.

## Yêu cầu
- JDK 21
- Docker Desktop

## Chạy dự án
1. Bật CSDL: `docker compose up -d`
2. Đặt `JWT_SECRET`, là khóa ký token đăng nhập: một chuỗi ngẫu nhiên dài ít nhất 32 byte, mỗi người tự tạo trên máy mình và không commit. Thiếu biến này thì backend báo lỗi và không khởi động.
   - Tạo khóa ngẫu nhiên 48 byte:
     - Windows (PowerShell): `$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)`
     - macOS, Linux, Git Bash: `openssl rand -base64 48`
   - Ghi khóa vừa tạo vào file `.env` ở thư mục gốc dự án, một dòng dạng `JWT_SECRET=<khóa>`. Backend tự đọc file này khi khởi động; xem mục "Biến môi trường và file `.env`" bên dưới.

   Đổi khóa thì mọi token đã phát hành hết hiệu lực, người dùng phải đăng nhập lại.
3. Chạy backend: `.\mvnw spring-boot:run` (Windows) hoặc `./mvnw spring-boot:run`
4. Backend chạy ở http://localhost:8080. Flyway tự tạo bảng khi khởi động.
5. Kiểm tra: mở http://localhost:8080/api/health, thấy `{"status":"UP"}` là backend đã chạy.

Mặc định backend chỉ cho trang ở `http://localhost:3000` gọi API từ trình duyệt (CORS). Frontend chạy ở địa chỉ khác thì đặt biến môi trường `CORS_ALLOWED_ORIGINS` trước khi chạy backend, nhiều địa chỉ cách nhau bằng dấu phẩy, ví dụ `CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001`.

## Biến môi trường và file `.env`

Khóa dịch vụ và cấu hình riêng của từng máy không ghi vào code hay `application.properties`; file này chỉ tham chiếu tên biến, ví dụ `${R2_BUCKET:}`.

- **Khi dev:** tạo file `.env` ở thư mục gốc dự án, mỗi dòng một biến dạng `TEN_BIEN=gia_tri`. Backend tự đọc file này khi khởi động, không cần đặt biến trong terminal. `.env` đã nằm trong `.gitignore`, không commit file này.
- **Khi triển khai:** đặt biến môi trường của hệ thống. Biến hệ thống được ưu tiên hơn giá trị trong `.env`.

Lưu ý khi viết `.env`:
- Không bọc giá trị trong dấu nháy: `R2_BUCKET="abc"` sẽ lấy cả dấu nháy.
- Dòng bắt đầu bằng `#` là chú thích. Dấu `\` phải viết thành `\\`.
- Giá trị chỉ dùng ký tự ASCII (không dấu), vì backend đọc file theo bảng mã ISO-8859-1.
- Backend tìm `.env` ở thư mục đang chạy: chạy `mvnw` từ thư mục gốc; chạy bằng IDE thì đặt thư mục làm việc (working directory) của cấu hình chạy là thư mục gốc.
- Test cũng đọc `.env` nếu có, giống ứng dụng. Đổi `CORS_ALLOWED_ORIGINS` trong `.env` thì test CORS trên máy đó có thể không qua; CI không có `.env` nên không bị ảnh hưởng.

| Biến | Bắt buộc | Dùng cho |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Không, mặc định khớp `compose.yaml` | PostgreSQL |
| `CORS_ALLOWED_ORIGINS` | Không, mặc định `http://localhost:3000` | Origin frontend được gọi API |
| `JWT_SECRET` | **Có**, ít nhất 32 byte | Khóa ký token đăng nhập (JWT) |
| `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET` | Chưa, cho tới khi làm tính năng tải tệp | Cloudflare R2 |
| `AI_API_KEY`, `AI_MODEL` | Chưa, cho tới khi làm tính năng AI tóm tắt | Dịch vụ AI |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Chưa, cho tới khi làm tính năng gửi email; `MAIL_PORT` nếu đặt phải từ 1 đến 65535 | Email (SMTP) |

Biến bắt buộc mà thiếu, hoặc biến có giá trị sai, thì backend dừng ngay khi khởi động và ghi tên biến vào log; thông báo lỗi không chứa giá trị của khóa.

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
