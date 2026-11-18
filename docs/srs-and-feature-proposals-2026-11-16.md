# PawHome (Pet Rescue) — Mô tả dự án, SRS rút gọn và đề xuất tính năng

- **Ngày:** 16/11/2026
- **Căn cứ:** `README.md`, `agent/01-scope-da2.md`, `agent/02-plan.md`, `docs/ux-walkthrough-2026-11-11.md`, `docs/fe-be-status.md`, danh sách controller và migration V1–V27.
- **Trạng thái tài liệu:** bản nháp để tinh chỉnh. Mục 5 là đề xuất, chưa phải quyết định.
- **Đính chính 16/11:** bản đầu dựa trên plan ngày 03/11 và nhánh `fix/query-performance`. Đối chiếu lại với `master` (`fdb7b22`) thì thông báo đơn nhận nuôi, lọc thú theo tỉnh, banner công khai và ghép đôi (V28) **đã có**; trạng thái bên dưới đã sửa theo `master`. `agent/02-plan.md` chưa phản ánh điều này.

---

## 1. Mô tả dự án

### 1.1 Bài toán

Ở Việt Nam, người dân thường gặp chó mèo bị thương, bị bỏ rơi nhưng **không biết báo cho ai**, và thông tin chỉ nằm rải rác trên mạng xã hội, nhóm Facebook, rất dễ trôi. Về phía nhận nuôi, người muốn nhận nuôi **khó biết mình có hợp với con thú đó không**; việc nhận nuôi thất bại (trả lại, bỏ rơi) thường xảy ra trong những ngày đầu mà không ai theo dõi.

### 1.2 Giải pháp

PawHome là nền tảng kết nối **người báo – người cứu – tổ chức cứu hộ – người nhận nuôi** trên một chuỗi liền mạch:

```text
Báo ca → (AI phân loại) → Hàng chờ / Nhận ca → Hồ sơ thú → Ghép đôi giải thích được → Nhận nuôi → Theo dõi sau nhận nuôi
```

Đây là backend REST (Spring Boot 3.5, Java 21, Clean Architecture + CQRS) với PostgreSQL/PostGIS, MongoDB (chat, bài đăng), Redis, RabbitMQ, Cloudinary; frontend web riêng (`paw-home-ui.vercel.app`). Đồ án DA2, hạn đầu tháng 1/2027.

### 1.3 Đối tượng sử dụng

| Vai trò | Mô tả | Nhu cầu chính |
| --- | --- | --- |
| Khách | Chưa đăng nhập | Xem thú cần nhận nuôi, bản đồ ca, banner |
| Người dùng (`USER`) | Người dân thường | Báo ca, nhận cứu ca, nhận nuôi, đăng bài, chat, quản lý "Thú của tôi" |
| Thành viên tổ chức (`MEMBER`: `OWNER`/`STAFF`/`VET`) | Trạm cứu hộ, phòng khám | Nhận ca, quản lý thú, duyệt đơn nhận nuôi, theo dõi sau nhận nuôi |
| Quản trị (`ADMIN`) | Vận hành nền tảng | Kiểm duyệt, thống kê, xử lý tranh chấp |

### 1.4 Phạm vi

**Trong phạm vi:** báo và điều phối ca cứu hộ (kèm bản đồ), hồ sơ thú, nhận nuôi + ghép đôi, theo dõi sau nhận nuôi, mạng xã hội cơ bản (bài đăng, bình luận, chat), thông báo, AI phân loại văn bản.

**Ngoài phạm vi (theo đề cương):** tự huấn luyện LLM, chẩn đoán bệnh bằng AI, nhận diện thú qua ảnh, moderation/scam detection bằng AI, ứng dụng mobile native, microservices.

**Nguyên tắc AI:** AI chỉ phân tích và đề xuất; quyết định thuộc về người có thẩm quyền; AI không chấm điểm ghép đôi; chỉ gửi văn bản, không gửi ảnh hay hồ sơ y tế.

---

## 2. Yêu cầu chức năng (SRS rút gọn)

Trạng thái: ✅ đã có ở backend · 🔶 một phần · ⬜ chưa có (theo plan). Mức ưu tiên: **M**ust / **S**hould / **C**ould.

### 2.1 Tài khoản và phân quyền

