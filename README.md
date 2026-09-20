# Expense Tracker

Expense Tracker là một ứng dụng di động native Android giúp người dùng quản lý chi tiêu cá nhân một cách trực quan và nhanh chóng. Ứng dụng tập trung vào trải nghiệm người dùng tối giản (minimalist), cho phép ghi nhận chi tiêu tức thì bằng hình ảnh, hoạt động trơn tru ngay cả khi không có mạng và được tích hợp AI để tư vấn tài chính.

Đồ án thuộc môn học: **Mobile Application Development (NT118)**.

## 🌟 Các tính năng chính (Core Features)

Dựa trên thiết kế UI/UX theo phong cách Locket (chụp nhanh - vuốt mượt) và kiến trúc Offline-first, ứng dụng cung cấp các tính năng nổi bật sau:

1. **Trải nghiệm Camera-First (Locket-style Capture)**
   - Màn hình chính của ứng dụng ngay lập tức mở Camera (không độ trễ) bằng CameraX.
   - Quy trình ghi nhận siêu tốc: Chụp hóa đơn/sản phẩm ➔ Nhập số tiền ➔ Lưu. Form tự động reset sẵn sàng cho lần chụp tiếp theo.

2. **Điều hướng thông minh bằng cử chỉ (Smart Swipe Navigation)**
   - **Vuốt ngang (Horizontal Swipe):** Chuyển đổi nhanh chóng giữa 3 luồng chính: Dashboard (Quản lý) ➔ Camera (Chụp ảnh) ➔ Photos (Lịch sử hóa đơn).
   - **Vuốt dọc (Vertical Swipe):** Trải nghiệm xem lại hóa đơn theo phong cách TikTok/Reels - vuốt lên/xuống để xem chi tiết từng giao dịch với hình ảnh full màn hình.

3. **Quản lý tài chính đa dạng (Dashboard & Wallets)**
   - Tóm tắt chi tiêu trực quan bằng biểu đồ (PieChart).
   - Quản lý đa ví (Wallets) để theo dõi ngân sách theo từng nguồn tiền.
   - Xem lịch sử giao dịch (Ledger) theo dạng cộng/trừ chi tiết như app ngân hàng.

4. **Đồng bộ hóa ngoại tuyến (Offline-first & Deferred Sync)**
   - Hoạt động mượt mà không cần mạng. Mọi giao dịch được lưu ngay lập tức vào Local Database (Room).
   - Cơ chế đồng bộ ngầm thông minh: Tự động phát hiện khi có mạng (Internet) để đẩy dữ liệu đã lưu tạm lên Server thông qua Background Service.

5. **Trợ lý Ngân sách AI (AI Assistant - Gemini API)**
   - Hoạt động dưới dạng Chat icon hoặc Floating Bubble.
   - Trợ lý AI tự động phân tích dữ liệu chi tiêu trong tháng (tổng hợp thành file JSON) và đưa ra những lời khuyên tài chính cá nhân hóa.

6. **Kho lưu trữ Hóa đơn trực quan (Receipt Grid)**
   - Toàn bộ chi tiêu được hiển thị dưới dạng lưới hình ảnh trực quan, giúp người dùng dễ dàng nhớ lại khoản tiền đó đã dùng vào việc gì thay vì chỉ đọc các con số khô khan.

## 🛠 Công nghệ sử dụng (Technology Stack)

Ứng dụng được phát triển bằng ngôn ngữ **Java** và thiết kế giao diện bằng **XML**, tuân thủ chặt chẽ kiến trúc **MVVM (Model-View-ViewModel)** để quản lý state và đảm bảo khả năng mở rộng.

