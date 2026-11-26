# Hợp đồng API: phân hệ cuộc họp (F4)

> **Hiệu lực:** khi được merge vào `main`. Muốn thay đổi thì mở pull request theo mục 8 của [conventions.md](conventions.md).
> **Issue backend:** #27 Tạo cuộc họp và đề xuất khung giờ, #28 Bình chọn khung giờ họp, #29 Tổng hợp bình chọn và gợi ý khung giờ, #30 Chốt lịch họp chính thức, #31 Xem lịch họp và liên kết họp.
> **Issue frontend:** `TeamSync-se/teamsync-frontend` #22–#26.
> **Use Case:** UC13, UC14, UC15. **Màn hình prototype:** 5.1–5.5.
> Các quy ước chung (định dạng lỗi, cách gửi token, định dạng thời điểm, phân trang, quyền theo nhóm) nằm ở [conventions.md](conventions.md).

| API | Mục đích | Issue BE | Cần đăng nhập |
|---|---|---|---|
| `POST /api/groups/{groupId}/meetings` | Tạo cuộc họp và đề xuất khung giờ | #27 | Có |
| `GET /api/groups/{groupId}/meetings` | Danh sách cuộc họp của nhóm | #31 | Có |
| `GET /api/groups/{groupId}/meetings/{meetingId}` | Chi tiết cuộc họp | #31 | Có |
| `POST /api/groups/{groupId}/meetings/{meetingId}/votes` | Bình chọn, cập nhật hoặc hủy bình chọn khung giờ | #28 | Có |
| `GET /api/groups/{groupId}/meetings/{meetingId}/votes` | Tổng hợp bình chọn và gợi ý khung giờ | #29 | Có |
| `POST /api/groups/{groupId}/meetings/{meetingId}/confirm` | Chốt lịch họp chính thức | #30 | Có |

## 1. Quy tắc chung của phân hệ cuộc họp

