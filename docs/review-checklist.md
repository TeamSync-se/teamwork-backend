# Checklist review pull request

Người review đọc PR theo thứ tự dưới đây. Đây là **nguồn duy nhất** cho việc review: PR template và skill `review-pr` đều dựa vào file này, nên muốn đổi quy tắc review thì chỉ sửa file này.

Nhánh `main` được bảo vệ: mọi thay đổi phải qua pull request và cần **1 approve** của một thành viên khác. Commit mới sau khi đã được approve sẽ làm mất approve cũ, và mọi bình luận phải được giải quyết trước khi merge.

## 0. Điều kiện để bắt đầu review

- [ ] PR liên kết đúng issue bằng `Closes #số` trong mô tả
- [ ] Issue đã có Acceptance Criteria (không còn nhãn `chua-co-AC`)
- [ ] CI chạy xanh (khi đã có CI)
- [ ] PR chỉ làm việc của issue đó; việc khác tách ra PR khác

Thiếu một trong các mục trên thì trả PR lại cho người mở, chưa review nội dung.

**Ngoại lệ:** PR chỉ sửa tài liệu hoặc quy trình (`docs/`, `.github/`, `.claude/`, `README.md`) được phép không có issue, nhưng mô tả PR phải ghi rõ lý do thay đổi. Khi đó bỏ qua mục 1 và chỉ xét các mục còn áp dụng.

## 1. Acceptance Criteria

- [ ] Đạt **từng dòng** Acceptance Criteria trong issue
- [ ] Dòng nào chưa đạt thì PR phải ghi rõ lý do và đã được nhóm đồng ý

## 2. Bảo mật và quyền (NFR-01, NFR-02, NFR-04)

- [ ] API mới yêu cầu đăng nhập, trừ `/api/auth/**` và `/api/health`
- [ ] API theo nhóm dùng cơ chế kiểm tra quyền chung, không tự viết lại
- [ ] Có test chứng minh người ngoài nhóm bị chặn
- [ ] Người gọi và vai trò lấy từ token và CSDL, không tin dữ liệu frontend gửi lên
- [ ] Không có khóa, mật khẩu hay token thật trong code, test, log hoặc file cấu hình

## 3. API và hợp đồng

- [ ] Lỗi trả theo định dạng lỗi chung, mã HTTP đúng quy ước
- [ ] Tên trường `camelCase`, đường dẫn theo quy ước trong `docs/api/conventions.md` (khi đã có)
- [ ] API thêm mới hoặc thay đổi thì hợp đồng API đã được cập nhật và frontend đã biết
- [ ] Response không lộ dữ liệu nhạy cảm (mật khẩu, mã băm, thông tin của nhóm khác)

## 4. Cơ sở dữ liệu

- [ ] Thay đổi CSDL bằng file migration mới, đặt tên theo quy ước ở mục 4 của [huong-dan-dong-gop.md](huong-dan-dong-gop.md)
- [ ] **Không sửa** file migration đã merge vào `main`
- [ ] Tên bảng, cột khớp ERD; có khóa ngoại và ràng buộc cần thiết
- [ ] Thao tác ghi nhiều bảng nằm trong cùng một giao dịch

## 5. Kiểm thử

- [ ] Có test cho luồng chính và các kịch bản lỗi nêu trong Acceptance Criteria
- [ ] `./mvnw test` chạy qua

## 6. Mã nguồn

- [ ] Code đặt đúng package, nhánh và commit đúng quy ước ở mục 4 của [huong-dan-dong-gop.md](huong-dan-dong-gop.md)
- [ ] Code do AI viết đã được người mở PR đọc và giải thích được
- [ ] Không còn code thừa, log debug hay `TODO` không gắn issue
- [ ] Tên lớp, hàm, biến rõ nghĩa

## Kết luận review

- **Approve:** đạt hết mục 0 đến 5.
- **Request changes:** có ít nhất một mục ở 0 đến 5 chưa đạt; ghi rõ cần sửa gì và ở đâu.
- Góp ý ở mục 6 hoặc ý kiến cá nhân thì ghi là **gợi ý**, không chặn merge.

## Thỏa thuận trong nhóm

- Review trong vòng 24 giờ kể từ khi được nhờ.
- Luân phiên người review để ai cũng đọc code của mọi người.
- PR nền tảng (đăng nhập, tạo nhóm, kiểm tra quyền, migration CSDL) nên nhờ 2 người xem, dù quy tắc chỉ bắt buộc 1 approve.
- AI review (skill `review-pr`, `/code-review`) chỉ để hỗ trợ; approve phải do người đọc code và bấm.