| ID | Yêu cầu | Ưu tiên | TT |
| --- | --- | --- | --- |
| FR-AUTH-01 | Đăng ký, đăng nhập, JWT access/refresh, đăng xuất | M | ✅ |
| FR-AUTH-02 | Xác minh email (token 24 giờ), gửi lại, quên/đặt lại mật khẩu | M | ✅ |
| FR-AUTH-03 | Khoá đăng nhập 30 phút sau 5 lần sai | S | ✅ |
| FR-AUTH-04 | Phân quyền theo vai trò hệ thống và vai trò trong tổ chức; chặn thao tác trái quyền (403) | M | ✅ |
| FR-USER-01 | Xem/sửa hồ sơ cá nhân (`GET /users/me`), kèm tổ chức và vai trò | M | ✅ |
| FR-USER-02 | Kết bạn, xem hồ sơ người khác (không lộ email/SĐT công khai) | C | 🔶 (Q11) |

### 2.2 Cứu hộ

| ID | Yêu cầu | Ưu tiên | TT |
| --- | --- | --- | --- |
| FR-RES-01 | Người đăng nhập báo ca: loài, mô tả, mức khẩn cấp, ảnh, toạ độ, tỉnh/phường, SĐT liên hệ | M | ✅ |
| FR-RES-02 | Xem ca theo bản đồ (bán kính, khung nhìn), lọc theo trạng thái/mức khẩn cấp | M | ✅ |
| FR-RES-03 | Nhận ca: người thường (mềm, có hạn, tối đa 2 ca) và tổ chức (khoá ca); gia hạn, trả ca, hết hạn tự động | M | ✅ |
| FR-RES-04 | Báo cáo hoàn tất kèm ảnh; người báo xác nhận/phản đối; tổ chức/admin duyệt | M | ✅ |
| FR-RES-05 | Hàng chờ của tổ chức: ca chưa nhận cùng tỉnh, xếp theo khẩn cấp rồi thời gian | M | ✅ |
| FR-RES-06 | Intake: tạo hồ sơ thú từ ca đã cứu, hoặc người cứu giữ thú (`keep-pet`) | M | ✅ |
| FR-RES-07 | Ẩn SĐT liên hệ, chỉ người nhận/người báo/tổ chức/admin thấy | M | ✅ |
| FR-RES-08 | AI phân loại ca (khẩn cấp, tình trạng), gắn cờ ca trùng | S | ⬜ |
| FR-RES-10 | Cảnh báo ca có thể trùng trước khi báo (`GET /rescue-cases/possible-duplicates`: ca còn mở, trong 48 giờ, bán kính 300 m) | S | ✅ (16/11) |
| FR-RES-09 | Ba loại ca: cứu hộ / lạc / tìm thấy, khớp văn bản | C | ⬜ |

### 2.3 Thú và nhận nuôi

| ID | Yêu cầu | Ưu tiên | TT |
| --- | --- | --- | --- |
| FR-PET-01 | CRUD hồ sơ thú, ảnh, hồ sơ y tế, lịch sử chủ, phân loại loài/giống song ngữ | M | ✅ |
| FR-PET-02 | Tìm thú nhận nuôi theo loài, tuổi, giới tính, **tỉnh** | M | ✅ |
| FR-ADO-01 | Nộp đơn nhận nuôi; tổ chức duyệt/từ chối/huỷ; hoàn tất; thu hồi có nhật ký | M | ✅ |
| FR-ADO-02 | **Thông báo** cho người nộp đơn khi duyệt/từ chối/hoàn tất, và cho tổ chức khi có đơn mới | M | ✅ |
| FR-ADO-03 | Hồ sơ người nhận nuôi (nhà ở, thời gian, trẻ em, thú đang nuôi) | S | ✅ |
| FR-ADO-04 | Ghép đôi giải thích được: cảnh báo + 5 tiêu chí + lý do tiếng Việt; vi phạm chỉ cảnh báo, tổ chức quyết định | S | ✅ |
| FR-ADO-05 | Theo dõi sau nhận nuôi (ngày 2, tuần 2), tự trả thú không trừ điểm, ghi kết cục | S | ⬜ |
| FR-ADO-06 | "Thú của tôi": phân biệt thú nhận nuôi và thú tự đăng | S | ⬜ |

### 2.4 Cộng đồng và thông báo

