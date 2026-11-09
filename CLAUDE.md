# CLAUDE.md

Hướng dẫn cho Claude Code khi làm việc trong repo này. Quy tắc gốc nằm ở các file được trỏ tới bên dưới; khi quy tắc thay đổi thì sửa ở đó, không sửa ở đây.

- Dự án: backend Spring Boot (Java 21, PostgreSQL) của Hệ thống hỗ trợ làm việc nhóm trực tuyến. Yêu cầu nằm trong `docs/requirements/` (PA01, PA02, `features.md`).
- Cách chạy dự án và quy trình làm việc: `README.md`.
- Viết issue, pull request và tài liệu bằng tiếng Việt, thống nhất với các tài liệu sẵn có.

## Issue
- Issue mới viết theo `.github/ISSUE_TEMPLATE/yeu-cau.md`.
- Không bắt đầu làm issue còn nhãn `chua-co-AC`.

## Pull request
- Nhánh `main` được bảo vệ: không push thẳng; mỗi issue làm trên một nhánh riêng, ví dụ `feature/8-dang-ky`.
- Mô tả pull request viết theo `.github/pull_request_template.md`, có `Closes #số`.

## Review
- Review theo `docs/review-checklist.md`, dùng skill `/review-pr`.
- Không bao giờ approve hay merge pull request thay người dùng.

## Code
- Thay đổi CSDL bằng file migration Flyway mới; không sửa migration đã merge.
- Không đưa khóa bí mật vào code; đọc từ biến môi trường.
