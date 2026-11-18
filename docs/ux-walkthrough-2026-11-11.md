# Trải nghiệm hai vai và đánh giá plan — 2026-11-11

- **Nhánh đối chiếu:** `fix/query-performance` tại `90a05ba`. Nhánh này đã gộp `fix/phase-0-authorization`, `feature/phase-1-rescue-claims` và `fix/fe-api-audit`.
- **Plan đối chiếu:** `agent/02-plan.md` (cập nhật 03/11/2026).
- **Frontend đối chiếu:** `docs/fe-be-status.md`.
- **Cách làm:** đi theo từng bước của hai vai trên API thật của backend (157 endpoint, 24 controller), ghi lại bước nào chạy được, bước nào hụt. Không gọi lên server, chỉ đọc code và DTO.

## Kết luận ngắn

1. Backend đã có đủ chuỗi cứu hộ từ báo ca đến hồ sơ thú, cho cả người thường lẫn tổ chức. Phần này **hoàn thiện tốt**.
2. Chuỗi nhận nuôi **đứt ở khâu thông báo**: người nộp đơn không được báo khi duyệt, từ chối, hoàn tất; tổ chức dùng web không được báo khi có đơn mới. Sửa rẻ vì hạ tầng thông báo của V26 đã có.
3. Tìm thú nhận nuôi **không lọc theo tỉnh**, dù DTO đã trả `province`. Người ở TP.HCM phải lướt qua thú ở Hà Nội.
4. **Frontend là điểm nghẽn lớn nhất.** Trang nhận nuôi hiện 0 thú, không tạo được thú và trung tâm, và chưa có màn hình nào cho nhận ca, intake, hàng chờ, thông báo. Backend đi trước frontend khoảng hai giai đoạn. Rủi ro này chưa nằm trong bảng rủi ro của plan.
5. Về plan: giữ Matching và AI phân loại ca; **đảo thứ tự Giai đoạn 3 lên trước 2B**; làm key AI và smoke test thật ngay tuần này; chen ba việc nhỏ (thông báo nhận nuôi, lọc tỉnh, nới chữ ký điện tử) trước khi bắt đầu Matching.

---

## Vai 1 — Lan, người dùng thường

Lan ở TP.HCM, thấy một con mèo bị thương gần nhà, muốn báo ca. Sau đó Lan muốn nhận nuôi một con chó.

### Bước 1. Đăng ký và xác minh

| Việc | API | Kết quả |
| --- | --- | --- |
| Đăng ký | `POST /auth/register` | Chạy. Token xác minh email có hạn 24 giờ (Q12) |
| Xác minh, gửi lại mail | `GET /auth/verify-email`, `POST /auth/resend-verification` | Chạy |
| Quên mật khẩu | `POST /auth/forgot-password`, `POST /auth/reset-password` | Chạy |
| Xem hồ sơ của mình | `GET /users/me` | Chạy, có cả tổ chức và vai trò nếu có |

**Đánh giá:** đủ. Không có gì cản.

### Bước 2. Báo ca mèo bị thương

| Việc | API | Kết quả |
| --- | --- | --- |
| Tải ảnh | `POST /media/upload/temp` | Chạy |
| Báo ca | `POST /rescue-cases` | Chạy. Gửi được loài, màu, kích cỡ, mức khẩn cấp, mô tả, toạ độ, tỉnh, phường, ảnh, số liên hệ |
| Xem ca mình báo | `GET /rescue-cases/{id}` | Chạy |
| Lọc ca gần mình | `GET /rescue-cases?lat=&lng=&distance=` | Chạy |

**Hụt:**

- Không có gợi ý "ca này có thể trùng với ca gần đây" lúc báo. Plan đã cắt có chủ đích (ưu tiên 2 của Giai đoạn 1).
- Không có giới hạn số ca báo mỗi ngày. Cũng đã cắt.
- Phải đăng nhập mới báo được (Q12). Chấp nhận được, nhưng là rào cản với người báo lần đầu.

**Đánh giá:** tốt cho đồ án. Hai điểm hụt là hợp lý để cắt.

### Bước 3. Theo dõi ca sau khi báo

Có người nhận ca, rồi báo đã cứu xong. Lan được gì?