| ID | Yêu cầu | Ưu tiên | TT |
| --- | --- | --- | --- |
| FR-SOC-01 | Bài đăng, bình luận, like (MongoDB; đếm bất đồng bộ) | S | ✅ |
| FR-SOC-02 | Chat realtime (WebSocket `/chat-ws`), tự mở chat khi nhận ca | S | ✅ |
| FR-SOC-03 | AI phân tích bài đăng (loại bài, loài, địa điểm, thẻ), người dùng xác nhận | C | ✅ (chưa thử với key thật) |
| FR-NOT-01 | Thông báo trong ứng dụng (lưu, đếm chưa đọc, đẩy realtime, Expo push) | M | ✅ (mới phủ chuỗi cứu hộ) |

### 2.5 Quản trị

| ID | Yêu cầu | Ưu tiên | TT |
| --- | --- | --- | --- |
| FR-ADM-01 | Dashboard thống kê (cache Redis), quản lý role/tag/banner/tổ chức/user | M | ✅ |
| FR-ADM-02 | Hàng chờ tranh chấp và ca quá hạn | S | ✅ |

---

## 3. Yêu cầu phi chức năng

| ID | Nhóm | Yêu cầu |
| --- | --- | --- |
| NFR-01 | Hiệu năng | Danh sách/tìm kiếm trả trong ≤ 500 ms ở dữ liệu demo; không N+1 (đã có index V27, xem `performance-audit-2026-11-04.md`) |
| NFR-02 | Tài nguyên | Chạy một tiến trình trong 512 MB RAM (Render free tier); đo lại sau mỗi giai đoạn |
| NFR-03 | Bảo mật | JWT; kiểm tra quyền ở tầng domain; không lộ SĐT/email công khai; giảm access token từ 10 ngày xuống 15 phút khi frontend đã có refresh (Q10) |
| NFR-04 | Riêng tư/AI | Chỉ gửi văn bản lên LLM; nội dung người dùng là dữ liệu không tin cậy, validate đầu ra theo schema |
| NFR-05 | Độ tin cậy | Tắt/lỗi AI không làm hỏng báo ca và hàng chờ; thao tác nhận ca an toàn khi đồng thời |
| NFR-06 | Khả dụng | Giao diện tiếng Việt; thông báo lỗi dễ hiểu; dùng được trên điện thoại (web responsive) |
| NFR-07 | Bảo trì | Clean Architecture/CQRS; migration Flyway; test tự động (hiện 198 test); CI trên `master` |
| NFR-08 | Quan sát | Log lỗi, thống kê AI (`/admin/ai/post-analysis/metrics`) |

---

## 4. Ràng buộc và giả định

- Hạn nộp đầu tháng 1/2027; nhóm 2 sinh viên → phạm vi phải cắt được theo thứ tự đã ghi ở plan.
- **Không có tổ chức thật** (Q9): quy trình của tổ chức là giả định, dữ liệu đánh giá là giả lập và phải nói rõ trong báo cáo.
- Frontend đang đi chậm hơn backend khoảng hai giai đoạn (trang nhận nuôi hiện 0 thú; chưa có màn hình nhận ca, intake, hàng chờ, thông báo). **Một tính năng backend không có màn hình thì chưa có giá trị với người dùng.**
- Phụ thuộc ngoài: Cloudinary, Gemini/Claude (cần key), MongoDB Atlas, Render.

---

## 5. Đề xuất tính năng theo giá trị thực tế

**Tiêu chí chấm:** (1) có giải quyết một nỗi đau thật của người dùng không; (2) có giúp cứu/nhận nuôi được nhiều thú hơn hoặc ít thất bại hơn không; (3) công sức nhỏ so với tác dụng; (4) dùng được ngay cả khi chưa có tổ chức thật. Độ khó kỹ thuật **không** là tiêu chí.

### 5.1 Nhóm A — Nên làm ngay (giá trị cao, công sức thấp)

> A1, A2, A3 đã có trên `master`; còn lại A4–A6, chủ yếu là việc frontend.

