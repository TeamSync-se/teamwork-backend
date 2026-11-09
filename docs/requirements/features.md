# Hệ thống hỗ trợ làm việc nhóm trực tuyến — Yêu cầu tính năng

Ký hiệu: **[B]** Bắt buộc · **[N]** Nâng cao

Mã trong ngoặc là mã truy vết tới PA02 (FR, NFR, UC) và PA01 (QĐ).

Mỗi dòng là một issue trên GitHub. **Milestone 1** gồm mọi dòng [B] (33 issue), **Milestone 3** gồm mọi dòng [N] (10 issue).

## F1. Tài khoản và nhóm làm việc

- [B] Đăng ký tài khoản bằng họ tên, email, mật khẩu; mật khẩu được băm; từ chối email đã tồn tại. (FR-01, UC01, QĐ01)
- [B] Đăng nhập và xác thực; từ chối truy cập chức năng nhóm khi chưa đăng nhập. (FR-02, UC02, NFR-01)
- [B] Xem và cập nhật thông tin cá nhân. (FR-03)
- [B] Tạo nhóm làm việc; người tạo là Trưởng nhóm; xem danh sách nhóm đã tham gia. (FR-04, UC03, QĐ02)
- [B] Mời thành viên bằng email; người được mời chấp nhận hoặc từ chối lời mời. (FR-04, FR-05, UC04, UC05)
- [B] Xem danh sách thành viên và loại bỏ thành viên khỏi nhóm. (FR-05, QĐ03)
- [B] Cập nhật thông tin nhóm và phân quyền vai trò; chặn người không đủ quyền. (FR-06, QĐ03)
- [N] Lời mời có thời hạn và có thể thu hồi.
- [N] Đổi mật khẩu, quên mật khẩu.

## F2. Trao đổi thông tin và tài nguyên

- [B] Nhắn tin nhóm: gửi, nhận, xem lịch sử; chỉ thành viên nhóm mới thấy. (FR-07, UC06, QĐ04)
- [B] Tin nhắn cập nhật gần thời gian thực qua WebSocket/STOMP; mất kết nối không làm sai dữ liệu đã lưu. (NFR-06)
- [B] Tải lên tài nguyên (tài liệu, hình ảnh, tệp) lên Cloudflare R2; kiểm tra tệp hợp lệ, báo lỗi khi thất bại. (FR-08, UC07)
- [B] Xem danh sách và tải xuống tài nguyên; Backend kiểm tra quyền, không dùng đường dẫn công khai để phân quyền. (FR-09, NFR-03)
- [B] Thread thảo luận gắn với một tài nguyên hoặc một nhiệm vụ; quyền truy cập theo đối tượng gắn. (FR-10, UC08, QĐ06)
- [B] AI tóm tắt nội dung trao đổi; chỉ xử lý nội dung người yêu cầu có quyền xem; báo lỗi khi dịch vụ AI không phản hồi. (FR-11, UC09, QĐ09)
- [N] Tìm kiếm tin nhắn và tài nguyên trong nhóm.
- [N] Xem trước ảnh và PDF ngay trong ứng dụng.

## F3. Nhiệm vụ, tiến độ và thông báo

- [B] Tạo nhiệm vụ với tên, mô tả, thời hạn thuộc một nhóm; chỉ người có quyền mới tạo. (FR-12, UC10, QĐ05)
- [B] Phân công nhiệm vụ cho thành viên của đúng nhóm; từ chối phân công cho người ngoài nhóm. (FR-13, UC10)
- [B] Cập nhật trạng thái nhiệm vụ bởi người được giao. (FR-14, UC11)
- [B] Xem danh sách nhiệm vụ, người phụ trách, trạng thái, thời hạn. (FR-15)
- [B] Thông báo trong ứng dụng khi được giao việc, có thay đổi quan trọng hoặc sắp đến hạn; bộ định thời kiểm tra deadline. (FR-16, UC12)
- [N] Gửi email thông báo dùng chung một hạ tầng: lời mời vào nhóm, nhiệm vụ mới hoặc sắp đến hạn, lịch họp đã chốt.
- [N] Thông báo đẩy trên trình duyệt khi người dùng cho phép.

## F4. Cuộc họp

- [B] Tạo cuộc họp thuộc nhóm, nhập nội dung, chọn thành viên liên quan và đề xuất nhiều khung giờ dự kiến. (FR-17, FR-18, UC13)
- [B] Thành viên bình chọn các khung giờ có thể tham dự. (FR-19, UC14)
- [B] Tổng hợp số người theo từng khung giờ và gợi ý khung giờ phù hợp nhất; kết quả chỉ mang tính đề xuất. (FR-20, QĐ07)
- [B] Người tổ chức chốt giờ họp chính thức; lưu vào lịch nhóm. (FR-21, UC15)
- [B] Xem lịch họp và liên kết họp bên ngoài; không xây dựng gọi video. (FR-22)
- [N] Cảnh báo khi cuộc họp bị trùng lịch.

## F5. Dashboard tổng hợp

- [B] Dashboard cá nhân tổng hợp nhiệm vụ được giao, thời hạn sắp tới, lịch họp sắp diễn ra từ các nhóm có quyền truy cập; chuyển giữa chế độ tổng hợp và xem theo từng nhóm. (FR-23, FR-24, UC16, QĐ02, QĐ08)
- [N] Thống kê tiến độ nhiệm vụ theo thành viên.

## F6. Bảo mật và chất lượng hệ thống

- [B] Mọi API đọc/ghi dữ liệu nghiệp vụ phải xác thực và kiểm tra quyền theo nhóm ở Backend; có test truy cập chéo nhóm. (NFR-01, NFR-02, QĐ04)
- [B] Khóa dịch vụ AI, email, lưu trữ tệp cấu hình qua biến môi trường, không đưa vào mã nguồn. (NFR-04)
- [B] Báo lỗi rõ ràng khi dữ liệu không hợp lệ, không có quyền hoặc dịch vụ ngoài không phản hồi. (NFR-09)
- [N] Đặc tả API tự sinh bằng OpenAPI/Swagger.
- [N] Kế hoạch sao lưu và khôi phục CSDL. (NFR-10)

## NT. Nền tảng tích hợp, kiểm thử và triển khai

Không phải tính năng, nhưng cần để hệ thống chạy được. Ưu tiên làm trước, trừ dữ liệu mẫu làm cuối Milestone 1.

- [B] Cấu hình CORS và kết nối thử frontend với backend. (PA02 mục 2.4)
- [B] Tạo tài khoản dịch vụ ngoài (Cloudflare R2, LLM, SMTP) và file `.env.example`. (NFR-04)
- [B] Hạ tầng kiểm thử với Testcontainers. (PA01 mục 4.4)
- [B] CI bằng GitHub Actions: build và chạy test khi push và khi mở pull request.
- [B] Dockerfile và chạy backend bằng Docker Compose. (PA01 mục 4.5)
- [B] Dữ liệu mẫu cho demo.