| Sự kiện | Thông báo cho Lan |
| --- | --- |
| Người thường nhận ca | `RESCUE_CLAIMED` |
| Tổ chức nhận ca | `RESCUE_ORGANIZATION_CLAIMED` |
| Người nhận trả ca, hết hạn | `RESCUE_CLAIM_RELEASED`, `RESCUE_CLAIM_EXPIRED` |
| Có báo cáo hoàn tất, cần Lan xác nhận | `RESCUE_COMPLETION_SUBMITTED` |
| Báo cáo được duyệt hoặc bị từ chối | `RESCUE_COMPLETION_APPROVED`, `RESCUE_COMPLETION_REJECTED` |
| Ca bị đóng, chuyển | `RESCUE_CASE_ENDED`, `RESCUE_CASE_MOVED` |

Lan đọc qua `GET /notifications`, `GET /notifications/unread-count`, đánh dấu đã đọc. Thông báo đẩy realtime qua `/chat-ws`. Khi có người nhận ca, chat giữa Lan và người nhận tự mở.

Lan xác nhận hoặc phản đối báo cáo qua `POST /rescue-cases/completions/{id}/confirm` và `/dispute`.

**Đánh giá:** đây là phần hoàn thiện nhất của hệ thống. Người báo biết ca đi đến đâu ở mọi bước.

### Bước 4. Lan tự nhận một ca khác

| Việc | API | Kết quả |
| --- | --- | --- |
| Nhận ca | `POST /rescue-cases/{id}/claim` với `safetyAcknowledged` | Chạy. Nhận mềm, hạn 4 giờ, tối đa 2 ca cùng lúc |
| Gia hạn, trả ca | `POST .../claim/extend`, `/claim/release` | Chạy. Có nhắc `RESCUE_CLAIM_EXPIRING` |
| Xem ca mình đang nhận | `GET /users/me/rescue-claims` | Chạy |
| Báo đã cứu xong | `POST /rescue-cases/{id}/complete` | Chạy, kèm `outcome` và ảnh |
| Giữ thú lại nuôi | `POST /rescue-cases/{id}/keep-pet` | Chạy, ghi `pets.rescue_case_id` |

**Hụt:**

- Nội dung lưu ý an toàn do frontend hiển thị, API chỉ nhận cờ đã đọc. Frontend chưa có màn hình này.
- Giới hạn 2 ca khoá theo ca, không theo người, nên vẫn gửi được nhiều yêu cầu cùng lúc (đã ghi ở `13-design-rescue-claim.md`).
- Thú giữ lại chưa hiện trong "Thú của tôi" vì chưa có `GET /users/me/pets` (Giai đoạn 2B).

**Đánh giá:** tốt. Luồng "người thường cứu được ca" là điểm khác biệt của sản phẩm và đã chạy đầu cuối.

### Bước 5. Tìm chó để nhận nuôi

| Việc | API | Kết quả |
| --- | --- | --- |
| Danh sách thú | `GET /pets?status=AVAILABLE` | Chạy. Lọc được `search`, `species`, `breed`, `gender`, `status`, `organizationId` |
| Chi tiết thú | `GET /pets/{id}` | Chạy |
| Hồ sơ y tế, lịch sử chủ của thú tổ chức | `GET /pets/{id}/medical-records`, `/ownerships` | Chạy, công khai cho thú tổ chức |
| Chọn loài, giống song ngữ | `GET /pets/taxonomy` | Chạy |

**Hụt:**

- **Không lọc theo tỉnh hoặc khoảng cách.** `PetSummaryResponseDto` đã có `province`, `ward`, nhưng `GET /pets` không nhận tham số này. Với 21 thú demo thì chưa thấy đau; với dữ liệu thật đây là bộ lọc đầu tiên người dùng tìm.
- Không có yêu thích hay lưu thú để xem sau.
- Chưa có xếp hạng phù hợp (Giai đoạn 2).
- Frontend: trang `/nhan-nuoi` gọi `GET /pets/available` nên hiện 0 thú (B1 trong audit FE).

**Đánh giá:** yếu nhất trong hành trình của Lan, nhưng phần lớn sửa rẻ.

### Bước 6. Nộp đơn nhận nuôi

