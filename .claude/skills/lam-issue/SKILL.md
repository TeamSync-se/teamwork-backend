---
name: lam-issue
description: Thực hiện một issue của repo teamwork-backend theo đúng quy trình của nhóm, từ đọc issue, lập kế hoạch, viết code và test, đến mở pull request, có điểm dừng để người dùng duyệt. Dùng khi người dùng gõ /lam-issue hoặc muốn bắt đầu làm một issue.
argument-hint: "[số issue]"
disable-model-invocation: true
---

# Làm một issue theo quy trình của nhóm

Quy trình và quy ước nằm ở `docs/huong-dan-dong-gop.md` (mục 3 và mục 4) và `docs/review-checklist.md`. Đọc cả hai file ở đầu mỗi lần chạy và làm theo chúng; không dùng quy tắc nhớ sẵn. Nếu thiếu một trong hai file, báo người dùng và dừng.

Số issue là `$ARGUMENTS`. Nếu trống, hỏi người dùng. Trên Windows, nếu lệnh `gh` không được nhận ra, dùng `C:\Program Files\GitHub CLI\gh.exe`.

Có ba **điểm dừng** (đánh dấu ⏸) phải chờ người dùng trả lời. Không tự vượt qua điểm dừng.

## Bước 1: Đọc issue và kiểm tra điều kiện

1. `gh issue view <số> --json number,title,body,labels,assignees,state`
2. Issue đã đóng: báo và dừng.
3. Issue còn nhãn `chua-co-AC` hoặc mục Acceptance Criteria trống: **không viết code**. Đề nghị soạn nháp Acceptance Criteria từ Use Case trong `docs/requirements/PA02.md` cho người dùng xem, rồi dừng. Chỉ cập nhật issue trên GitHub khi người dùng yêu cầu rõ ràng.
4. Đọc dòng **Cứng** trong mục Phụ thuộc, kiểm tra trạng thái từng issue: `gh issue view <số> --json state,title`. Issue nào chưa đóng thì liệt kê ra và hỏi người dùng có làm trước phần không phụ thuộc không.
5. Issue chưa có ai được gán: nhắc người dùng tự gán mình.

## Bước 2: Đọc tài liệu và lập kế hoạch

1. Đọc các tài liệu ở mục 2 của `docs/huong-dan-dong-gop.md`: Use Case và prototype ghi trong issue, các file quy ước API, hợp đồng API, ERD nếu đã có.
2. Đọc phần code hiện có liên quan để không viết trùng.
3. Trình bày kế hoạch:
   - Bảng ánh xạ **từng dòng Acceptance Criteria** sang cách làm và test sẽ chứng minh nó.
   - File sẽ tạo hoặc sửa, theo cấu trúc package ở mục 4.
   - Migration mới (tên theo quy ước), API mới (đường dẫn, dữ liệu gửi và nhận, mã lỗi).
   - Thư viện mới cần thêm, kèm lý do.
   - Câu hỏi còn mở mà issue chưa trả lời.

⏸ **Điểm dừng 1:** chờ người dùng duyệt hoặc sửa kế hoạch.

## Bước 3: Viết code và test

1. Kiểm tra `git status` sạch; nếu có thay đổi chưa commit, hỏi người dùng xử lý trước.
2. `git checkout main`, `git pull`, rồi tạo nhánh theo quy ước đặt tên ở mục 4.
3. Viết code và test đúng kế hoạch đã duyệt. Phát sinh việc ngoài phạm vi issue thì ghi lại để đề xuất issue mới, không tự làm.
4. Chạy `./mvnw test` (cần Docker Desktop đang chạy) và sửa đến khi qua.
5. Tự kiểm theo `docs/review-checklist.md`.
6. Commit theo quy ước ở mục 4; có thể chia nhiều commit theo từng phần có nghĩa.

## Bước 4: Báo cáo để người dùng kiểm tra

Trình bày:
- Bảng Acceptance Criteria: tiêu chí, đạt hay chưa, bằng chứng (file:dòng, tên test).
- Danh sách file thay đổi (`git diff --stat main...HEAD`).
- Cách chạy thử: lệnh, API và dữ liệu mẫu để gọi.
- Việc ngoài phạm vi đã phát hiện (nếu có), đề xuất tạo issue mới.

Nhắc người dùng đọc lại toàn bộ thay đổi và chạy thử, vì người mở PR chịu trách nhiệm cho code.

⏸ **Điểm dừng 2:** chờ người dùng xác nhận đã kiểm tra và đồng ý push.

## Bước 5: Mở pull request

1. `git push -u origin <tên nhánh>`
2. Soạn mô tả theo đúng `.github/pull_request_template.md`: `Closes #<số>`, đã làm gì, cách kiểm tra, đánh dấu các ô tự kiểm đã đạt. Ô nào chưa đạt thì để trống và ghi lý do.

⏸ **Điểm dừng 3:** cho người dùng xem tiêu đề và mô tả PR, chờ đồng ý rồi mới tạo:
`gh pr create --base main --head <tên nhánh> --title "<tiêu đề>" --body-file <file mô tả>`

3. Nhắc người dùng chọn người review theo vòng luân phiên (PR nền tảng nhờ 2 người).

Không bao giờ approve hay merge pull request.
