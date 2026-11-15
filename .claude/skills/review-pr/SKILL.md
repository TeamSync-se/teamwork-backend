---
name: review-pr
description: Review một pull request của repo teamwork-backend theo checklist của nhóm (docs/review-checklist.md) và Acceptance Criteria của issue được liên kết, rồi trả kết quả theo một form cố định. Dùng khi người dùng muốn review PR, kiểm tra PR có đạt Acceptance Criteria không, hoặc gõ /review-pr.
argument-hint: "[số PR, để trống nếu review PR của nhánh hiện tại]"
---

# Review pull request theo form của nhóm

Bạn hỗ trợ một thành viên nhóm review pull request. Kết quả để **người** đọc và quyết định; bạn không approve thay họ.

## Nguyên tắc

- Quy tắc review lấy từ `docs/review-checklist.md`. Luôn đọc file này ở đầu mỗi lần review và làm theo nó; không dùng checklist nhớ sẵn. Nếu file không tồn tại, báo người dùng và dừng.
- Không chạy lệnh làm thay đổi repo hay GitHub nếu người dùng chưa yêu cầu: không approve, không merge, không đăng bình luận, không checkout nhánh khác.
- Mọi nhận định phải có bằng chứng: đường dẫn file kèm số dòng, tên test, hoặc trích đoạn diff. Không kiểm được từ code thì ghi là "Không kiểm được", không đoán là đạt.
- Tìm lỗi lập trình chung không phải việc chính của skill này. Nếu người dùng muốn, gợi ý chạy thêm `/code-review`.
- Viết kết quả bằng tiếng Việt.

## Các bước

1. **Xác định PR.** Số PR là `$ARGUMENTS`. Nếu trống, lấy PR của nhánh hiện tại: `gh pr view --json number`. Không tìm thấy PR thì hỏi người dùng số PR.
   Trên Windows, nếu lệnh `gh` không được nhận ra, dùng đường dẫn `C:\Program Files\GitHub CLI\gh.exe`.
2. **Đọc checklist:** `docs/review-checklist.md`.
3. **Lấy thông tin PR:**
   `gh pr view <số> --json number,title,body,author,headRefName,baseRefName,files,closingIssuesReferences,reviewDecision,statusCheckRollup`
4. **Tìm issue được liên kết** từ `closingIssuesReferences`; nếu trống, tìm `Closes #`, `Fixes #` hoặc `Resolves #` trong mô tả PR. Đọc issue:
   `gh issue view <số issue> --json number,title,body,labels`
5. **Kiểm tra điều kiện mục 0 của checklist trước.** Nếu PR không liên kết issue, hoặc issue còn nhãn `chua-co-AC`, hoặc mục Acceptance Criteria còn trống, thì dừng và trả kết luận "Chưa đủ điều kiện review" cùng lý do. Không review nội dung.
   Áp dụng phần **Ngoại lệ** ở mục 0 của checklist: nếu mọi file thay đổi đều là tài liệu hoặc quy trình, PR không cần issue; bảng Acceptance Criteria thay bằng dòng "Không áp dụng (PR tài liệu)" và chỉ xét các mục checklist còn áp dụng.
6. **Đọc thay đổi:** `gh pr diff <số>`. Khi cần ngữ cảnh, đọc thêm file đầy đủ trong repo.
7. **Đối chiếu từng dòng Acceptance Criteria** của issue với diff, và các mục 1 đến 6 của checklist.
8. **Chỉ chạy test khi người dùng đồng ý**, vì phải checkout nhánh của PR.
9. **Trả kết quả theo form dưới đây**, không thêm phần khác.

## Form kết quả

```markdown
## Review PR #<số>: <tiêu đề>

**Issue:** #<số> · **Người mở PR:** <login> · **CI:** <xanh / đỏ / chưa có>
**Kết luận đề xuất:** Approve / Request changes / Chưa đủ điều kiện review

### Acceptance Criteria
| # | Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | <nguyên văn dòng AC> | ✅ Đạt / ❌ Chưa đạt / ⚠️ Không kiểm được | <file:dòng, tên test> |

### Checklist của nhóm
| Mục | Kết quả | Ghi chú |
|---|---|---|
| 2. Bảo mật và quyền | ✅ / ❌ / ⚠️ / Không áp dụng | <ngắn gọn> |
| 3. API và hợp đồng | ... | ... |
| 4. Cơ sở dữ liệu | ... | ... |
| 5. Kiểm thử | ... | ... |
| 6. Mã nguồn | ... | ... |

### Cần sửa trước khi merge
1. <vấn đề> (<file:dòng>): <sửa thế nào>

### Gợi ý (không bắt buộc)
- <gợi ý>
```

Quy tắc kết luận theo phần "Kết luận review" trong checklist. Không có mục nào cần sửa thì ghi "Không có" ở phần đó.

## Sau khi trả kết quả

Nhắc người dùng rằng approve phải do họ tự đọc lại và bấm trên GitHub. Nếu họ muốn đăng kết quả lên PR, chỉ đăng dạng bình luận, không bao giờ approve:
`gh pr review <số> --comment --body-file <file chứa kết quả>`