`POST /adoptions` cần: `petId`, `experience` (10–2000 ký tự), `housingCondition` (enum), `liveCondition`, và **`signatureMediaId` bắt buộc**. `organizationId` nếu gửi sẽ bị bỏ qua, luôn lấy từ thú (sửa ở Giai đoạn 0).

**Hụt:**

- Chữ ký điện tử bắt buộc là rào cản nặng cho bản demo. Audit FE chưa mở được form này vì thú đang mở không cho nhận nuôi.
- Đơn chưa có câu hỏi có cấu trúc về trẻ em, thú đang nuôi, thời gian chăm. Đây là đầu vào của Matching, sẽ có ở Giai đoạn 2 qua `adopter-profile`.

### Bước 7. Chờ kết quả đơn

| Sự kiện | Thông báo cho Lan |
| --- | --- |
| Tổ chức duyệt | **Không có** |
| Tổ chức từ chối | **Không có** |
| Tổ chức hoàn tất, chuyển chủ | **Không có** |
| Thú bị thu hồi | **Không có** |

Lan chỉ biết khi tự vào `GET /adoptions/user/{userId}` hoặc `GET /adoptions/{id}` xem lại. `AdoptionCommandUseCase` chỉ đẩy Expo push cho thành viên tổ chức lúc nộp đơn, không ghi gì vào bảng `notifications`.

**Đánh giá:** đây là **chỗ đứt rõ nhất** trong trải nghiệm của Lan. Chuỗi cứu hộ báo đủ mọi bước, chuỗi nhận nuôi không báo bước nào.

### Bước 8. Sau khi nhận nuôi

Không có gì. Không có "Thú của tôi", không có sổ sức khoẻ của thú mình, không có hỏi thăm, không có đường "không nuôi được nữa" ngoài việc tổ chức thu hồi và trừ 300 điểm. Plan đã ghi nhận ở Giai đoạn 2B.

### Bước 9. Cộng đồng

Bài đăng, bình luận, thích, bạn bè, chat đều chạy, có phân tích AI bài đăng. Frontend chưa dùng chat và bạn bè. Form bài đăng trên web bắt gõ tay UUID (D1 trong audit FE).

---

## Vai 2 — Minh, nhân viên STAFF của một trạm cứu hộ ở TP.HCM

### Bước 1. Vào hàng chờ

`GET /organizations/me/rescue-queue`: ca chưa ai nhận, cùng tỉnh với tổ chức, xếp theo mức khẩn cấp rồi thời gian. Có phân trang.

**Đánh giá:** đúng thứ Minh cần. Không có bộ lọc theo loài hay bán kính trong hàng chờ, nhưng có thể dùng `GET /rescue-cases` với `lat`, `lng`, `distance` thay thế.

### Bước 2. Nhận ca và cứu

| Việc | API | Kết quả |
| --- | --- | --- |
| Nhận ca cho tổ chức | `POST /rescue-cases/{id}/claim` | Chạy. Tổ chức nhận là khoá ca, người thường không nhận chồng được |
| Xem ai đang nhận | `GET /rescue-cases/{id}/claims` | Chạy |
| Báo hoàn tất | `POST /rescue-cases/{id}/complete` | Chạy |
| Duyệt báo cáo của người thường | `POST /rescue-cases/completions/{id}/approve`, `/reject` | Chạy, STAFF được duyệt |
| Hàng chờ duyệt | `GET /rescue-cases/completions/review-queue` | Chạy |

### Bước 3. Intake thành hồ sơ thú

`POST /rescue-cases/{id}/intake` tạo thú thuộc tổ chức, trạng thái `FOSTERING`, ghi `rescue_cases.pet_id`. Làm lần hai trả 409.

Sau đó Minh thêm y tế (`POST /pets/{id}/medical-records`), ảnh (`POST /pets/{id}/media`, `/media/attach`, đặt ảnh chính), sửa hồ sơ (`PUT /pets/{id}`), rồi mở `AVAILABLE` qua `PATCH /pets/{id}/status`.

**Đánh giá:** chuỗi ca → thú liền mạch, không phải nhập lại. Đây là mục tiêu 3 của đề cương và đã nối xong ở phần cứu hộ.

### Bước 4. Nhận đơn nhận nuôi

