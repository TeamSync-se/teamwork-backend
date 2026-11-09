# Hướng dẫn làm việc chung

File này thống nhất cách cả nhóm làm việc, kể cả khi code do AI viết. Đây là **nguồn chính**: các file hướng dẫn cho AI (`CLAUDE.md`, `AGENTS.md`, `.github/copilot-instructions.md`) chỉ trỏ về đây. Muốn đổi quy tắc thì sửa file này qua pull request để cả nhóm cùng duyệt.

## 1. Nguyên tắc

1. **AI viết code, người chịu trách nhiệm.** Trước khi mở pull request, bạn phải đọc hết thay đổi và giải thích được nó. Không mở PR cho code mình không hiểu.
2. **Đúng phạm vi issue.** Thấy việc khác cần làm thì tạo issue mới, không gộp vào PR đang làm.
3. **Quy tắc nằm trong repo**, không nằm trong tin nhắn hay trí nhớ của ai. Thống nhất gì mới thì ghi vào file này.
4. **AI không bao giờ** approve, merge, hay push thẳng vào `main`.

## 2. Tài liệu phải đọc

| Khi nào | Đọc gì |
|---|---|
| Mỗi lần làm issue | Chính issue đó (Phụ thuộc, Description, Acceptance Criteria, Solution) |
| Mỗi lần làm issue | File này và [review-checklist.md](review-checklist.md) |
| Mỗi lần làm issue | Use Case và màn hình prototype ghi trong issue ([PA02.md](requirements/PA02.md)) |
| Lần đầu tham gia | [README.md](../README.md), các quy định QĐ01 đến QĐ09 ([PA01.md](requirements/PA01.md)), mục 2.5 và 3.2.2 của PA02 |
| Khi đã có | `docs/api/conventions.md`, hợp đồng API của phân hệ, ERD, hướng dẫn kiểm tra quyền nhóm |

## 3. Quy trình làm một issue

Cột **Ai** cho biết bước đó người làm hay giao cho AI làm.

| # | Bước | Ai |
|---|---|---|
| 1 | Nhận issue: tự gán mình vào **Assignees** | Người |
| 2 | Kiểm tra mục **Phụ thuộc**: phụ thuộc cứng phải đã đóng mới chạy thử được. Chưa đóng thì chỉ làm phần không cần chúng, hoặc báo người làm issue đó | Người |
| 3 | Issue còn nhãn `chua-co-AC`: viết Acceptance Criteria từ Use Case trong PA02 (AI có thể soạn nháp), nhờ một bạn đọc lại, rồi gỡ nhãn. **Chưa gỡ nhãn thì chưa được viết code** | Người, AI hỗ trợ |
| 4 | Issue có API mới: chốt hợp đồng API với frontend trước | Người |
| 5 | Đọc tài liệu ở mục 2 và **trình bày kế hoạch**: file sẽ tạo hoặc sửa, migration, API, test, ứng với từng dòng Acceptance Criteria | AI |
| 6 | **Duyệt kế hoạch** trước khi cho viết code | Người |
| 7 | Tạo nhánh từ `main` mới nhất, viết code và test theo mục 4, chạy `./mvnw test` | AI |
| 8 | Đọc lại toàn bộ thay đổi, chạy thử ứng dụng và gọi thử API | Người |
| 9 | Tự kiểm theo review-checklist, commit theo quy ước ở mục 4 | AI |
| 10 | Push và mở pull request theo mẫu, có `Closes #số`. AI chỉ làm bước này khi người đồng ý | Người hoặc AI |
| 11 | Chọn người review theo vòng luân phiên; PR nền tảng nhờ 2 người | Người |
| 12 | Xử lý góp ý: AI có thể sửa, người kiểm lại rồi push vào cùng nhánh và nhờ review lại | Người, AI hỗ trợ |
| 13 | Đủ 1 approve, hết bình luận, CI xanh: người mở PR bấm merge, rồi xóa nhánh | Người |
| 14 | Báo người làm các issue đang chờ issue này, và báo frontend nếu API đã sẵn sàng | Người |

Lệnh Git tương ứng với bước 7, 10 và 13:

```
git checkout main
git pull
git checkout -b feature/8-dang-ky
# ... viết code, commit ...
git push -u origin feature/8-dang-ky
# sau khi merge
git checkout main
git pull
git branch -d feature/8-dang-ky
```

Nếu `main` có code mới trong lúc PR đang chờ: `git pull origin main` trên nhánh của mình, sửa xung đột, chạy lại test, push.

