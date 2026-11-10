# teamwork-backend
Backend for Online Team Collaboration System – Software Engineering Project

## Yêu cầu
- JDK 21
- Docker Desktop

## Chạy dự án
1. Bật CSDL: `docker compose up -d`
2. Chạy backend: `.\mvnw spring-boot:run` (Windows) hoặc `./mvnw spring-boot:run`
3. Backend chạy ở http://localhost:8080. Flyway tự tạo bảng khi khởi động.

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