| Việc | API | Kết quả |
| --- | --- | --- |
| Biết có đơn mới | Expo push | **Chỉ có push cho app di động.** Web không có Expo, không có bản ghi trong `notifications`, không có realtime |
| Danh sách đơn của tổ chức | `GET /adoptions/organization/{orgId}` | Chạy, có `petPrimaryImageUrl`, `applicantUsername`, `readyAt` |
| Xem đơn | `GET /adoptions/{id}` | Chạy, có `experience`, `liveCondition`, `housingCondition` |
| Xem uy tín người nộp | `GET /users/{id}/reputation` | Chạy |
| Duyệt, từ chối kèm lý do | `PATCH /adoptions/{id}/approve`, `/reject` | Chạy, STAFF được làm. Duyệt một đơn thì các đơn cạnh tranh tự đóng |
| Hoàn tất, chuyển chủ, thú thành `ADOPTED` | `PATCH /adoptions/{id}/complete` | Chỉ **OWNER**. Hợp lý |
| Thu hồi do vi phạm | `POST /adoptions/{id}/reclaim` | Chỉ OWNER, trừ 300 điểm |

**Hụt:**

- Giữa `APPROVED` và `COMPLETED` không có bước hẹn gặp hay thăm nhà. Minh phải chat tay với Lan. Chấp nhận được cho đồ án, nhưng là chỗ 2B có thể nối vào.
- Không có xếp hạng đơn theo độ phù hợp (Giai đoạn 2).
- Không có tổng quan riêng cho tổ chức. `GET /admin/dashboard/*` chỉ admin.

### Bước 5. Sau khi giao thú

Không có việc nào cho Minh: không có hỏi thăm ngày 2, tuần 2, không có hàng việc Đỏ. Minh không biết Lan có nuôi tốt không cho đến khi có chuyện phải thu hồi. Thuộc 2B.

---

## Điểm nghẽn chung: frontend

Theo audit 03/11, web `paw-home-ui.vercel.app`:

- Trang nhận nuôi gọi sai route, hiện 0 thú (B1).
- Không tạo được thú và trung tâm vì gửi sai trường (B2, B3).
- Ca cứu trợ tạo từ admin mất ảnh và số điện thoại (B4).
- Gửi `size` nhưng backend đọc `pageSize`, nên dropdown và bảng cắt ở 20 dòng (M1).
- Khách không thấy banner (B6, lỗi backend, đã sửa trên nhánh `fix/fe-api-audit`).

Và **chưa có màn hình nào** cho: nhận ca, gia hạn, trả ca, hoàn tất, intake, giữ thú, hàng chờ tổ chức, thông báo, chat, bạn bè. Tức là toàn bộ Giai đoạn 1 chưa demo được trên web.

Hạn đầu tháng 1/2027. Nếu frontend không đuổi kịp, bản demo phải dựa vào Swagger hoặc Postman có kịch bản sẵn. Nên quyết định sớm điều này thay vì để đến Giai đoạn 5.

---

## Đánh giá các feature trong plan sắp tới

Chấm theo ba trục: bắt buộc theo đề cương, giá trị cho người dùng thật, chi phí làm. Thang 1–5.

