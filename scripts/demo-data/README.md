# Dữ liệu mẫu cho staging và demo

**Chỉ dùng cho staging hoặc máy local, không chạy trên production.** Tất cả tài khoản dùng chung một mật khẩu công khai trong file này.

Các script không phải migration của Flyway, nên chỉ chạy khi có người gọi.

Dữ liệu được dựng quanh **ba tài khoản chính**. Mỗi tài khoản đi được trọn các kịch bản của vai trò đó mà không phải đổi tài khoản:

| Tài khoản | Vai trò | Dùng để demo |
|---|---|---|
| `demo.user1@petrescue.demo` (Nguyễn Minh Anh) | Người dùng | Báo ca, đi cứu, thú của tôi, báo lạc và gợi ý ghép, nhận nuôi kèm điểm phù hợp, hỏi thăm sau nhận nuôi, bài đăng có AI đọc, chat, bạn bè, thông báo |
| `demo.owner1@petrescue.demo` (Trần Thị Hồng Nhung) | Chủ Trạm cứu hộ Mái Ấm Sài Gòn | Duyệt đơn theo điểm phù hợp, chờ đón, việc sau nhận nuôi, thu hồi, hàng chờ cứu hộ, ca trạm đang nhận, đặc điểm thú |
| `demo.admin@petrescue.demo` (Lê Quang Hải) | Quản trị | Dashboard, duyệt trung tâm, khiếu nại báo cáo hoàn tất, ca bị gắn cờ, AI phân loại, banner, người dùng |

Mật khẩu của mọi tài khoản: `Demo@12345`. Đăng nhập bằng email hoặc username (`demo.user1`).

## Thứ tự chạy

Script chạy từ máy của bạn và kết nối thẳng tới database, nên staging trên Render free (không có shell) vẫn dùng được.

1. Deploy API lên staging và chờ khởi động xong, để Flyway và `MongoSchemaInitializer` tạo schema. Script PostgreSQL cần migration `V37` trở lên.
2. Lấy connection string của PostgreSQL và MongoDB. Với Neon, nên dùng URL trực tiếp (không có `-pooler`) dạng `postgresql://user:pass@host/db?sslmode=require`.
3. Nếu database có giới hạn IP (ví dụ Network Access của Atlas), cho phép IP máy của bạn.
4. Chạy trong PowerShell tại thư mục gốc của repo. Chỉ cần Docker, không phải cài `psql` hay `mongosh`:

```powershell
$env:STAGING_DB_URL = '<connection string PostgreSQL>'
$env:STAGING_MONGO_URI = '<connection string MongoDB>'

docker run --rm -v "${PWD}/scripts/demo-data:/demo" postgres:16-alpine psql $env:STAGING_DB_URL -q -v ON_ERROR_STOP=1 -f /demo/seed-postgres.sql
docker run --rm -v "${PWD}/scripts/demo-data:/demo" mongo:7 mongosh $env:STAGING_MONGO_URI --quiet /demo/seed-mongo.js
```

Nếu máy đã có sẵn `psql` và `mongosh`:

```bash
psql "$STAGING_DB_URL" -v ON_ERROR_STOP=1 -f scripts/demo-data/seed-postgres.sql
mongosh "$STAGING_MONGO_URI" scripts/demo-data/seed-mongo.js
```

5. **Điểm phù hợp của đơn nhận nuôi do chính API tính**, không ghi sẵn trong script. Job `MatchBackfillWorker` chấm mọi đơn còn mở chưa có điểm, khi người nộp đã có hồ sơ người nhận nuôi và bé đã có đặc điểm. Job chạy khoảng 20 giây sau khi API khởi động, rồi cứ 5 phút một lần. Seed xong thì chờ tối đa 5 phút, hoặc khởi động lại API. Có thể chỉnh bằng `app.matching.backfill-ms` hoặc tắt bằng `app.matching.backfill-enabled=false`.

