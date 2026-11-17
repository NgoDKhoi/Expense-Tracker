# Expense Tracker — Tính năng Hệ thống (4 Phần cho Slide)

Tài liệu tóm tắt ngắn gọn các tính năng đã hoàn thiện của **Expense Tracker** (Android Java 17, Clean Architecture + MVVM, Pure Dagger 2), chia thành **4 phần** để dễ trình bày trên slide.

---

## Phần 1: Ghi nhận Kiểu Locket & Điều hướng Cử chỉ
- **Camera-First tức thì (`CameraX`)**: Mở app vào thẳng camera chụp hóa đơn, hỗ trợ Flash và nút chụp lò xo tùy biến (`CaptureButtonView`).
- **Nhập giao dịch siêu tốc**: Định dạng tiền tự động (`₫`), chọn loại **Thu / Chi**, chọn ví nhanh ngay trên header và danh mục động (10 danh mục mặc định + tự tạo mới với 8 màu).
- **Vuốt 2 chiều (`ViewPager2`)**:
  - **Vuốt ngang**: Chuyển mượt giữa *Dashboard ↔ Camera ↔ Lưới ảnh 3 cột*.
  - **Vuốt dọc**: Xem lại chi tiết từng hóa đơn toàn màn hình kiểu TikTok/Reels.
- **Trải nghiệm hoàn chỉnh**: Onboarding 3 slide cho người dùng mới, hỗ trợ song ngữ **Tiếng Việt / Tiếng Anh**.

---

## Phần 2: Quản lý Tài chính — Dashboard, Ví & Sổ cái
- **Tổng quan trực quan (`Dashboard`)**: Biểu đồ tròn `PieChart` phân bổ chi tiêu, lọc theo **Ngày / Tháng / Năm** và danh sách giao dịch gần đây.
- **Quản lý đa ví (`Wallets`)**: Hiển thị dạng thẻ ngân hàng với màu/icon riêng; hỗ trợ **Thêm / Sửa / Xóa** và tự động tính số dư theo giao dịch (`WalletWithBalance`).
- **Sổ cái thông minh (`History`)**: Tìm kiếm real-time, lọc theo *Tất cả / Chi tiêu / Thu nhập*, hỗ trợ **vuốt để xóa** kèm thanh hoàn tác (Undo 4 giây).

---

## Phần 3: Gamification & Trợ lý AI Heo Tiết Kiệm
- **Linh thú Heo Tiết Kiệm**: Tính `dailySpent`, `streakDays` và `totalSafeDays` từ Room DB thật; có **3 trạng thái cảm xúc** (*Vui / Lo lắng / Buồn*), **4 cấp tiến hóa** (`BABY → STUDENT → ADULT → ROYAL`), tương tác chạm nảy lò xo và chỉnh sửa hạn mức ngày.
- **Bảng xếp hạng (`Leaderboard`)**: Hiển thị thẻ vị trí hiện tại theo *Chuỗi Streak* hoặc *Tổng ngày đạt chuẩn* (Toàn cầu / Bạn bè giả lập), đồng bộ tên người dùng đang đăng nhập.
- **Chatbot AI (`Gemini 2.0 Flash`)**: Chat tư vấn tài chính với persona Heo Đất, **4 nút hỏi nhanh** và **chế độ Offline dự phòng** tự phân tích dữ liệu tại máy khi mất mạng.

---

## Phần 4: Tài khoản Supabase & Đồng bộ Hai chiều
- **Xác thực đa phương thức**: Đăng nhập/Đăng ký qua **Email**, **Username**, xác thực **OTP 6 số** (đăng ký & quên mật khẩu) và **Đăng nhập với Google (OAuth)** qua Deep Link `expensetracker://auth-callback`.
- **Offline-First & Đồng bộ tự động (`WorkManager` + PostgREST)**:
  - Dùng bình thường ở chế độ Khách không cần mạng.
  - Tự động đẩy/kéo dữ liệu khi thay đổi, khi có mạng trở lại, định kỳ mỗi **6 giờ** hoặc bấm *Đồng bộ ngay*.
- **Xử lý xung đột & Gộp dữ liệu**: Giải quyết xung đột theo mốc mới hơn (*newer edit wins*), xóa mềm, hỗ trợ **gộp hoặc xóa dữ liệu Khách** khi đăng nhập và cảnh báo thay đổi chưa đồng bộ lúc đăng xuất.