| Feature | Bắt buộc | Giá trị người dùng | Chi phí | Ý kiến |
| --- | --- | --- | --- | --- |
| Giai đoạn 2: Matching | 5 | 3 | 4 | **Giữ, làm bản gọn.** Mục tiêu 2 của đề cương. Giá trị người dùng chỉ trung bình vì ít thú; giá trị học thuật cao nhất |
| Giai đoạn 2 phần quan sát đặc điểm có nguồn, độ chắc chắn | 2 | 2 | 3 | **Hoãn.** Không có tổ chức thật (Q9) thì không ai quan sát. Để tổ chức điền thẳng đặc điểm thú |
| Giai đoạn 2B: Thú của tôi, `acquisition_type` | 1 | 5 | 1 | **Làm sớm.** Rẻ, đóng lỗ hổng "người thường không có gì sau khi nhận nuôi hoặc giữ thú" |
| Giai đoạn 2B: theo dõi ngày 2, tuần 2, tự trả thú | 1 | 3 | 3 | **Chỉ làm nếu còn giờ sau Giai đoạn 3.** Giá trị thật bị Q9 giới hạn: không có tổ chức xử lý việc Đỏ, dữ liệu kết cục cho Mục tiêu 4 sẽ là dữ liệu giả |
| Giai đoạn 3: AI mức khẩn cấp, tình trạng, chuẩn hoá | 5 | 4 | 3 | **Giữ, làm bản gọn, và bắt đầu smoke test ngay.** Đã một tuần từ 03/11 mà chưa có key, chưa gọi API thật. Đây là rủi ro lớn nhất về tiến độ |
| Giai đoạn 3: ca trùng và gộp | 1 | 3 | 4 | **Cắt.** Không có trong đề cương |
| Giai đoạn 4: ba loại ca, lạc – tìm thấy | 1 | 4 | 5 | **Cắt.** Đồng ý với plan |
| Giai đoạn 5: bộ bài đăng tiếng Việt có nhãn | 5 | — | 2 | **Bắt đầu ngay, song song.** Không phụ thuộc code |
| Giai đoạn 5: bộ cặp người – thú 30–50 cặp | 5 | — | 2 | Làm sau khi Matching xong, nhưng viết sẵn mẫu cặp từ bây giờ |

### Ba việc nhỏ nên chen vào trước Matching

Mỗi việc dưới một ngày, không có trong plan, và đóng thẳng ba chỗ đứt đã thấy ở trên.

1. **Thông báo in-app cho chuỗi nhận nuôi.** Người nộp đơn nhận thông báo khi duyệt, từ chối, hoàn tất, thu hồi. Thành viên tổ chức nhận thông báo khi có đơn mới. Dùng lại `NotificationDomainService` và cơ chế đẩy của V26, chỉ thêm `NotificationType` mới và gọi từ `AdoptionCommandUseCase`.
2. **Lọc `province` cho `GET /pets`.** Cột đã có trong DTO và bảng, chỉ thêm tham số và điều kiện truy vấn. Cân nhắc thêm `lat`, `lng`, `distance` như `GET /rescue-cases` đã có.
3. **Nới `signatureMediaId`.** Đổi thành tuỳ chọn, hoặc thay bằng ô cam kết đã đọc điều khoản. Cùng tinh thần Q2: không chặn, để tổ chức quyết. Làm cùng lúc với Q11, tách DTO công khai và riêng tư cho `GET /users/{id}`.

### Lịch đề xuất

So với lịch trong plan, khác ở ba chỗ: chen ba việc nhỏ vào tuần này, đưa Giai đoạn 3 lên trước 2B, và tách riêng "Thú của tôi" khỏi phần theo dõi.

| Tuần bắt đầu | Việc |
| --- | --- |
| 10/11 | Key AI và smoke test thật với dữ liệu giả. Ba việc nhỏ ở trên. Phối hợp frontend sửa B1, B6, B4, M1. Quyết định demo bằng web hay Swagger |
| 17/11, 24/11 | Giai đoạn 2 Matching bản gọn: `adopter-profile`, `MatchingService` thuần, `GET /pets/{id}/match`, `sort=match`, cờ cảnh báo trên đơn |
| 01/12 | Giai đoạn 3 bản gọn: sự kiện báo ca, worker, mức khẩn cấp, tình trạng, chuẩn hoá, hàng chờ dùng kết quả |
| 08/12 | Thú của tôi, `acquisition_type`, `GET /users/me/pets`. Theo dõi ngày 2 chỉ nếu còn giờ |
| 15/12, 22/12 | Giai đoạn 5: bộ dữ liệu, chỉ số, báo cáo, demo. Frontend đuổi kịp |
| 29/12 | Dự phòng |

### Rủi ro cần thêm vào plan

| Rủi ro | Cách giảm |
| --- | --- |
| Frontend chưa có màn hình cho Giai đoạn 1 và còn lỗi chặn ở các màn cũ | Chốt sớm phương án demo; ưu tiên frontend cho một đường demo duy nhất: báo ca → nhận → hoàn tất → intake → nộp đơn → duyệt |
| Chưa gọi API AI thật sau một tuần | Làm trong tuần 10/11, không dời sang Giai đoạn 3 |
| Chuỗi nhận nuôi không có thông báo nên demo thiếu "phản hồi" | Chen vào tuần 10/11 |