| # | Tính năng | Vì sao có giá trị thực tế | Công sức |
| --- | --- | --- | --- |
| A1 | **Thông báo vòng đời đơn nhận nuôi** (duyệt/từ chối/hoàn tất → người nộp; đơn mới → tổ chức) | Hiện người nộp đơn không biết kết quả → bỏ cuộc hoặc nộp trùng; tổ chức không biết có đơn → đơn nằm chết. Hạ tầng thông báo V26 đã có | Nhỏ |
| A2 | **Lọc thú theo tỉnh/thành** (+ mặc định theo tỉnh của người dùng) | Người ở TP.HCM không nhận nuôi con thú ở Hà Nội; DTO đã có `province`, chỉ thiếu tham số lọc | Rất nhỏ |
| A3 | **Cho khách xem banner/danh sách thú không cần đăng nhập** (sửa 401) | Khách đầu tiên vào web thấy trang trống là thoát | Rất nhỏ |
| A4 | **Nút "Gọi/nhắn người báo" an toàn**: dùng chat sẵn có thay vì lộ SĐT | Người cứu cần liên lạc nhanh, người báo cần riêng tư | Nhỏ |
| A5 | **Trạng thái ca hiển thị cho người báo ("ca của tôi")** đi kèm timeline | Người báo muốn biết "có ai đến chưa" — là lý do họ quay lại | Nhỏ (API có sẵn, cần màn hình) |
| A6 | **Đóng vòng frontend cho chuỗi cứu hộ**: màn hình nhận ca, hàng chờ, thông báo, intake | Backend đã xong nhưng người dùng chưa thấy được; đây là điểm nghẽn lớn nhất | Vừa (phần FE) |

### 5.2 Nhóm B — Giá trị cao, công sức vừa (đã nằm trong plan, giữ lại)

| # | Tính năng | Vì sao có giá trị thực tế |
| --- | --- | --- |
| B1 | **Theo dõi sau nhận nuôi (ngày 2, tuần 2) + tự trả thú không trừ điểm** | Thất bại nhận nuôi dồn vào những ngày đầu; hỏi đúng lúc là cách rẻ nhất để giảm trả/bỏ thú. Không cần AI, không cần tổ chức thật mới có giá trị cho người nhận |
| B2 | **Ghép đôi giải thích được, chỉ cảnh báo** | Giúp người nhận nuôi tự biết mình có hợp không *trước khi* nộp đơn, giảm đơn không phù hợp cho tổ chức |
| B3 | **"Thú của tôi" + sổ sức khoẻ + nhắc tiêm phòng/tẩy giun** | Lý do để người dùng thường ở lại sau khi nhận nuôi; nhắc lịch tiêm là nhu cầu có thật, dùng hằng ngày |
| B4 | **AI phân loại mức khẩn cấp cho ca** (không hạ mức, chỉ gợi ý thứ tự) | Tổ chức ít người, cần biết ca nào đi trước; AI hỏng thì vẫn chạy bình thường |

### 5.3 Nhóm C — Đề xuất mới, giá trị thực tế cao, chưa có trong plan

