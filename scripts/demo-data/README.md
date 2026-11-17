# Dữ liệu mẫu cho staging và demo

**Chỉ dùng cho staging hoặc máy local, không chạy trên production.** Tất cả tài khoản dùng chung một mật khẩu công khai trong file này.

Các script không phải migration của Flyway, nên chỉ chạy khi có người gọi.

## Thứ tự chạy

Script chạy từ máy của bạn và kết nối thẳng tới database, nên staging trên Render free (không có shell) vẫn dùng được.

1. Deploy API lên staging và chờ khởi động xong, để Flyway và `MongoSchemaInitializer` tạo schema. Script PostgreSQL cần migration `V29` trở lên.
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

Chạy lại nhiều lần vẫn an toàn: bản ghi đã có được giữ nguyên. Vì vậy, nếu database đang có dữ liệu mẫu của phiên bản script cũ (mã `DM...`, tên "Trạm cứu hộ Demo 1"), hãy chạy phần [Xóa dữ liệu mẫu](#xóa-dữ-liệu-mẫu) trước rồi mới seed lại. Nếu không, dữ liệu cũ và mới sẽ nằm lẫn với nhau.

Script Mongo ghi vào database mà API đọc (`MONGO_DATABASE`, mặc định `petrescue`), không phải database trong connection string. Connection string của Atlas thường không có tên database, nên nếu dựa vào nó thì `mongosh` sẽ ghi nhầm vào `test`. Nếu server đặt `MONGO_DATABASE` khác `petrescue`, thêm `--eval "var DB_NAME = 'tên'"` trước tên file. Script dừng và báo lỗi nếu database đó chưa có collection `posts`.

## Tài khoản

Mật khẩu của mọi tài khoản: `Demo@12345`. Đăng nhập bằng email hoặc username.

| Vai trò | Email | Ghi chú |
|---|---|---|
| Quản trị | `demo.admin@petrescue.demo` | Lê Quang Hải. Vào được dashboard và hàng đợi review |
| Chủ tổ chức | `demo.owner1@petrescue.demo` | `owner1` đến `owner8`. `owner1` là chủ Trạm cứu hộ Mái Ấm Sài Gòn. Tổ chức 4 và 8 đang `PENDING` |
| Nhân viên | `demo.staff1@petrescue.demo` | `staff1` đến `staff8`. `staff5` có vai trò bác sĩ thú y (`VET`) |
| Người dùng | `demo.user1@petrescue.demo` | `user1` đến `user30` |

## Dữ liệu có sẵn

Với `demo.scale = 1`, dữ liệu trải đều trong 6 tháng gần nhất để dashboard tháng nào cũng có số liệu:

| Loại | Số lượng |
|---|---|
| Tài khoản | 47, có họ tên và số điện thoại giống thật |
| Tổ chức | 8 (6 `ACTIVE`, 2 `PENDING`), có tên, địa chỉ và mô tả riêng |
| Thú cưng | 60 (30 chó, 30 mèo), mỗi bé có tên, giống, mô tả và ảnh đúng loài |
| Hồ sơ y tế | 64 (40 lần tiêm phòng, 24 lần điều trị) |
| Ca cứu hộ | 80, đủ các trạng thái, ở 24 phường thật của TP.HCM, Hà Nội, Đà Nẵng |
| Báo cáo hoàn tất | 36 |
| Đơn nhận nuôi | 28 |
| Bài đăng cộng đồng (Mongo) | 24, trong đó 11 bài gắn với ca cứu hộ (9 ca còn cần giúp) |

Script không tạo tin nhắn chat.

Muốn nhiều dữ liệu hơn để đo hiệu năng, đổi `demo.scale` ở đầu `seed-postgres.sql`. Mức 100 tạo 8.000 ca cứu hộ và 6.000 thú cưng (tên và ảnh lặp lại).

### Quy ước dữ liệu

- **Giá trị danh mục và enum lưu bằng tiếng Anh, một dạng duy nhất**, đúng như API lưu: loài `Dog`/`Cat`, giống theo tên trong `pet_breeds`, enum viết hoa (`MALE`, `SMALL`, `CRITICAL`), tag dạng slug (`lost-pet`). Nội dung người dùng tự viết (mô tả, màu lông, ghi chú, địa chỉ) là tiếng Việt.
- **Mã dữ liệu mẫu** có chữ `D` sau dấu gạch: `U-DU0001`, `O-D0001`, `P-D0001`, `R-D0001`, `A-D0001`. Mã thật do API cấp chỉ có số nên không bao giờ trùng.
- Tổ chức 4 và 8 đang `PENDING`, nên không có thú cưng hay ca nào gắn với chúng.

Test `DemoDataScriptsIntegrationTest` chạy cả script seed và cleanup trên PostGIS thật, nên khi schema đổi mà script chưa sửa theo thì test sẽ báo.

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

Script cleanup Mongo chỉ xóa bài đăng mẫu cùng comment và lượt thích của chúng. Nó không xóa hội thoại, inbox, hay comment mà tài khoản mẫu viết trên bài thật.
