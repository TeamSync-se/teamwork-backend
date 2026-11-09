# AGENTS.md

Hướng dẫn cho trợ lý AI viết code trong repo này. Quy tắc nằm ở các file được trỏ tới; khi quy tắc thay đổi thì sửa ở đó, không sửa ở đây.

Trước khi viết code cho một issue, **bắt buộc** đọc:
1. Chính issue đó (Phụ thuộc, Description, Acceptance Criteria, Solution).
2. `docs/huong-dan-dong-gop.md`: quy trình làm issue, quy ước code, những gì AI không được làm.
3. `docs/review-checklist.md`: những gì người review sẽ kiểm tra.
4. Use Case và prototype ghi trong issue, trong `docs/requirements/PA02.md`.

Quy tắc không được vi phạm:
- Không bắt đầu viết code khi issue còn nhãn `chua-co-AC`.
- Trình bày kế hoạch và chờ người dùng duyệt trước khi viết code.
- Không push thẳng `main`, không approve, không merge pull request.
- Chỉ push và mở pull request khi người dùng đồng ý; mô tả viết theo `.github/pull_request_template.md`.
- Không sửa file migration đã merge; không đưa khóa bí mật vào code.

Viết issue, pull request và tài liệu bằng tiếng Việt.