### Core Android Components
- **Activity**: `MainActivity` đóng vai trò là host chính (Single-Activity Architecture), sử dụng `ViewPager2` để điều hướng swipe 2 chiều giữa các màn hình chính.
- **Fragment**: Gồm các màn hình độc lập như `DashboardFragment` (tổng quan & thẻ điều hướng), `CameraHostFragment` (Camera-first UI kiểu Locket-style), `PhotosFragment` (Lịch sử ảnh) và `AiAssistantFragment` (Trợ lý AI).
- **Service & Broadcast Receiver**: Cặp bài trùng `SyncService` và `NetworkChangeReceiver` được dùng để theo dõi trạng thái mạng và thực hiện tiến trình đồng bộ dữ liệu ngầm một cách đáng tin cậy.
- **Content Provider (Optional)**: Hỗ trợ chia sẻ dữ liệu chi tiêu an toàn với các ứng dụng khác nếu có yêu cầu.

### Thư viện & Công cụ (Libraries & Tools)
- **UI/UX**: Các component tiêu chuẩn như RecyclerView, CardView, Spinner.
- **Camera**: **CameraX** (PreviewView) cho trải nghiệm chụp ảnh mượt mà, khởi động cực nhanh và tối ưu hiệu suất.
- **Local Storage**: **Room Persistence Library** - Một abstraction layer của SQLite, dùng để lưu trữ dữ liệu ngoại tuyến (ví dụ: `ExpenseRecord` Entity) an toàn và dễ dàng truy vấn.
- **State Management**: **ViewModel** và **LiveData** để quản lý trạng thái UI, giúp giao diện tự động cập nhật theo thời gian thực (reactive) khi có thay đổi từ cơ sở dữ liệu.
- **Networking/API**: **Retrofit** xử lý các HTTP request (gọi API đồng bộ hóa và kết nối với AI Cloud API).
- **Image Loading**: **Glide** hoặc **Picasso** để tải và hiển thị thumbnail hình ảnh từ bộ nhớ thiết bị một cách mượt mà và tối ưu RAM.

## 📂 Kiến trúc & Luồng hoạt động (Architecture & Flow)

Kiến trúc áp dụng **Repository Pattern** để tạo ra một Single Source of Truth (SSOT), đóng vai trò trung gian giữa Room DAO (Local) và Retrofit (Remote API).

- **Dashboard**: Màn hình `DashboardFragment` hiển thị tổng quan chi tiêu (Chart) và điều hướng.
- **Add Expense (Camera-first)**: Màn hình `CameraHostFragment` full màn hình, chụp ảnh nhanh kiểu Locket-style. Lưu dữ liệu ngay lập tức vào Room với `syncStatus = PENDING`.
- **History**: Màn hình `PhotosFragment` hiển thị danh sách dạng lưới (grid) các ảnh hóa đơn/vật phẩm đã chi.
- **AI Assistant**: Giao diện thẻ thông tin (CardView) hoặc chat nhỏ gọn, nơi hiển thị các insight về tài chính được AI sinh ra sau khi phân tích data json của người dùng.

## 🚀 Kế hoạch phát triển (Development Phases)

Dự án được triển khai qua 5 giai đoạn:
1. **Foundation & Setup**: Khởi tạo project, cài đặt dependencies, setup MVVM và thiết kế XML cơ bản.
2. **Local Storage & Camera**: Cấu hình Room Database, viết các DAO; tích hợp CameraX để thực hiện tác vụ chụp và lưu ảnh offline.
3. **Deferred Syncing & Background Tasks**: Xây dựng Service đồng bộ ngầm khi `NetworkChangeReceiver` báo có mạng.
4. **UI/UX Flow & Data Display**: Hoàn thiện tính năng xem lịch sử với LiveData để binding dữ liệu ra UI.
5. **AI Integration & Polish**: Gọi API AI để lấy tư vấn, tối ưu hóa tốc độ khởi động camera, dọn dẹp code và hoàn thiện thiết kế UI/UX.

---
*(File README.md này được thiết kế để cung cấp cái nhìn tổng quan nhất cho cả Developer và AI Assistant tham gia vào quá trình xây dựng dự án Expense Tracker).*
