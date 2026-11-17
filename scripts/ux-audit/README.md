# Kịch bản Playwright cho audit UI/UX

Các kịch bản dùng để chạy lại phần kiểm tra trong `docs/fe-be-status.md`. Mỗi file là một hàm `async (page) => { ... }` tự chứa, chạy bằng công cụ `browser_run_code_unsafe` của Playwright MCP, truyền đường dẫn file vào `filename`. Đường dẫn tính từ gốc repo:

```
browser_run_code_unsafe { filename: "scripts/ux-audit/smoke-guest.js" }
```

| File | Làm gì |
|---|---|
| `smoke-guest.js` | Vai khách: `lang`, CTA ở hero, số bé sẵn sàng, enum thô còn sót, nút nhận nuôi bật hay tắt, nguồn tile bản đồ, lỗi console |
| `login-owner.js` | Lỗi khi để trống form, đăng nhập bằng chủ tổ chức, trang được chuyển tới, breadcrumb, màn tổng quan. Muốn thử vai khác thì chép file rồi đổi `EMAIL` |
| `contrast-audit.js` | Đo tương phản theo WCAG 1.4.3 trên trang đang mở, đọc được màu `lab()` và `oklch()` |
| `page-audit.js` | Đo tổng hợp trên trang đang mở: tương phản, vùng bấm < 24px, heading, ô nhập thiếu label, font, cỡ chữ |
| `_lib.js` | URL gốc và tài khoản demo, để tham khảo |

## Chạy

1. Bật FE: `pnpm dev --port 3100` trong `paw-home-ui`, với `NEXT_PUBLIC_API_URL` trỏ tới API có dữ liệu demo (`scripts/demo-data/`).
2. Gọi từng file như trên. `contrast-audit.js` và `page-audit.js` đo trang đang mở, nên phải mở trang cần đo trước.

## Lưu ý

- `browser_run_code_unsafe` chỉ chạy một hàm tự chứa, không `require` được file khác. Vì vậy `BASE` và tài khoản được viết lại trong từng file. Đổi `_lib.js` thì phải đổi theo trong các file kịch bản.
- `BASE` mặc định là `http://localhost:3100`. Muốn chạy trên bản deploy thì đổi `BASE` trong từng file.
- Kịch bản chỉ đọc: không gửi đơn, không tạo ca, không duyệt đơn. `login-owner.js` có bấm "Đăng nhập" khi form trống, nhưng lỗi bắt ở FE nên không có request nào lên server.
