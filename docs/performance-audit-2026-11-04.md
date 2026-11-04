# Rà soát hiệu năng service và query — 2026-11-04

- **Nhánh:** `feature/phase-1-rescue-claims`, trên commit `1368426`. Các thay đổi trong tài liệu này **chưa commit**.
- **Phạm vi:** domain service, use case, query PostgreSQL và MongoDB, index, cấu hình kết nối và log.
- **Kết quả test:** 233/233 test xanh sau khi sửa (trước đó 198).

## Cách kiểm tra

| Nhãn | Ý nghĩa |
|---|---|
| **Đo** | `EXPLAIN ANALYZE` trên PostGIS 16 local. Dữ liệu từ `seed-postgres.sql` với `demo.scale = 100`: 8.000 ca cứu hộ, 6.000 thú cưng, 2.800 đơn nhận nuôi, 800 tổ chức. So sánh `app_old` (V26, SQL tại `HEAD`) với `app_new` (V27, SQL đã sửa). Lấy trung vị của 3 lần chạy |
| **Test** | Integration test trên PostGIS và MongoDB thật, đếm câu SQL bằng Hibernate Statistics |
| **Chạy thử** | App chạy thật với profile `free`, gọi API qua HTTP. Dùng PostgreSQL, Redis, RabbitMQ, MongoDB tạm trong Docker và thông tin Cloudinary giả |

Thời gian đo là thời gian query trong database, chưa gồm độ trễ mạng tới Neon. Chưa thử trên staging hoặc Cloudinary thật.

## Kết quả đo

| Query | Trước | Sau | Thay đổi chính |
|---|---|---|---|
| Dashboard overview | Quá 60 giây, bị dừng | 3,4 ms | Bỏ tích Descartes 8 bảng |
| Hàng đợi tổ chức | 288 ms | 9,6 ms | Index `rescue_case_completions(case_id)` |
| Ca gần đây, bán kính 3 km | 62,9 ms, quét toàn bảng | 9,9 ms, dùng index | GiST trên `location::geography` |
| Marker tổ chức trong vùng | 8,9 ms, quét toàn bảng | 0,6 ms, dùng GiST | `ST_MakeEnvelope` thay cho `ST_Y/ST_X BETWEEN` |
| Tìm thú theo tên | 6,4 ms | 1,5 ms | Bỏ JOIN thừa, thêm `pg_trgm` |
| Top tổ chức | 1,7 ms | 4,8 ms | Chậm hơn vì giờ xét mọi tổ chức, không chỉ tổ chức tạo trong tháng |

Số câu SQL mỗi request (**Test**, chưa tính JWT filter):

| Request | Trước | Sau |
|---|---|---|
| Chi tiết ca cứu hộ, người dùng đã đăng nhập | 7 | Tối đa 3 |
| Danh sách hoàn tất, 5 dòng | 7 | Tối đa 3 |
| Chi tiết thú | 3 | Tối đa 2 |
| Danh sách bạn bè theo cursor | Có thể thêm `COUNT` | 1 |

JWT filter trước đây tốn 2 query mỗi request vì tra UUID như email trước; giờ tra thẳng theo id nên còn 1 query.

## Lỗi đã sửa

### Mức nghiêm trọng

| Vấn đề | Tác động | Cách sửa |
|---|---|---|
| Dashboard JOIN 8 bảng không có khóa nối | Số dòng là tích số bản ghi của cả 8 tập, query không kết thúc được khi hai tháng liên tiếp đều có dữ liệu | Tổng hợp từng bảng riêng bằng `COUNT(*) FILTER` |
| Dashboard trả 500 từ lần mở thứ 2 (phát hiện khi chạy thử) | Cache Redis đọc lại thành `LinkedHashMap`, ép kiểu về DTO thất bại | Dùng JDK serialization, thêm prefix key `v2:` |
| Gọi Cloudinary trong transaction | Giữ khóa dòng và connection suốt các HTTP call, trong khi profile free chỉ có 5 connection | Chuyển ảnh sau commit bằng `MediaPromotionRequestedEvent`; rollback thì không chuyển |
| Chi tiết ca không có tọa độ | `CAST(NULL AS double)` khiến `latitude`, `longitude` luôn null; địa chỉ cũng bị bỏ | Lấy `ST_Y/ST_X`, trả thêm `locationText`, `wardName`, `provinceName` |
| Ảnh tạm của ca cứu hộ không được chuyển sang thư mục chính thức (phát hiện khi test) | `MediaQueryUseCase` không trả `status`, điều kiện `TEMP` luôn sai | Trả `status` |
| Reputation mất cập nhật khi đồng thời | Test đo được 21 lần cộng chỉ ghi được 7 | `INSERT ... ON CONFLICT DO UPDATE` cộng trong database |