- **Quyền Người tổ chức:** Mọi thành viên trong nhóm (`LEADER` và `MEMBER`) đều tạo được cuộc họp và trở thành người tổ chức (`creator`). Chỉ `creator` của cuộc họp hoặc `LEADER` của nhóm được chốt lịch chính thức (`confirm`).
- **Quyền xem và bình chọn:** Mọi thành viên hiện tại của nhóm (kể cả người không nằm trong danh sách mời) đều xem được danh sách cuộc họp, chi tiết cuộc họp và kết quả tổng hợp bình chọn. Chỉ thành viên nằm trong danh sách tham gia (`participants`) và hiện còn thuộc nhóm mới được bình chọn.
- **Trạng thái cuộc họp (`status`):** Milestone 1 chỉ có hai giá trị là `"VOTING"` (đang bình chọn) và `"CONFIRMED"` (đã chốt lịch). Chưa có chức năng hủy cuộc họp và hạn chót bình chọn.
- **Thành viên hiện tại:** Mọi số liệu và danh sách người tham gia (`participants`, `totalInvited`, `voters`, `voteCount`, `totalVoted`, `hasVoted`) chỉ tính những người **hiện còn là thành viên** của nhóm `groupId`. Người đã bị loại khỏi nhóm (#14) vẫn giữ bản ghi lịch sử trong CSDL nhưng bị bỏ qua khi tính và khi trả về. Riêng `creator` luôn được trả về trong trường `creator` dù người đó đã rời nhóm.
- **Thời gian:** Request nhận chuỗi thời điểm ISO 8601 có múi giờ hoặc offset (ví dụ `"2026-11-28T09:00:00+07:00"` hoặc `"2026-11-28T02:00:00Z"`). Backend chuẩn hóa lưu và trả về UTC có hậu tố `Z` theo mục 2 của [conventions.md](conventions.md). Frontend tự đổi sang giờ Việt Nam (`Asia/Ho_Chi_Minh`) khi hiển thị. Mọi so sánh với thời điểm "hiện tại" dùng đồng hồ máy chủ (bean `Clock` ở backend).
- **Tham số đường dẫn và truy cập nhóm:** `groupId` hoặc `meetingId` không phải số nguyên hợp lệ trả `400 BAD_REQUEST`. Người không thuộc nhóm `groupId`, nhóm không tồn tại, hoặc `meetingId` không tồn tại / không thuộc nhóm `groupId` trả `404 NOT_FOUND` theo mục 6 của [conventions.md](conventions.md).
- **Ranh giới hệ thống (FR-22):** Hệ thống chỉ lưu và hiển thị `externalLink` (Google Meet, Zoom, Microsoft Teams...); không xây dựng chức năng gọi video.

### Mã lỗi riêng của phân hệ (`MeetingErrorCode`)

Ngoài các mã lỗi chung ở mục 5 của [conventions.md](conventions.md) (`VALIDATION_ERROR`, `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, ...), phân hệ cuộc họp sử dụng các mã lỗi sau:

| `code` | HTTP | Khi nào |
|---|---|---|
| `INVALID_TIME_SLOT` | 400 | Khi tạo cuộc họp (#27): `startTime >= endTime`, `startTime <= hiện tại`, hoặc có hai khung giờ trùng khớp hoàn toàn cả `startTime` lẫn `endTime` sau khi chuẩn hóa UTC (hai khung giao nhau một phần vẫn hợp lệ). Khi chốt lịch (#30): khung giờ được chọn đã kết thúc (`endTime <= hiện tại`) |
| `INVALID_PARTICIPANT` | 400 | Có ID trong `invitedUserIds` không tồn tại hoặc không thuộc nhóm `groupId` |
| `INVALID_SLOT_SELECTION` | 400 | ID khung giờ gửi lên khi bình chọn (#28) hoặc chốt lịch (#30) không tồn tại hoặc không thuộc cuộc họp `meetingId` |
| `MEETING_ALREADY_CONFIRMED` | 409 | Gửi bình chọn (#28) hoặc chốt lịch (#30) khi cuộc họp đã ở trạng thái `CONFIRMED` |

## 2. Cấu trúc dùng chung: Chi tiết cuộc họp

Cấu trúc này được dùng chung cho response của `POST /api/groups/{groupId}/meetings` (`201 Created`), `GET /api/groups/{groupId}/meetings/{meetingId}` (`200 OK`) và `POST /api/groups/{groupId}/meetings/{meetingId}/confirm` (`200 OK`). Trường chưa có giá trị luôn trả `null`, không bỏ trường.

Ví dụ response ở trạng thái `"VOTING"`:

```json
{
  "id": 10,
  "groupId": 3,
  "title": "Họp kiểm tra tiến độ sprint 2",
  "description": "Rà soát các API đã hoàn thiện và thống nhất kế hoạch tích hợp.",
  "externalLink": "https://meet.google.com/cnpm-progress-sync",
  "status": "VOTING",
  "confirmedSlotId": null,
  "confirmedStartTime": null,
  "confirmedEndTime": null,
  "creator": {
    "id": 1,
    "fullName": "Nguyễn Minh Tuấn"
  },
  "isInvited": true,
  "totalInvited": 3,
  "participants": [
    { "id": 2, "fullName": "Lê Hoàng An" },
    { "id": 1, "fullName": "Nguyễn Minh Tuấn" },
    { "id": 4, "fullName": "Phạm Quỳnh Chi" }
  ],
  "timeSlots": [
    {
      "id": 101,
      "startTime": "2026-11-28T02:00:00Z",
      "endTime": "2026-11-28T03:00:00Z"
    },
    {
      "id": 102,
      "startTime": "2026-11-28T07:00:00Z",
      "endTime": "2026-11-28T08:00:00Z"
    }
  ],
  "createdAt": "2026-11-26T07:00:00Z",
  "updatedAt": "2026-11-26T07:00:00Z"
}
```

Khi cuộc họp ở trạng thái `"CONFIRMED"`, các trường chốt lịch có giá trị cụ thể:

```json
{
  "status": "CONFIRMED",
  "confirmedSlotId": 102,
  "confirmedStartTime": "2026-11-28T07:00:00Z",
  "confirmedEndTime": "2026-11-28T08:00:00Z",
  "updatedAt": "2026-11-26T09:15:00Z"
}
```

| Trường | Kiểu | Mô tả |
|---|---|---|
| `id` | `number` | ID cuộc họp |
| `groupId` | `number` | ID nhóm chứa cuộc họp |
| `title` | `string` | Tiêu đề / nội dung chính cuộc họp |
| `description` | `string \| null` | Mô tả chi tiết / ghi chú |
| `externalLink` | `string \| null` | Liên kết nền tảng họp trực tuyến bên ngoài |
| `status` | `"VOTING" \| "CONFIRMED"` | Trạng thái cuộc họp |
| `confirmedSlotId` | `number \| null` | ID khung giờ đã chốt (`null` nếu `VOTING`) |
| `confirmedStartTime` | `string \| null` | Thời điểm bắt đầu đã chốt, UTC (`null` nếu `VOTING`) |
| `confirmedEndTime` | `string \| null` | Thời điểm kết thúc đã chốt, UTC (`null` nếu `VOTING`) |
| `creator` | `{ id: number, fullName: string }` | Người tạo / tổ chức cuộc họp |
| `isInvited` | `boolean` | Người gọi API hiện có trong danh sách mời (`participants`) hay không |
| `totalInvited` | `number` | Số người trong `participants` hiện còn là thành viên nhóm (tính cả `creator` nếu còn trong nhóm) |
| `participants` | `{ id: number, fullName: string }[]` | Danh sách thành viên tham gia còn thuộc nhóm, sắp xếp theo `fullName` tăng dần, rồi `id` tăng dần |
| `timeSlots` | `{ id: number, startTime: string, endTime: string }[]` | Danh sách khung giờ đề xuất, sắp xếp theo `startTime` tăng dần, `endTime` tăng dần, rồi `id` tăng dần |
| `createdAt` | `string` | Thời điểm tạo (UTC) |
| `updatedAt` | `string` | Thời điểm cập nhật gần nhất (UTC) |

## 3. Tạo cuộc họp và đề xuất khung giờ: `POST /api/groups/{groupId}/meetings`

**Issue:** BE #27 · FE #22 · **Use Case:** UC13 · **Màn hình:** 5.2

Tạo cuộc họp mới thuộc nhóm `groupId` kèm danh sách người tham gia và từ 1 đến 20 khung giờ dự kiến trong cùng một giao dịch. Người tạo (`creator`) luôn lấy từ JWT token và tự động có mặt trong danh sách tham gia (`participants`), kể cả khi `invitedUserIds` không chứa ID của người tạo.

### Gửi lên

```json
{
  "title": "Họp kiểm tra tiến độ sprint 2",
  "description": "Rà soát các API đã hoàn thiện và thống nhất kế hoạch tích hợp.",
  "externalLink": "https://meet.google.com/cnpm-progress-sync",
  "invitedUserIds": [2, 4],
  "timeSlots": [
    {
      "startTime": "2026-11-28T09:00:00+07:00",
      "endTime": "2026-11-28T10:00:00+07:00"
    },
    {
      "startTime": "2026-11-28T14:00:00+07:00",
      "endTime": "2026-11-28T15:00:00+07:00"
    }
  ]
}
```

| Trường | Bắt buộc | Quy tắc |
|---|---|---|
| `title` | Có | Không được trống hoặc chỉ có khoảng trắng; tối đa 200 ký tự (backend `trim` hai đầu) |
| `description` | Không | Tối đa 2000 ký tự; trống hoặc chỉ khoảng trắng lưu thành `null` |
| `externalLink` | Không | Backend `trim` hai đầu; trống hoặc chỉ khoảng trắng lưu thành `null`. Nếu có nội dung: tối đa 500 ký tự và phải là URL `http://` hoặc `https://` có host hợp lệ |
| `invitedUserIds` | Không | Mảng `number[]`, không chứa phần tử `null`; backend khử trùng lặp. Không truyền, `null` hoặc `[]` đều hợp lệ (danh sách tham gia chỉ gồm `creator`). Mọi ID phải là thành viên hiện tại của nhóm `groupId` |
| `timeSlots` | Có | Mảng từ 1 đến 20 phần tử `{ startTime, endTime }`, không chứa phần tử `null`; mỗi phần tử bắt buộc có `startTime` và `endTime` đúng chuẩn ISO 8601 có múi giờ hoặc offset |

### Thành công: `201 Created`

Trả về cấu trúc **Chi tiết cuộc họp** (mục 2) với `status: "VOTING"` và `confirmedSlotId`, `confirmedStartTime`, `confirmedEndTime` đều bằng `null`.

**Thông báo (phụ thuộc mềm #26):** Sau khi giao dịch tạo cuộc họp commit, backend gửi thông báo trong ứng dụng loại `MEETING_INVITED` tới các thành viên trong `participants` còn thuộc nhóm (trừ `creator`). Lỗi khi gửi thông báo không làm hỏng việc tạo cuộc họp.

### Lỗi và thứ tự kiểm tra

Thứ tự kiểm tra: `401` → lỗi body / tham số (`400 BAD_REQUEST`, `400 VALIDATION_ERROR`) → `404` → lỗi nghiệp vụ (`400 INVALID_TIME_SLOT`, `400 INVALID_PARTICIPANT`).

| HTTP | `code` | Khi nào | Frontend hiển thị |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | Thiếu token, token sai hoặc hết hạn | Xóa token, chuyển về `/login` |
| 400 | `BAD_REQUEST` | Body không phải JSON hợp lệ, chuỗi thời gian sai chuẩn ISO 8601 có múi giờ/offset, hoặc `groupId` không phải số | Hiện `detail` |
| 400 | `VALIDATION_ERROR` | Vi phạm ràng buộc trường; `errors[].field` có thể là `title`, `description`, `externalLink`, `invitedUserIds`, `invitedUserIds[i]`, `timeSlots`, `timeSlots[i]`, `timeSlots[i].startTime`, `timeSlots[i].endTime` | Hiện lỗi theo `errors[].field` dưới từng ô nhập |
| 404 | `NOT_FOUND` | Nhóm không tồn tại hoặc người gọi không thuộc nhóm `groupId` | Hiện `detail` |
| 400 | `INVALID_TIME_SLOT` | `startTime >= endTime`, `startTime <= hiện tại`, hoặc hai khung giờ trùng khớp hoàn toàn cả `startTime` lẫn `endTime` sau khi chuẩn hóa UTC | Hiện `detail` ở phần chọn khung giờ |
| 400 | `INVALID_PARTICIPANT` | Có ID trong `invitedUserIds` không tồn tại hoặc không thuộc nhóm `groupId` | Hiện `detail` ở phần chọn thành viên |

Ví dụ lỗi `400 VALIDATION_ERROR`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Dữ liệu không hợp lệ",
  "instance": "/api/groups/3/meetings",
  "code": "VALIDATION_ERROR",
  "timestamp": "2026-11-26T07:00:00Z",
  "errors": [
    { "field": "title", "message": "Tiêu đề cuộc họp không được để trống" },
    { "field": "timeSlots[0].startTime", "message": "Thời gian bắt đầu không được để trống" }
  ]
}
```

Ví dụ lỗi `400 INVALID_TIME_SLOT`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Thời gian bắt đầu phải nằm trong tương lai",
  "instance": "/api/groups/3/meetings",
  "code": "INVALID_TIME_SLOT",
  "timestamp": "2026-11-26T07:00:00Z"
}
```

## 4. Danh sách cuộc họp của nhóm: `GET /api/groups/{groupId}/meetings`

**Issue:** BE #31 · FE #26 · **Use Case:** UC15 (FR-22) · **Màn hình:** 5.1

Trả về danh sách cuộc họp của nhóm có phân trang, hỗ trợ lọc theo trạng thái/thời điểm.

### Tham số truy vấn (Query)

| Tham số | Bắt buộc | Mặc định | Quy tắc |
|---|---|---|---|
| `status` | Không | `VOTING` + `UPCOMING` | Bộ lọc: `VOTING` (đang bình chọn), `CONFIRMED` (mọi cuộc họp đã chốt, gồm sắp tới và đã qua), `UPCOMING` (`CONFIRMED` và `confirmedEndTime > hiện tại`), `PAST` (`CONFIRMED` và `confirmedEndTime <= hiện tại`), `ALL` (mọi cuộc họp của nhóm). Không truyền thì trả `VOTING` và `UPCOMING` |
| `page` | Không | `0` | Số nguyên `>= 0` |
| `size` | Không | `20` | Số nguyên từ `1` đến `100` |

*Lưu ý:* `status` ở query là **bộ lọc** (`VOTING`, `CONFIRMED`, `UPCOMING`, `PAST`, `ALL`); trường `status` trong từng phần tử trả về chỉ mang giá trị `"VOTING"` hoặc `"CONFIRMED"`.

### Quy tắc sắp xếp (ổn định khi phân trang)

Danh sách chia làm hai nhóm, **Nhóm 1 luôn đứng trước Nhóm 2**:

1. **Nhóm 1 (`VOTING` và `UPCOMING`):** tăng dần theo thời điểm bắt đầu (`confirmedStartTime` nếu đã chốt; `startTime` sớm nhất trong các khung giờ nếu đang `VOTING`), sau đó `createdAt` tăng dần, `id` tăng dần.
2. **Nhóm 2 (`PAST`):** giảm dần theo `confirmedStartTime`, sau đó `id` giảm dần.

Vì vậy: bộ lọc mặc định, `VOTING` và `UPCOMING` chỉ gồm Nhóm 1; `PAST` chỉ gồm Nhóm 2; `CONFIRMED` và `ALL` có cả Nhóm 1 đứng trước Nhóm 2.

### Thành công: `200 OK`

```json
{
  "items": [
    {
      "id": 10,
      "groupId": 3,
      "title": "Họp kiểm tra tiến độ sprint 2",
      "description": "Rà soát các API đã hoàn thiện và thống nhất kế hoạch tích hợp.",
      "status": "VOTING",
      "externalLink": null,
      "confirmedStartTime": null,
      "confirmedEndTime": null,
      "creator": {
        "id": 1,
        "fullName": "Nguyễn Minh Tuấn"
      },
      "totalInvited": 5,
      "totalVoted": 4,
      "hasMyVote": false,
      "slotCount": 3,
      "isInvited": true,
      "createdAt": "2026-11-26T07:00:00Z"
    },
    {
      "id": 8,
      "groupId": 3,
      "title": "Họp phân chia nhiệm vụ",
      "description": "Thảo luận kiến trúc hệ thống tổng quan và phân bổ đầu việc.",
      "status": "CONFIRMED",
      "externalLink": "https://meet.google.com/xyz-abcd-efg",
      "confirmedStartTime": "2026-11-29T02:00:00Z",
      "confirmedEndTime": "2026-11-29T03:30:00Z",
      "creator": {
        "id": 1,
        "fullName": "Nguyễn Minh Tuấn"
      },
      "totalInvited": 5,
      "totalVoted": 5,
      "hasMyVote": true,
      "slotCount": 2,
      "isInvited": true,
      "createdAt": "2026-11-20T03:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 2,
  "totalPages": 1
}
```

Mỗi phần tử trong `items` luôn có đủ 15 trường (`id`, `groupId`, `title`, `description`, `status`, `externalLink`, `confirmedStartTime`, `confirmedEndTime`, `creator`, `totalInvited`, `totalVoted`, `hasMyVote`, `slotCount`, `isInvited`, `createdAt`); trường chưa có giá trị trả `null`. Trong đó `hasMyVote` cho biết người gọi đã có ít nhất một phiếu bầu cho cuộc họp này hay chưa.

### Lỗi và thứ tự kiểm tra

Thứ tự kiểm tra: `401` → lỗi tham số (`400 BAD_REQUEST`, `400 VALIDATION_ERROR`) → `404`.

| HTTP | `code` | Khi nào |
|---|---|---|
| 401 | `UNAUTHORIZED` | Thiếu token, token sai hoặc hết hạn |
| 400 | `BAD_REQUEST` | `status` không thuộc `VOTING`, `CONFIRMED`, `UPCOMING`, `PAST`, `ALL`; `page`, `size` hoặc `groupId` không phải số |
| 400 | `VALIDATION_ERROR` | `page < 0`, `size < 1` hoặc `size > 100`; `errors[].field` là `page` hoặc `size` |
| 404 | `NOT_FOUND` | Nhóm không tồn tại hoặc người gọi không thuộc nhóm `groupId` |

## 5. Chi tiết cuộc họp: `GET /api/groups/{groupId}/meetings/{meetingId}`

**Issue:** BE #31 · FE #23, #24, #25, #26 · **Use Case:** UC14, UC15 · **Màn hình:** 5.3, 5.4, 5.5

Không có query hay body.

### Thành công: `200 OK`

Trả về cấu trúc **Chi tiết cuộc họp** (mục 2).

### Lỗi và thứ tự kiểm tra

Thứ tự kiểm tra: `401` → lỗi tham số (`400 BAD_REQUEST`) → `404`.

| HTTP | `code` | Khi nào |
|---|---|---|
| 401 | `UNAUTHORIZED` | Thiếu token, token sai hoặc hết hạn |
| 400 | `BAD_REQUEST` | `groupId` hoặc `meetingId` không phải số |
| 404 | `NOT_FOUND` | Người gọi không thuộc nhóm `groupId`, hoặc `meetingId` không tồn tại / không thuộc nhóm `groupId` |

## 6. Bình chọn khung giờ họp: `POST /api/groups/{groupId}/meetings/{meetingId}/votes`

**Issue:** BE #28 · FE #23 · **Use Case:** UC14 · **Màn hình:** 5.3

Ghi nhận, cập nhật hoặc hủy bình chọn của người gọi. Người bình chọn luôn lấy từ JWT token. Khi cuộc họp đang `"VOTING"`, gửi lại request hợp lệ sẽ xóa toàn bộ lựa chọn cũ của người gọi và lưu lựa chọn mới trong cùng một giao dịch (có khóa dòng `SELECT ... FOR UPDATE` trên cuộc họp để chống tranh chấp). Gửi `selectedSlotIds: []` để hủy toàn bộ bình chọn của mình (khi đó người gọi được tính là chưa bình chọn ở API tổng hợp).

### Gửi lên

```json
{
  "selectedSlotIds": [101, 102]
}
```

| Trường | Bắt buộc | Quy tắc |
|---|---|---|
| `selectedSlotIds` | Có | Mảng `number[]`, không được `null`, không chứa phần tử `null`. Backend khử trùng lặp ID. Gửi `[]` để hủy toàn bộ bình chọn |

### Thành công: `200 OK`

```json
{
  "meetingId": 10,
  "selectedSlotIds": [101, 102]
}
```

`selectedSlotIds` trả về danh sách ID khung giờ đã lưu của người gọi, sắp xếp theo `startTime` tăng dần, `endTime` tăng dần, rồi `id` tăng dần.

### Lỗi và thứ tự kiểm tra

Thứ tự kiểm tra: `401` → lỗi body / tham số (`400 BAD_REQUEST`, `400 VALIDATION_ERROR`) → `404` → `403` → `409` → `400 INVALID_SLOT_SELECTION`.

| HTTP | `code` | Khi nào | Frontend hiển thị |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | Thiếu token, token sai hoặc hết hạn | Xóa token, chuyển về `/login` |
| 400 | `BAD_REQUEST` | Body không phải JSON hợp lệ, sai kiểu dữ liệu, hoặc `groupId`/`meetingId` không phải số | Hiện `detail` |
| 400 | `VALIDATION_ERROR` | Thiếu `selectedSlotIds`, `selectedSlotIds` là `null`, hoặc có phần tử `null`; `errors[].field` là `selectedSlotIds` hoặc `selectedSlotIds[i]` | Hiện `detail` / lỗi trường |
| 404 | `NOT_FOUND` | Người gọi không thuộc nhóm `groupId`, hoặc `meetingId` không tồn tại / không thuộc nhóm `groupId` | Hiện `detail` |
| 403 | `FORBIDDEN` | Thuộc nhóm `groupId` nhưng **không** nằm trong `meeting_participants` (không được mời) | Ẩn form bình chọn hoặc hiện thông báo không có quyền |
| 409 | `MEETING_ALREADY_CONFIRMED` | Cuộc họp đã chuyển sang trạng thái `CONFIRMED` | Hiện `detail` và tải lại chi tiết cuộc họp |
| 400 | `INVALID_SLOT_SELECTION` | Có ID trong `selectedSlotIds` không tồn tại hoặc không thuộc cuộc họp `meetingId` | Hiện `detail` |

Ví dụ lỗi `409 MEETING_ALREADY_CONFIRMED`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Cuộc họp đã chốt lịch, không thể thay đổi bình chọn",
  "instance": "/api/groups/3/meetings/10/votes",
  "code": "MEETING_ALREADY_CONFIRMED",
  "timestamp": "2026-11-26T09:16:00Z"
}
```

## 7. Tổng hợp bình chọn và gợi ý khung giờ: `GET /api/groups/{groupId}/meetings/{meetingId}/votes`

**Issue:** BE #29 · FE #23, #24, #25 · **Use Case:** UC14, UC15 · **Màn hình:** 5.3, 5.4

Trả về kết quả tổng hợp bình chọn và khung giờ được gợi ý cho cả cuộc họp `VOTING` lẫn `CONFIRMED`. Mọi thành viên hiện tại của nhóm (kể cả người không được mời) đều gọi được API này. Không có query hay body.

### Thành công: `200 OK`

Ví dụ response (minh họa trường hợp nhiều khung giờ hòa phiếu cao nhất như màn hình prototype 5.4):

```json
{
  "meetingId": 10,
  "status": "VOTING",
  "totalInvited": 4,
  "totalVoted": 4,
  "mySelectedSlotIds": [101, 102],
  "participants": [
    { "id": 4, "fullName": "Hoàng Văn Dũng", "hasVoted": true },
    { "id": 3, "fullName": "Lê Kim Chi", "hasVoted": true },
    { "id": 1, "fullName": "Nguyễn Văn An", "hasVoted": true },
    { "id": 2, "fullName": "Trần Thái Bình", "hasVoted": true }
  ],
  "timeSlots": [
    {
      "id": 101,
      "startTime": "2026-11-28T02:00:00Z",
      "endTime": "2026-11-28T03:00:00Z",
      "voteCount": 3,
      "voters": [
        { "id": 4, "fullName": "Hoàng Văn Dũng" },
        { "id": 3, "fullName": "Lê Kim Chi" },
        { "id": 1, "fullName": "Nguyễn Văn An" }
      ],
      "isSuggested": true
    },
    {
      "id": 102,
      "startTime": "2026-11-28T07:00:00Z",
      "endTime": "2026-11-28T08:00:00Z",
      "voteCount": 3,
      "voters": [
        { "id": 3, "fullName": "Lê Kim Chi" },
        { "id": 1, "fullName": "Nguyễn Văn An" },
        { "id": 2, "fullName": "Trần Thái Bình" }
      ],
      "isSuggested": true
    },
    {
      "id": 103,
      "startTime": "2026-11-29T03:00:00Z",
      "endTime": "2026-11-29T04:00:00Z",
      "voteCount": 2,
      "voters": [
        { "id": 4, "fullName": "Hoàng Văn Dũng" },
        { "id": 2, "fullName": "Trần Thái Bình" }
      ],
      "isSuggested": false
    }
  ]
}
```

| Trường | Kiểu | Mô tả |
|---|---|---|
| `meetingId` | `number` | ID cuộc họp |
| `status` | `"VOTING" \| "CONFIRMED"` | Trạng thái hiện tại của cuộc họp |
| `totalInvited` | `number` | Số người trong `meeting_participants` hiện còn là thành viên nhóm (tính cả `creator` nếu còn trong nhóm) |
| `totalVoted` | `number` | Số thành viên khác nhau hiện còn trong nhóm và có ít nhất một phiếu bầu ở cuộc họp này |
| `mySelectedSlotIds` | `number[]` | Các ID khung giờ người gọi đã chọn, sắp xếp theo `startTime` ↑, `endTime` ↑, `id` ↑; trả `[]` nếu chưa bình chọn hoặc không được mời |
| `participants` | `{ id: number, fullName: string, hasVoted: boolean }[]` | Danh sách thành viên mời còn trong nhóm để dựng ma trận bình chọn, sắp xếp theo `fullName` ↑, `id` ↑ |
| `timeSlots` | `object[]` | Danh sách khung giờ sắp xếp theo `startTime` ↑, `endTime` ↑, `id` ↑ |
| `timeSlots[].voteCount` | `number` | Số người hiện còn trong nhóm đã chọn khung giờ này |
| `timeSlots[].voters` | `{ id: number, fullName: string }[]` | Danh sách người đã chọn khung giờ này (còn trong nhóm), sắp xếp theo `fullName` ↑, `id` ↑ |
| `timeSlots[].isSuggested` | `boolean` | Cờ gợi ý khung giờ phù hợp nhất |

### Cách hiển thị ma trận bình chọn trên màn hình 5.4

Frontend kết hợp `participants[].hasVoted` và `timeSlots[].voters` để xác định trạng thái của từng thành viên tại mỗi khung giờ, tránh hiển thị nhầm người chưa bình chọn thành `"Bận"`:

- `participant.hasVoted === false`: hiển thị **"Chưa bình chọn"**.
- `participant.hasVoted === true` và `participant.id` có trong `slot.voters`: hiển thị **"Có thể"**.
- `participant.hasVoted === true` và `participant.id` không có trong `slot.voters`: hiển thị **"Bận"**.

### Quy tắc gán `isSuggested` (QĐ07)

- Nếu mọi khung giờ đều có `voteCount == 0` → tất cả trả `isSuggested: false`.
- Nếu có ít nhất một phiếu (`maxVoteCount > 0`) → **mọi** khung giờ có `voteCount == maxVoteCount` đều trả `isSuggested: true` (kể cả khi có từ hai khung giờ trở lên hòa phiếu cao nhất); các khung giờ còn lại trả `false`.
- Kết quả gợi ý chỉ mang tính đề xuất; hệ thống **không** tự động chuyển cuộc họp sang `"CONFIRMED"`.

### Lỗi và thứ tự kiểm tra

Thứ tự kiểm tra: `401` → lỗi tham số (`400 BAD_REQUEST`) → `404`.

| HTTP | `code` | Khi nào |
|---|---|---|
| 401 | `UNAUTHORIZED` | Thiếu token, token sai hoặc hết hạn |
| 400 | `BAD_REQUEST` | `groupId` hoặc `meetingId` không phải số |
| 404 | `NOT_FOUND` | Người gọi không thuộc nhóm `groupId`, hoặc `meetingId` không tồn tại / không thuộc nhóm `groupId` |

## 8. Chốt lịch họp chính thức: `POST /api/groups/{groupId}/meetings/{meetingId}/confirm`

**Issue:** BE #30 · FE #25 · **Use Case:** UC15 · **Màn hình:** 5.4, 5.5

Chỉ `creator` của cuộc họp hoặc `LEADER` của nhóm (kiểm tra từ JWT và CSDL) mới được chốt lịch. Theo QĐ07, người có quyền được chốt **bất kỳ** khung giờ hợp lệ nào của cuộc họp (kể cả khung không có `isSuggested: true` hoặc khi chưa có ai bình chọn), miễn là khung giờ chưa kết thúc. Thao tác dùng khóa dòng `SELECT ... FOR UPDATE` để đảm bảo an toàn khi nhiều request chốt hoặc bình chọn chạy đồng thời.

### Gửi lên

```json
{
  "confirmedSlotId": 102,
  "externalLink": "https://meet.google.com/cnpm-progress-sync"
}
```

| Trường | Bắt buộc | Quy tắc |
|---|---|---|
| `confirmedSlotId` | Có | ID của một khung giờ thuộc cuộc họp `meetingId`, có `endTime > hiện tại` |
| `externalLink` | Không | - Không truyền hoặc gửi `null`: **giữ nguyên** liên kết đã lưu trước đó.<br>- Gửi chuỗi rỗng `""` hoặc chỉ khoảng trắng: **xóa** liên kết (lưu `null`).<br>- Gửi chuỗi có nội dung: backend `trim` hai đầu, tối đa 500 ký tự, phải là URL `http://` hoặc `https://` có host hợp lệ, và **ghi đè** liên kết cũ |

### Thành công: `200 OK`

Trả về cấu trúc **Chi tiết cuộc họp** (mục 2) với `status: "CONFIRMED"`, `confirmedSlotId`, `confirmedStartTime`, `confirmedEndTime`, `externalLink` và `updatedAt` đã cập nhật.

**Thông báo (phụ thuộc mềm #26):** Sau khi giao dịch commit, backend gửi thông báo trong ứng dụng loại `MEETING_CONFIRMED` tới các thành viên trong `participants` còn thuộc nhóm `groupId` (trừ người thực hiện chốt lịch). Lỗi khi gửi thông báo không làm hỏng việc chốt lịch.

### Lỗi và thứ tự kiểm tra

Thứ tự kiểm tra: `401` → lỗi body / tham số (`400 BAD_REQUEST`, `400 VALIDATION_ERROR`) → `404` → `403` → `409` → `400 INVALID_SLOT_SELECTION` → `400 INVALID_TIME_SLOT`.

| HTTP | `code` | Khi nào | Frontend hiển thị |
|---|---|---|---|
| 401 | `UNAUTHORIZED` | Thiếu token, token sai hoặc hết hạn | Xóa token, chuyển về `/login` |
| 400 | `BAD_REQUEST` | Body không phải JSON hợp lệ, sai kiểu dữ liệu, hoặc `groupId`/`meetingId` không phải số | Hiện `detail` |
| 400 | `VALIDATION_ERROR` | Thiếu `confirmedSlotId`, hoặc `externalLink` sai định dạng URL / quá 500 ký tự; `errors[].field` là `confirmedSlotId` hoặc `externalLink` | Hiện lỗi dưới ô tương ứng |
| 404 | `NOT_FOUND` | Người gọi không thuộc nhóm `groupId`, hoặc `meetingId` không tồn tại / không thuộc nhóm `groupId` | Hiện `detail` |
| 403 | `FORBIDDEN` | Không phải `creator` của cuộc họp và không phải `LEADER` của nhóm | Ẩn khối chốt lịch hoặc báo không có quyền |
| 409 | `MEETING_ALREADY_CONFIRMED` | Cuộc họp đã ở trạng thái `CONFIRMED` | Hiện `detail` và tải lại chi tiết cuộc họp |
| 400 | `INVALID_SLOT_SELECTION` | `confirmedSlotId` không tồn tại hoặc không thuộc cuộc họp `meetingId` | Hiện `detail` |
| 400 | `INVALID_TIME_SLOT` | Khung giờ được chọn đã kết thúc (`endTime <= hiện tại`) | Hiện `detail` và yêu cầu chọn khung khác |

## 9. Luồng sử dụng phía frontend và phạm vi Milestone 1

### Luồng màn hình

1. **Danh sách cuộc họp (màn hình 5.1 — `/groups/[groupId]/meetings`):**
   - Gọi `GET /api/groups/{groupId}/meetings?status=...&page=...&size=20`.
   - Ánh xạ 3 tab lọc trên màn hình 5.1 khớp BE #31: `Tất cả` (`?status=ALL`), `Đã chốt` (`?status=CONFIRMED`), `Đang bình chọn` (`?status=VOTING`). Khi gọi API không truyền `status`, backend trả mặc định `VOTING` và `UPCOMING`.
   - Dựa vào `status`, `isInvited` và `hasMyVote` để hiển thị nút `"Bình chọn ngay"` / `"Chi tiết"` và nhãn `"x/y thành viên đã biểu quyết"`.
2. **Tạo cuộc họp mới (màn hình 5.2 — `/groups/[groupId]/meetings/new`):**
   - Tải danh sách thành viên nhóm (từ API thành viên nhóm) để chọn `invitedUserIds`.
   - Với mỗi khung giờ, người dùng chọn 1 ngày và giờ bắt đầu / kết thúc trong ngày đó theo giờ Việt Nam (`+07:00`); client ghép thành chuỗi ISO 8601 gửi qua `POST /api/groups/{groupId}/meetings`. Thành công (`201`) chuyển tới `/groups/[groupId]/meetings/[meetingId]`.
3. **Chi tiết, bình chọn, kết quả và chốt lịch (màn hình 5.3, 5.4, 5.5 — `/groups/[groupId]/meetings/[meetingId]`):**
   - Gọi song song `GET /api/groups/{groupId}/meetings/{meetingId}` và `GET /api/groups/{groupId}/meetings/{meetingId}/votes`.
   - Nếu `status === "VOTING"`:
     - Thành viên có `isInvited === true` thấy phần bình chọn (5.3), khởi tạo checkbox từ `mySelectedSlotIds`, gửi qua `POST .../votes` (khi lưu gọi lại `GET .../votes` để cập nhật tổng hợp).
     - Hiển thị bảng/ma trận kết quả và thẻ đề xuất theo `timeSlots[].isSuggested` kèm ghi chú đây chỉ là đề xuất (5.4).
     - Nếu người dùng là `creator` hoặc `LEADER` của nhóm: hiển thị khối chọn 1 khung giờ chính thức + ô nhập/chỉnh `externalLink` + nút `"Chốt lịch họp"`, gửi `POST .../confirm`, thành công cập nhật giao diện sang 5.5.
   - Nếu `status === "CONFIRMED"`: hiển thị thông tin lịch chính thức (`confirmedStartTime`, `confirmedEndTime`), danh sách thành viên tham gia và liên kết họp bên ngoài (mở tab mới `rel="noopener noreferrer"` hoặc sao chép liên kết) như màn hình 5.5.

### Ngoài phạm vi Milestone 1 (không có trong hợp đồng này)

- Ô tìm kiếm cuộc họp và 3 thẻ thống kê tổng hợp trên đầu trang ở prototype 5.1.
- Hạn chót bình chọn ở prototype 5.3.
- Khối `"Tài liệu chuẩn bị"` và các nút `"Chia sẻ"`, `"Lưu lịch"` ở prototype 5.5.
- Hủy cuộc họp, cảnh báo trùng lịch (#40) và gửi email thông báo (#34) — thuộc Milestone 3 (`[N]`).
