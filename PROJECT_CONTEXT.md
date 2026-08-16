# Ngữ cảnh Hiện tại của Dự án (Project Context)

**Mục đích của file này:** Lưu trữ trạng thái hiện tại, các quyết định kiến trúc đã thống nhất và tiến độ của đồ án để các AI Agent hiểu rõ ngữ cảnh trước khi bắt tay vào làm việc. File này thay thế cho việc giao task trực tiếp, giúp AI nhận thức được bối cảnh tổng thể thay vì chỉ là một danh sách "cần làm".

## 1. Trạng thái Dự án (Current State)
- **Giai đoạn:** Đang ở Phase 2 (Hoàn thiện UI/UX và triển khai Core Data/Camera).
- **Thành quả đã đạt được:**
  - Thiết lập thành công cấu trúc dự án (Java, MVVM, Clean Architecture).
  - Hoàn thiện UI/UX kiến trúc điều hướng (`ViewPager2` cho swipe 2 chiều).
  - Tích hợp thành công giao diện Camera-first (`CameraHostFragment`) kiểu Locket-style.
  - Tích hợp Chart (`DashboardFragment`) và danh sách hình ảnh (`PhotosFragment`).
- **Các thành phần cốt lõi đang phát triển:** Tích hợp CameraX (tối ưu lifecycle) và Room Database (xử lý luồng dữ liệu).

## 2. Ngữ cảnh Kỹ thuật (Technical Context)
- **Kiến trúc UI:** 
  - `MainActivity` đóng vai trò là Host chứa `ViewPager2` quản lý 2 luồng chính: Camera & Dashboard.
  - Các Fragment con bên trong có thể cuộn dọc (như TikTok) hoặc điều hướng bằng Bottom Navigation.
- **Data Flow:** 
  - Giao diện lấy dữ liệu thông qua ViewModel -> UseCase -> Repository -> Room/Retrofit. Mọi thao tác DB bắt buộc chạy trên Background Thread.
- **Trải nghiệm người dùng (UX):** Nhanh, không độ trễ (zero-lag), tập trung vào thao tác vuốt (swipe) và chụp ảnh nhanh. AI Assistant hoạt động dưới dạng bong bóng/FAB nổi.

## 3. Các vấn đề đang tập trung giải quyết (Focus Areas)
- **Tối ưu CameraX:** Đảm bảo CameraX khởi động cực nhanh và giải phóng bộ nhớ đúng cách theo lifecycle của Android để tránh rò rỉ bộ nhớ.
- **Đồng bộ Dữ liệu:** Thiết lập luồng lưu dữ liệu tạm (Offline-first) với Room trước khi đồng bộ API.

## 4. Kế hoạch tiếp theo (Next Steps)
- Hoàn thiện luồng chụp ảnh và lưu URI ảnh vào Room Database.
- Xử lý mượt mà chuyển đổi trạng thái giao diện khi người dùng thao tác lưu chi tiêu.