### Hiệu năng

- `ProvinceQueryAdapter` parse file JSON 637 KB trên mỗi request: giờ chỉ parse một lần và không mở transaction.
- N+1 ảnh ở danh sách hoàn tất và hàng đợi review: tải ảnh theo lô bằng `IN`.
- `isAdmin` tải cả user và role: thay bằng một query kiểm tra tồn tại.
- Chi tiết ca đọc lại entity và nhiều query rời: gộp vào một projection.
- `pageSize` không giới hạn: `PageSizeLimitFilter` hạ giá trị lớn xuống 100 cho mọi endpoint.
- Query không có timeout: dừng query JPA và `JdbcTemplate` sau 20 giây.
- Log SQL và DEBUG bật mặc định cho `free`, `scale`: chuyển sang profile `dev`.
- Gửi tin nhắn: dùng `$inc` cho `unread`, bỏ đọc rồi cộng.
- Push chat chạy trên common pool: chuyển sang `@Async` (`ChatPushSender`). Client Expo dùng chung và có timeout. Token notification tra theo lô.
- Đổi hồ sơ người dùng: snapshot Mongo chạy `@Async`, thêm index `post_preview_author`.
- Feed: không tải `analysis`; index `post_tags_feed` thay cho `post_tags`.
- `V27__query_indexes.sql`: bỏ 3 GiST trùng, thêm index geography, `organization_id`, hàng đợi tổ chức, `completion.case_id` và `pg_trgm` cho tên thú, người dùng.

### Đúng use case và đủ dữ liệu

| Màn hình/API | Trước | Sau |
|---|---|---|
| Danh sách ca | `firstImageUrl` là `public_id` thô | URL thật, thêm `locationText` |
| Ca gần đây / theo vùng | Không có ảnh, không trả khoảng cách | Có ảnh; ca gần đây có `distanceMeters` |
| Danh sách hoàn tất | Thiếu `caseCode`, gồm cả bản ghi đã xóa | Có `caseCode`, lọc `is_deleted`, sort theo tham số |
| Marker bản đồ ca | Gồm cả ca `CLOSED`, có thể chiếm hết giới hạn 500 | Mặc định `REPORTED`, `IN_PROGRESS`; lọc status vẫn xem được ca khác |
| Bản đồ tổ chức | API công khai trả cả tổ chức `PENDING` | Mặc định `ACTIVE` |
| Danh sách tổ chức | Đọc phone, email, ảnh rồi bỏ (lỗi M5 trong audit FE) | Trả đủ các trường |
| Chi tiết đơn nhận nuôi | JOIN 4 bảng rồi bỏ người nộp, ghi chú, lý do từ chối, `readyAt` | Trả đủ |
| Ảnh đơn nhận nuôi | `MIN(publicId)` chọn theo chữ cái | Ảnh chính |
| Hoàn tất nhận nuôi | Đơn khác của thú vẫn mở, đơn `PENDING` chiếm hạn mức | Tự hủy đơn `PENDING`, `APPROVED` còn lại |
| Danh sách và chi tiết thú | Công khai số điện thoại chủ cá nhân; mã tỉnh rỗng gây lỗi cả query | Chỉ hiện số của tổ chức; bỏ `CAST` không dùng |
| Chi tiết bài đăng | Thiếu số like, số comment, avatar | Trả đủ |
| Feed | Không biết người xem đã thích bài chưa | Có `likedByMe`, null với khách |

## Thay đổi FE cần biết

