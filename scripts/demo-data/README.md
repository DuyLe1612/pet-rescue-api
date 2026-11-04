# Dữ liệu mẫu cho staging và demo

**Chỉ dùng cho staging hoặc máy local, không chạy trên production.** Tất cả tài khoản dùng chung một mật khẩu công khai trong file này.

Các script không phải migration của Flyway, nên chỉ chạy khi có người gọi.

## Thứ tự chạy

Script chạy từ máy của bạn và kết nối thẳng tới database, nên staging trên Render free (không có shell) vẫn dùng được.

1. Deploy API lên staging và chờ khởi động xong, để Flyway và `MongoSchemaInitializer` tạo schema.
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

Chạy lại nhiều lần vẫn an toàn: bản ghi đã có được giữ nguyên.

## Tài khoản

Mật khẩu của mọi tài khoản: `Demo@12345`. Đăng nhập bằng email hoặc username.

| Vai trò | Email | Ghi chú |
|---|---|---|
| Quản trị | `demo.admin@petrescue.demo` | Vào được dashboard và hàng đợi review |
| Chủ tổ chức | `demo.owner1@petrescue.demo` | `owner1` đến `owner8`. Tổ chức 4 và 8 đang `PENDING` |
| Nhân viên | `demo.staff1@petrescue.demo` | `staff1` đến `staff8` |
| Người dùng | `demo.user1@petrescue.demo` | `user1` đến `user30` |

## Dữ liệu có sẵn

Với `demo.scale = 1`, dữ liệu trải đều trong 6 tháng gần nhất để dashboard tháng nào cũng có số liệu:

| Loại | Số lượng |
|---|---|
| Tài khoản | 47 |
| Tổ chức | 8 (6 `ACTIVE`, 2 `PENDING`) |
| Thú cưng | 60 |
| Ca cứu hộ | 80, gồm đủ các trạng thái và có tọa độ tại TP.HCM, Hà Nội, Đà Nẵng |
| Báo cáo hoàn tất | 36 |
| Đơn nhận nuôi | 28 |
| Bài đăng cộng đồng (Mongo) | 24 |

Script không tạo tin nhắn chat.

Muốn nhiều dữ liệu hơn để đo hiệu năng, đổi `demo.scale` ở đầu `seed-postgres.sql`. Mức 100 tạo 8.000 ca cứu hộ và 6.000 thú cưng.

## Ảnh

Ảnh dùng public id `samples/animals/*`, là ảnh mẫu Cloudinary thường tạo sẵn cho tài khoản mới. Nếu tài khoản staging đã xóa các ảnh này thì ảnh sẽ không hiện. Khi đó, sửa bảng `demo_image` trong `seed-postgres.sql` và mảng `images` trong `seed-mongo.js`.

## Xóa dữ liệu mẫu

```powershell
docker run --rm -v "${PWD}/scripts/demo-data:/demo" postgres:16-alpine psql $env:STAGING_DB_URL -q -v ON_ERROR_STOP=1 -f /demo/cleanup-postgres.sql
docker run --rm -v "${PWD}/scripts/demo-data:/demo" mongo:7 mongosh $env:STAGING_MONGO_URI --quiet --eval "var CLEANUP = true" /demo/seed-mongo.js
```

Dữ liệu phát sinh khi demo gắn với tài khoản, tổ chức, thú cưng hoặc ca mẫu cũng bị xóa theo, kể cả khi do tài khoản thật tạo. Ví dụ: đơn của người thật xin nhận nuôi thú mẫu.

Script cleanup Mongo chỉ xóa bài đăng mẫu cùng comment và lượt thích của chúng. Nó không xóa hội thoại, inbox, hay comment mà tài khoản mẫu viết trên bài thật.