## 4. Quy ước code

### Nhánh và commit

- Nhánh: `feature/<số issue>-<ten-ngan>`, `fix/<số issue>-<ten-ngan>`, `docs/<ten-ngan>`. Ví dụ `feature/8-dang-ky`.
- Commit: `<loại>: <mô tả ngắn> (#số issue)`. Loại gồm `feat`, `fix`, `test`, `refactor`, `docs`, `chore`. Ví dụ `feat: thêm API đăng ký tài khoản (#8)`.
- Mỗi commit một thay đổi có nghĩa; không gộp nhiều việc không liên quan.

### Cấu trúc package

```
com.teamwork.backend
├── config/        Security, CORS, WebSocket, cấu hình chung
├── common/        Lỗi chung, kiểm tra quyền nhóm, tiện ích dùng chung
├── auth/  user/  group/                 Phân hệ 1
├── message/  resource/  thread/  ai/    Phân hệ 2
├── task/  notification/                 Phân hệ 3
├── meeting/                             Phân hệ 4
└── dashboard/                           Phân hệ 5
```

Trong mỗi package: entity `Xxx`, `XxxRepository`, `XxxService`, `XxxController`; DTO đặt trong `dto/`, viết dạng `record`, tên `XxxRequest` và `XxxResponse`.

- Controller chỉ nhận request, gọi service, trả response. Logic nghiệp vụ và kiểm tra quyền nằm ở service.
- Không trả entity trực tiếp ra API, luôn qua DTO.
- Đường dẫn API bắt đầu bằng `/api`, danh từ số nhiều; dữ liệu theo nhóm nằm dưới `/api/groups/{groupId}/...`.
- Thêm thư viện mới vào `pom.xml` thì ghi lý do trong mô tả PR.

### Cơ sở dữ liệu

- Mọi thay đổi CSDL bằng file migration Flyway trong `src/main/resources/db/migration/`.
- Tên file đánh số theo thời gian tạo, để ba người làm song song không bị trùng: `V<yyyyMMddHHmm>__<mo_ta>.sql`, ví dụ `V202611121430__create_work_groups.sql`. File `V1__create_users_table.sql` có sẵn giữ nguyên.
- **Không sửa** file migration đã merge vào `main`; cần đổi thì tạo file mới.
- Tên bảng và cột viết `snake_case`, tên bảng số nhiều.
- Trên máy cá nhân, nếu Flyway báo lỗi thứ tự migration sau khi `git pull`, chạy `docker compose down -v` rồi `docker compose up -d` để tạo lại CSDL (mất dữ liệu thử trên máy).

### Kiểm thử

- Mỗi API có test cho luồng chính và các kịch bản lỗi trong Acceptance Criteria.
- API theo nhóm luôn có test người ngoài nhóm bị chặn.
- `./mvnw test` phải chạy qua trước khi mở PR.

### Bảo mật

- Không đưa khóa, mật khẩu hay token thật vào code, test, log hay file cấu hình; đọc từ biến môi trường hoặc file `.env` (đã nằm trong `.gitignore`).
- Không ghi mật khẩu, mã băm hay khóa ra log hoặc response.

## 5. Làm việc với AI

Mỗi công cụ AI tự đọc file hướng dẫn của nó khi mở dự án, và các file đó trỏ về đây:

| Công cụ | File |
|---|---|
| Claude Code | `CLAUDE.md` |
| GitHub Copilot | `.github/copilot-instructions.md` |
| Công cụ khác (Codex, Cursor, …) | `AGENTS.md` |

**Câu lệnh mẫu để bắt đầu một issue**, dùng được với mọi công cụ:

> Làm issue #8 theo docs/huong-dan-dong-gop.md. Trước khi viết code, đọc issue, review-checklist và Use Case liên quan, rồi trình bày kế hoạch để mình duyệt.

Người dùng Claude Code gõ `/lam-issue 8` để chạy đúng quy trình ở mục 3. Khi review, gõ `/review-pr <số PR>`.

**Không giao cho AI:** quyết định thay đổi phạm vi issue, sửa migration đã merge, thêm thư viện mà không giải thích, approve hoặc merge pull request.

## 6. Giao tiếp

- Trao đổi về một issue thì ghi vào chính issue hoặc PR đó, để có lịch sử cho cả nhóm.
- Bị chặn quá một ngày thì báo nhóm.
- Review trong vòng 24 giờ kể từ khi được nhờ.