- `pageSize` tối đa 100.
- Marker ca và marker tổ chức đổi bộ lọc mặc định, xem bảng trên.
- `owner.phone` của thú do cá nhân sở hữu không còn được trả trên API công khai.
- Dashboard đổi cách tính:
  - Tổng và việc đang mở tính đến cuối kỳ.
  - `openRescueCases` gồm `REPORTED` và `IN_PROGRESS`.
  - `completedAdoptions` là đơn hoàn tất trong kỳ, dựa vào `updated_at`.
  - Không có lịch sử trạng thái, nên xu hướng của số liệu theo trạng thái chỉ là gần đúng.
  - Việc chờ duyệt gồm cả việc từ tháng trước, sắp xếp cũ nhất trước.
- Ảnh tạm được chuyển sau khi API trả về, nên `public_id` có thể đổi ngay sau đó. FE không nên lưu URL ảnh từ response tạo mới.

## Chưa làm

| Việc | Ghi chú |
|---|---|
| Làm mờ vị trí khoảng 300 m | `agent/05-workflows-screens.md:185` yêu cầu, nhưng marker và chi tiết ca đang trả tọa độ chính xác cho cả khách. **Cần quyết định trước production** |
| Thử lại khi chuyển ảnh lỗi | Ảnh vẫn ở thư mục tạm và vẫn hiển thị. Hiện chỉ ghi log |
| Tìm kiếm còn quét toàn bảng | Ca theo tên người báo, tổ chức, regex tìm bài đăng. Index `post_text` chưa được query nào dùng |
| Thông báo khi đơn bị hủy tự động | Lý do được lưu ở `rejectReason`, nhưng chưa gửi notification |
| N+1 nhỏ | `findClaimsOfCase`, `findMyActiveClaims`; số dòng thường ít |
| Xóa ảnh gọi Cloudinary trong transaction | `MediaFileDomainService.delete`; chỉ 1 call và đã bắt lỗi |
| Hikari keepalive trên Neon | Comment trong cấu hình nói nó ngăn Neon tự tắt, có thể tốn compute. Cần xem lại với gói đang dùng |

## Giới hạn kiểm chứng

- **Cloudinary:** đường chuyển ảnh thành công chỉ test bằng mock. Chạy thử với thông tin giả xác nhận rằng chuyển lỗi không làm hỏng báo cáo.
- **Chưa có test riêng:** `@Async` của snapshot sync, `ChatPushSender`, lô token notification, timeout Expo, luồng `PetDetailsCommandUseCase.addMedia`.
- **Viết test sau khi sửa code:** tỉnh thành, JWT, feed, chat, query timeout, cache Redis. Các test này không được thấy fail đúng lý do trước khi sửa.
- **Timeout 20 giây:** test xác nhận cơ chế với timeout 4 giây; giá trị 20 giây trong `application.properties` chưa được đo riêng.
- **Migration:** `V27` chạy trên PostGIS local và trong integration test, chưa chạy trên Neon. Migration cần quyền `CREATE EXTENSION pg_trgm`.

## Dữ liệu mẫu cho staging

Xem `scripts/demo-data/README.md`: thứ tự chạy, tài khoản, cách tăng khối lượng và cách xóa.

Đã kiểm tra:

- Seed chạy lại nhiều lần không tạo thêm dữ liệu.
- Cleanup PostgreSQL xóa sạch, kể cả sau khi đã thao tác trên app.
- Cleanup Mongo xóa bài mẫu cùng comment và lượt thích của chúng, không xóa hội thoại.

## Tái hiện phép đo

1. Tạo hai database PostGIS: một chạy migration đến V26, một đến V27.
2. Đổi `demo.scale` thành `100` trong một bản sao của `seed-postgres.sql`, nạp vào cả hai rồi chạy `VACUUM ANALYZE`.
3. Chạy `EXPLAIN (ANALYZE, TIMING OFF)` với SQL cũ (`git show 1368426:<file repository>`) trên database V26 và SQL mới trên database V27. Thay tham số bằng giá trị cụ thể, đặt `statement_timeout` để query cũ không chạy mãi.