Chạy lại nhiều lần vẫn an toàn: bản ghi đã có được giữ nguyên. Vì vậy, nếu database đang có dữ liệu mẫu của phiên bản script cũ (mã `DM...`, tên "Trạm cứu hộ Demo 1"), hãy chạy phần [Xóa dữ liệu mẫu](#xóa-dữ-liệu-mẫu) trước rồi mới seed lại. Nếu không, dữ liệu cũ và mới sẽ nằm lẫn với nhau. Một kịch bản đã đi qua (ví dụ đã duyệt đơn) muốn làm lại thì cũng xóa rồi seed lại.

Script Mongo ghi vào database mà API đọc (`MONGO_DATABASE`, mặc định `petrescue`), không phải database trong connection string. Connection string của Atlas thường không có tên database, nên nếu dựa vào nó thì `mongosh` sẽ ghi nhầm vào `test`. Nếu server đặt `MONGO_DATABASE` khác `petrescue`, thêm `--eval "var DB_NAME = 'tên'"` trước tên file. Script dừng và báo lỗi nếu database đó chưa có collection `posts`.

## Kịch bản demo

Đường dẫn dạng `/thu-cua-toi` là trang của FE. Những chức năng FE chưa có màn hình thì ghi endpoint API, thử qua Swagger tại `/swagger-ui.html`. Các tiền tố `/api/v1` được lược bớt.

### 1. `demo.user1`: người dùng

user1 ở TP.HCM, nhà phố có sân, có con lớn và một bé mèo, làm việc ở nhà, đã nuôi thú nhiều năm.

| # | Kịch bản | Vào đâu | Có sẵn gì |
|---|---|---|---|
| 1.1 | Xác nhận kết quả ca mình báo | `/ca-cua-toi`, ca **R-D0030** | Ca mèo user1 báo, Lý Hoàng Nam (`user12`) đang nhận, đã đăng 2 cập nhật (một có ảnh) và gửi báo cáo hoàn tất "giao cho trạm" chờ user1 bấm **Xác nhận** hoặc **Khiếu nại** (`POST /rescue-cases/completions/{id}/confirm`, `/dispute`) |
| 1.2 | Đi cứu một ca | `/ban-do`, `/ca-cua-toi`, ca **R-D0049** | user1 đang nhận ca, đã đăng một cập nhật. Có thể đăng thêm cập nhật, gửi báo cáo hoàn tất hoặc trả ca. Ca mới để nhận thử: các ca `REPORTED` trên bản đồ |
| 1.3 | Thú của tôi | `/thu-cua-toi` | 3 bé: **Lu** (nhận nuôi từ Mái Ấm Sài Gòn 16 ngày trước), **Gấu** (chó user1 cứu ở ca R-D0079 rồi giữ nuôi, công khai), **Mực** (mèo nhà, riêng tư, đang `LOST`) |
| 1.4 | Báo lạc và gợi ý ghép | ca **R-D9002** (`LOST`). API: `GET /rescue-cases/{id}/matches` | Mực lạc gần chợ Bà Chiểu. Hệ thống gợi ý ca **R-D9003** (`FOUND`, Bùi Tuấn Kiệt đang giữ ở nhà, cách 950 m, cùng vòng cổ đỏ), điểm 82, chờ user1 **xác nhận** hoặc **không phải** (`POST .../matches/{suggestionId}/confirm`, `/reject`) |
| 1.5 | Hồ sơ người nhận nuôi | API: `GET`, `PUT /users/me/adopter-profile` | Hồ sơ đã điền đủ: nhà, nội quy tòa nhà, trẻ em, dị ứng, thú đang nuôi, thời gian vắng nhà, ngân sách, kinh nghiệm |
| 1.6 | Xem bé hợp với mình tới đâu, nộp đơn | `/nhan-nuoi/{petId}`. API: `GET /pets/{petId}/match` | Đơn **A-D9001** xin bé **Gạo** đang chờ duyệt, đã có điểm phù hợp. Có thể nộp thêm đơn cho bé khác của trạm 1, ví dụ bé ở `/nhan-nuoi?organizationId=...` |
| 1.7 | Trả lời hỏi thăm sau nhận nuôi | `/hoi-tham`. API: `GET /adoptions/{id}/followups`, `POST /followups/{id}/answers` | Bé Lu: mốc ngày 2 đã trả lời (ổn, hơi sợ tiếng xe), **mốc tuần 2 đang chờ trả lời**, mốc 3 và 6 tháng đã lên lịch |
| 1.8 | Bài đăng có AI đọc | `/cong-dong`. API: `GET /posts/{id}/analysis`, `PATCH .../confirm`, `POST .../reject` | 2 bài của user1. Bài tìm Mực viết không dấu, viết tắt ("q.binh thanh", "ib"): AI đọc ra *Thất lạc, Mèo, gần chợ Bà Chiểu, Phường Bình Thạnh, TP.HCM*, **chờ user1 xác nhận**. Bài về Gấu đã xác nhận |
| 1.9 | Chat | API: `GET /chats/conversations`, `.../{id}/messages` | 3 cuộc trò chuyện, mỗi cuộc 1 tin chưa đọc: với bạn Trần Gia Bảo, với trạm về đơn A-D9001 (hẹn thứ Bảy gặp bé), với người cứu ca R-D0030 |
| 1.10 | Gắn cờ ca | API: `POST /rescue-cases/{id}/flags` | user1 đã gắn cờ ca R-D0001 "đã có người xử lý". Gắn thử cờ trên ca khác |
| 1.11 | Thông báo, bạn bè, uy tín | chuông thông báo, `/trang-ca-nhan` | 4 thông báo mới (báo cáo hoàn tất, gợi ý ghép, hỏi thăm, cập nhật ca) và thông báo ca được nhận. Là bạn với `user2`. Điểm uy tín có nhật ký +50 (cứu xong ca) và +30 (nhận nuôi xong) |

### 2. `demo.owner1`: chủ Trạm cứu hộ Mái Ấm Sài Gòn

Trạm 1 ở Phường Bình Thạnh, TP.HCM. Thú của trạm: các bé chó số 1, 7, 13, 19, 31, 37, 43, 49. Nhân viên: `demo.staff1`.

| # | Kịch bản | Vào đâu | Có sẵn gì |
|---|---|---|---|
| 2.1 | Duyệt đơn theo điểm phù hợp | `/quan-ly-nhan-nuoi`, tab chờ duyệt | 5 đơn, mỗi đơn ở một trường hợp: **A-D9001** user1 xin Gạo (hợp), **A-D9002** cũng xin Gạo (hai đơn cho cùng một bé, duyệt một thì đơn kia tự hủy), **A-D9003** người dị ứng chó (cảnh báo `ALLERGY`), **A-D9004** căn hộ chỉ cho nuôi thú nhỏ xin chó lớn (cảnh báo `BUILDING_RULE`), **A-D9006** người chưa điền hồ sơ (chưa có điểm). Đơn có cảnh báo xếp cuối danh sách |
| 2.2 | Xác nhận đón bé | `/quan-ly-nhan-nuoi`, tab đã duyệt | Đơn **A-D9005** đã duyệt, hẹn đón 3 ngày nữa. Bấm **Xác nhận đã đón** để hoàn tất và mở lịch hỏi thăm |
| 2.3 | Việc sau nhận nuôi | `/viec-sau-nhan-nuoi`. API: `GET /organizations/me/followup-alerts`, `PATCH /followup-alerts/{id}` | 2 việc đang mở của gia đình nhận bé số 49: **đỏ** (tuần 2: gầm gừ với con nhỏ, đang cân nhắc trả bé, chưa ai nhận) và **vàng** (ngày 2: bé tru đêm, `staff1` đang xử lý). Một việc đỏ đã xong (thu hồi) |
| 2.4 | Thu hồi bé | `/quan-ly-nhan-nuoi`, nhật ký thu hồi. API: `GET /adoptions/reclaims`, `POST /adoptions/{id}/reclaim` | Bé số 7 đã thu hồi 10 ngày trước vì bị nhốt ngoài ban công, kèm ảnh bằng chứng, người nhận nuôi bị trừ 300 điểm uy tín. Thử thu hồi trên đơn đã hoàn tất A-D9008 |
| 2.5 | Hàng chờ cứu hộ | `/hang-cho-cuu-ho` | Các ca chưa ai nhận gần trạm, sắp theo mức khẩn cấp. Nhận một ca cho trạm |
| 2.6 | Ca trạm đang nhận | `/quan-ly-cuu-tro` | Ca **R-D9001** (chó kẹt chân vào hàng rào, Phường Bình Thạnh), `staff1` đang đi. Cùng các ca khác của trạm trên bản đồ |
| 2.7 | Đặc điểm thú | `/quan-ly-thu-cung/{petId}`. API: `GET`, `PUT /pets/{petId}/traits` | Mọi bé của trạm đã có đặc điểm (thân người, trẻ em, sợ người lạ, năng lượng, ở nhà một mình, chi phí...). Sửa đặc điểm bé Gạo rồi xem điểm phù hợp của đơn thay đổi ở đơn mới nộp |
| 2.8 | Thú đang điều trị, hồ sơ y tế | `/quan-ly-thu-cung` | Lọc theo sức khỏe: có bé `UNDER_TREATMENT`, `SPECIAL_NEEDS`, `RECOVERING`, mỗi bé có lịch tiêm và điều trị |
| 2.9 | Trang trung tâm, thành viên | `/quan-ly-trung-tam`, `/organization/members` | Ảnh trung tâm, các con số công khai và việc đang chờ của trạm. Thành viên: owner1, staff1 |
| 2.10 | Dashboard trạm | `/dashboard` | Số liệu 6 tháng, việc đang chờ dẫn thẳng tới từng mục |

### 3. `demo.admin`: quản trị

| # | Kịch bản | Vào đâu | Có sẵn gì |
|---|---|---|---|
| 3.1 | Dashboard toàn hệ thống | `/dashboard` | Số liệu đủ 6 tháng: ca, đơn, thú, trung tâm |
| 3.2 | Duyệt trung tâm | `/quan-ly-trung-tam` | Trung tâm 4 và 8 đang `PENDING`, chờ duyệt hoặc từ chối |
| 3.3 | Khiếu nại báo cáo hoàn tất | `/quan-ly-cuu-tro`. API: `GET /rescue-cases/completions/review-queue` | Ca **R-D0008**: người báo khiếu nại "quay lại vẫn thấy bé nằm đó". Admin chấp nhận hoặc bác báo cáo. Cùng 4 báo cáo khác đang chờ |
| 3.4 | Ca bị gắn cờ | API: `GET /rescue-cases/flagged` | Ca **R-D0002** bị 3 người gắn cờ (2 báo cáo sai, 1 trùng), ca **R-D0001** bị gắn cờ "đã xử lý" |
| 3.5 | AI phân loại ca | API: `GET /rescue-cases/{id}/triage` | 48 ca đang mở đều có kết quả AI: mức khẩn cấp, cần cứu hay không, tóm tắt tình trạng, thông tin còn thiếu. Vài ca AI đánh giá khẩn hơn người báo |
| 3.6 | Cấu hình và chỉ số AI | `/cau-hinh-ai`. API: `GET /admin/ai/post-analysis/metrics` | Đổi model đọc bài đăng, xem tỉ lệ người dùng xác nhận gợi ý |
| 3.7 | Banner | `/quan-ly-banner` | 3 banner đang chạy cho trang chủ, nhận nuôi, cứu hộ |
| 3.8 | Chỉ số matching và hỏi thăm | API: `GET /matching/metrics`, `GET /admin/followups/metrics` | Phân bố mức phù hợp, tỉ lệ trả lời hỏi thăm, kết quả sau nhận nuôi (1 thu hồi) |
| 3.9 | Bài đăng, người dùng, vai trò, thẻ | `/quan-ly-bai-dang`, `/quan-ly-nguoi-dung`, `/quan-ly-vai-tro`, `/quan-ly-the` | 26 bài đăng cộng đồng, 47 tài khoản |

### Tài khoản khác

| Vai trò | Email | Ghi chú |
|---|---|---|
| Chủ tổ chức | `demo.owner2` đến `demo.owner8` | Mỗi người một trung tâm. Trung tâm 5 là phòng khám thú y. Trung tâm 4 và 8 đang `PENDING` |
| Nhân viên | `demo.staff1` đến `demo.staff8` | `staffN` làm ở trung tâm N. `staff5` là bác sĩ thú y (`VET`) |
| Người dùng | `demo.user2` đến `demo.user30` | `user2` đến `user20` có hồ sơ người nhận nuôi, `user21` đến `user30` chưa có. `user6` là gia đình đang có việc đỏ ở kịch bản 2.3, `user12` là người cứu ca R-D0030 |

Email đầy đủ có dạng `demo.user2@petrescue.demo`.

## Dữ liệu có sẵn

Với `demo.scale = 1`, dữ liệu trải đều trong 6 tháng gần nhất để dashboard tháng nào cũng có số liệu:

| Loại | Số lượng |
|---|---|
| Tài khoản | 47, có họ tên và số điện thoại giống thật |
| Tổ chức | 8 (6 `ACTIVE`, 2 `PENDING`), có tên, địa chỉ, mô tả và ảnh riêng |
| Thú cưng | 62 (31 chó, 31 mèo), mỗi bé có tên, giống, mô tả và ảnh đúng loài. 60 bé có đặc điểm cho matching |
| Hồ sơ y tế | 64 (40 lần tiêm phòng, 24 lần điều trị) |
| Ca cứu hộ | 83, đủ các trạng thái, ở 24 phường thật của TP.HCM, Hà Nội, Đà Nẵng. Có 1 ca báo lạc, 1 ca tìm thấy và 1 gợi ý ghép. 48 ca đang mở có kết quả AI phân loại |
| Báo cáo hoàn tất | 37, trong đó 1 bị khiếu nại |
| Cập nhật tiến độ ca, cờ | 3 cập nhật, 4 cờ trên 2 ca |
| Đơn nhận nuôi | 37 |
| Hồ sơ người nhận nuôi | 20 |
| Hỏi thăm sau nhận nuôi | 9 mốc, 3 việc cần xử lý (đỏ, vàng, đã xong), 1 lần thu hồi |
| Banner | 3 |
| Bài đăng cộng đồng (Mongo) | 26, trong đó 11 bài gắn với ca cứu hộ (9 ca còn cần giúp) và 2 bài của user1 có kết quả AI |
| Cuộc trò chuyện (Mongo) | 3, đều của user1 |

Muốn nhiều dữ liệu hơn để đo hiệu năng, đổi `demo.scale` ở đầu `seed-postgres.sql`. Mức 100 tạo 8.000 ca cứu hộ và 6.000 thú cưng (tên và ảnh lặp lại). Dữ liệu của các kịch bản ở trên không nhân theo `demo.scale`.

### Quy ước dữ liệu

- **Giá trị danh mục và enum lưu bằng tiếng Anh, một dạng duy nhất**, đúng như API lưu: loài `Dog`/`Cat`, giống theo tên trong `pet_breeds`, enum viết hoa (`MALE`, `SMALL`, `CRITICAL`), tag dạng slug (`lost-pet`). Tỉnh lưu theo tên trong danh mục địa giới (`Thành phố Hồ Chí Minh`). Nội dung người dùng tự viết (mô tả, màu lông, ghi chú, địa chỉ) là tiếng Việt.
- **Mã dữ liệu mẫu** có chữ `D` sau dấu gạch: `U-DU0001`, `O-D0001`, `P-D0001`, `R-D0001`, `A-D0001`. Mã thật do API cấp chỉ có số nên không bao giờ trùng.
- **Bản ghi riêng của kịch bản đánh số từ 9001** (`R-D9001`, `A-D9001`, `P-D9001`...), để không đụng các bản ghi sinh tự động.
- Tổ chức 4 và 8 đang `PENDING`, nên không có thú cưng hay ca nào gắn với chúng.

Test `DemoDataScriptsIntegrationTest` chạy cả hai script seed và script cleanup trên PostGIS và Mongo thật. Test kiểm tra từng kịch bản ở trên đều có dữ liệu, cho job tính điểm phù hợp chấm các đơn mẫu, và đọc chat cùng kết quả AI qua chính API. Khi schema đổi mà script chưa sửa theo, test sẽ báo.

## Ảnh

Ảnh lấy từ Unsplash (giấy phép miễn phí), lưu trong `media_files.public_id` dưới dạng URL đầy đủ `https://images.unsplash.com/photo-...`. API trả nguyên URL này, vì Cloudinary SDK không đổi một `public_id` đã là URL (xem `CloudinaryUrlPassThroughTest`). Không cần tải gì lên Cloudinary của staging.

Ảnh hồ sơ nhận nuôi và ảnh ca cứu hộ là hai bộ riêng: ảnh ca là ảnh chó mèo ngoài đường. Danh sách ảnh nằm ở bảng `demo_photo` trong `seed-postgres.sql` và mảng `PHOTOS` trong `seed-mongo.js`. Hai nơi phải giống nhau, cả về thứ tự.

## Xóa dữ liệu mẫu

```powershell
docker run --rm -v "${PWD}/scripts/demo-data:/demo" postgres:16-alpine psql $env:STAGING_DB_URL -q -v ON_ERROR_STOP=1 -f /demo/cleanup-postgres.sql
docker run --rm -v "${PWD}/scripts/demo-data:/demo" mongo:7 mongosh $env:STAGING_MONGO_URI --quiet --eval "var CLEANUP = true" /demo/seed-mongo.js
```

Script cleanup PostgreSQL xóa cả dữ liệu mẫu của phiên bản cũ (mã `DM...`).

Dữ liệu phát sinh khi demo gắn với tài khoản, tổ chức, thú cưng hoặc ca mẫu cũng bị xóa theo, kể cả khi do tài khoản thật tạo. Ví dụ: đơn của người thật xin nhận nuôi thú mẫu.

Script cleanup Mongo xóa bài đăng mẫu cùng comment và lượt thích của chúng, và 3 cuộc trò chuyện mẫu cùng tin nhắn và inbox. Nó không xóa cuộc trò chuyện mới mở trong lúc demo, hay comment mà tài khoản mẫu viết trên bài thật.