| # | Tính năng | Vấn đề giải quyết | Công sức |
| --- | --- | --- | --- |
| C1 | **Hướng dẫn sơ cứu theo tình huống** (tĩnh, hiện khi báo ca: "thú bị xe đụng — đừng di chuyển, giữ ấm, gọi...") + danh bạ **phòng khám thú y 24/7 theo tỉnh** | Trong 15 phút đầu quyết định sống/chết; người báo thường chỉ biết chụp ảnh rồi đi. Nội dung tĩnh, không cần AI, có thể gom từ nguồn công khai | Nhỏ |
| C2 | **Cảnh báo ca trùng khi báo** (cùng khu vực, cùng loài, 24–48 giờ gần đây, dùng `ST_DWithin` sẵn có; không cần AI) | Một con mèo thường bị 5 người báo → tổ chức mất thời gian, người cứu đến thấy đã có người. Plan hiện cắt phần này | Nhỏ–vừa |
| C3 | **Bản tin cập nhật ca** (người nhận đăng ảnh/ghi chú tiến độ; người báo và người theo dõi nhận thông báo) | Người báo hỏi "con mèo đó giờ sao rồi" là tâm lý phổ biến nhất; tạo niềm tin và lan truyền câu chuyện đẹp | Nhỏ |
| C4 | **Câu chuyện thành công** (sau khi nhận nuôi ổn định ≥ 2 tuần, người nhận có thể đăng ảnh "trước – sau" lên feed) | Là nội dung hấp dẫn nhất để kéo người mới đến nhận nuôi; thay "feed cá nhân hoá" phức tạp | Nhỏ |
| C5 | **Danh sách cần giúp đỡ (wishlist) của tổ chức**: thức ăn, thuốc, cát, chăn; kèm cách ủng hộ (STK/QR, hoặc "tôi sẽ mang đến") | Trạm cứu hộ thiếu nguồn lực hơn thiếu người dùng; chỉ là thẻ thông tin + liên hệ, chưa cần thanh toán thật | Nhỏ |
| C6 | **Đăng ký nhà tạm nuôi (foster)**: người dùng khai tỉnh, loài nhận được, số ngày; tổ chức tìm khi hết chỗ | Thiếu chỗ tạm là nút thắt thực tế nhất của mọi trạm cứu hộ; dùng được cả khi chưa có ghép đôi | Vừa |
| C7 | **Mã thú/QR thẻ tên có SĐT chủ** (in được) cho thú nhận nuôi và "Thú của tôi" | Giảm lạc không quay về; không cần nhận diện ảnh. Nối được với luồng "Báo lạc" sau này | Nhỏ |
| C8 | **Báo cáo "ca bị báo sai/đã xử lý xong"** (một nút cho cộng đồng) | Giữ hàng chờ sạch, người cứu không chạy vô ích | Nhỏ |
| C9 | **Tuỳ chọn ngôn ngữ + chữ lớn cho người lớn tuổi**; đăng nhập bằng số điện thoại/Google | Rào cản đăng ký là lý do người không báo ca (Q12 bắt buộc đăng nhập) | Vừa |
| C10 | **Trang thống kê công khai** ("tháng này: 42 ca báo, 31 được cứu, 12 thú tìm được nhà") | Tạo niềm tin, thu hút tổ chức/nhà tài trợ; dữ liệu sẵn có từ dashboard admin | Nhỏ |

### 5.4 Nhóm D — Để sau / không khuyến nghị lúc này

| Tính năng | Lý do |
| --- | --- |
| Ba loại ca (lạc/tìm thấy) + khớp | Hữu ích nhưng cần đủ người dùng mới có dữ liệu để khớp; plan đã đặt "cắt đầu tiên" — giữ nguyên |
| Thanh toán/quyên góp trực tuyến | Rủi ro pháp lý và vận hành, lợi ích với đồ án thấp; C5 đã đủ |
| Cho điểm uy tín phức tạp, chống lừa đảo bằng AI | Ngoài phạm vi, dễ sai; giữ báo cáo + kiểm duyệt thủ công |
| Nhận diện thú bằng ảnh | Ngoài phạm vi, tốn tài nguyên, 512 MB RAM không đủ |
| Tìm nhà mới giữa người dùng (rehoming) | Q3 đã chốt để sau |

### 5.5 Gợi ý thứ tự (khớp với hạn đầu tháng 1/2027)

1. **Tuần này (16/11):** A1, A2, A3 (chỉ sửa nhỏ), kèm C1 (nội dung tĩnh) — thấy tác dụng ngay khi demo.
2. **16/11 → 30/11:** A4–A6 để chuỗi cứu hộ có màn hình thật; C2, C3.
3. **30/11 → 14/12:** B2 (Matching) rồi B1 (theo dõi), giữ bản gọn như plan.
4. **14/12 → 21/12:** B4 (AI phân loại, bắt buộc) + C4, C10 (rẻ, làm demo đẹp).
5. **Cuối cùng, nếu còn thời gian:** C5–C8; B3 nhắc lịch tiêm.

> Nguyên tắc cắt: tính năng nào **không có màn hình và không có người dùng chạm vào** thì cắt trước, kể cả khi kỹ thuật thú vị.

---

## 6. Câu hỏi mở cần chốt

1. Có bổ sung C1–C3 (sơ cứu, cảnh báo ca trùng, bản tin ca) vào plan không? Đề xuất: **có**, đều nhỏ.
2. Frontend do ai làm và còn bao nhiêu tuần? Quyết định mức A6 có đạt kịp hay không.
3. Q10 (thời hạn access token) và Q11 (lộ email/SĐT ở `GET /users/{id}`) cần sửa trước demo vì liên quan bảo mật.
4. Có chấp nhận đăng nhập bằng Google/SĐT (C9) để hạ rào cản báo ca không?
