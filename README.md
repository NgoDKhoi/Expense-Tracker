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
   - Đăng nhập để sao lưu lên Supabase và dùng trên nhiều máy. Đồng bộ ngầm bằng WorkManager, tự chạy lại khi có mạng (xem mục *Sao lưu & đồng bộ*).

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
- **Background work**: `SyncWorker` (WorkManager) chạy đồng bộ ngầm, có điều kiện "có mạng" và tự thử lại; `NetworkChangeReceiver` (NetworkCallback) yêu cầu đồng bộ mỗi khi máy có mạng.
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

## 🔑 Cấu hình Supabase (Đăng nhập / Đăng ký)

Tài khoản người dùng dùng **Supabase Auth**, gọi qua REST API bằng Retrofit (module `:core:network`). Đăng nhập là **không bắt buộc**: app vẫn dùng offline bình thường, nút avatar góc phải màn hình Camera dùng để đăng nhập hoặc xem tài khoản.

**Thiết lập một lần cho project Supabase (chỉ người quản lý project làm):**
1. Tạo project trên [supabase.com](https://supabase.com).
2. Vào **Authentication → Sign In / Providers → Email**, tắt **Confirm email**, rồi bấm Save.
3. Bật **đăng nhập bằng tên đăng nhập**: vào **SQL Editor → New query**, dán toàn bộ nội dung file [`supabase/username_login.sql`](supabase/username_login.sql), rồi bấm **Run**. File này tạo bảng `profiles` (lưu tên đăng nhập, không trùng) và hàm `email_for_username`. Chạy lại nhiều lần không sao.
4. Tạo **bảng sao lưu dữ liệu**: làm tương tự với file [`supabase/sync_schema.sql`](supabase/sync_schema.sql). File này tạo 3 bảng `wallets`, `categories`, `transactions`, mỗi người dùng chỉ đọc và ghi được dữ liệu của chính mình (RLS). Chạy lại nhiều lần không sao.
5. (Tuỳ chọn) Vào **Authentication → URL Configuration**, đặt **Site URL**. Đây là trang mở ra khi bấm link trong email "Quên mật khẩu".

**Mỗi thành viên tự làm trên máy mình:**
1. Lấy **Project URL**: bấm nút **Connect** trên dashboard (hoặc **Settings → Data API**). URL có dạng `https://<project-ref>.supabase.co`, với `<project-ref>` là đoạn sau `/project/` trên thanh địa chỉ. Lấy key: vào **Settings → API Keys**, copy **Publishable key** (`sb_publishable_...`).
2. Thêm 2 dòng sau vào `ExpenseTrackerApp/local.properties`. File này đã được gitignore nên key không bị đẩy lên GitHub.
   ```properties
   SUPABASE_URL=https://xxxxxxxx.supabase.co
   SUPABASE_ANON_KEY=eyJhbGciOi...   # hoặc sb_publishable_...
   ```
3. Bấm **Sync Project with Gradle Files** rồi chạy lại app.

Nếu thiếu 2 dòng trên, app vẫn chạy nhưng khi đăng nhập sẽ báo *"Chưa cấu hình Supabase"*.

> Không bao giờ dùng **service_role / secret key** trong app. Key đó có toàn quyền với database.

## 🔄 Sao lưu & đồng bộ (Sync)

Cần chạy [`supabase/sync_schema.sql`](supabase/sync_schema.sql) một lần (bước 4 ở mục trên).

- **Khách** dùng offline bình thường, dữ liệu chỉ nằm trên máy. **Đăng nhập** để sao lưu và dùng trên nhiều máy.
- **Khi nào đồng bộ:** vài giây sau mỗi lần thêm / sửa / xóa, khi máy có mạng (kể cả lúc mở app), mỗi 6 giờ, và khi bấm *Đồng bộ ngay* ở trang Tài khoản.
- **Cách hoạt động:** mỗi dòng có `uuid`, `updatedAt` và cờ `deleted` (xóa mềm). App đẩy các dòng `PENDING` lên (ví → danh mục → giao dịch), rồi kéo về các dòng đổi trên server sau lần kéo trước (`synced_at`). Hai máy cùng sửa một dòng thì **bản sửa mới hơn thắng**. Số dư ví được tính từ giao dịch, nên không bao giờ lệch giữa các máy.
- **Đăng nhập** khi máy đang có chi tiêu của khách: app hỏi *gộp vào tài khoản* hay *xóa khỏi máy*.
- **Đăng xuất** xóa dữ liệu trên máy; nếu còn thay đổi chưa sao lưu, app cảnh báo và cho đồng bộ trước.
- **Chưa có:** đồng bộ ảnh hóa đơn (ảnh chỉ nằm trên máy và mất khi đăng xuất). Xung đột so theo đồng hồ của máy.

Code: `SyncDataUseCase` (`:core:domain`), `SyncLocalImpl` (`:core:database`), `SupabaseSyncRemote` (`:core:network`), `SyncWorker` / `WorkManagerSyncScheduler` (`:app`).

## 🚀 Kế hoạch phát triển (Development Phases)

Dự án được triển khai qua 5 giai đoạn:
1. **Foundation & Setup**: Khởi tạo project, cài đặt dependencies, setup MVVM và thiết kế XML cơ bản.
2. **Local Storage & Camera**: Cấu hình Room Database, viết các DAO; tích hợp CameraX để thực hiện tác vụ chụp và lưu ảnh offline.
3. **Deferred Syncing & Background Tasks** *(đã xong)*: Đồng bộ theo tài khoản với Supabase qua WorkManager, chạy lại khi `NetworkChangeReceiver` báo có mạng.
4. **UI/UX Flow & Data Display**: Hoàn thiện tính năng xem lịch sử với LiveData để binding dữ liệu ra UI.
5. **AI Integration & Polish**: Gọi API AI để lấy tư vấn, tối ưu hóa tốc độ khởi động camera, dọn dẹp code và hoàn thiện thiết kế UI/UX.

---
*(File README.md này được thiết kế để cung cấp cái nhìn tổng quan nhất cho cả Developer và AI Assistant tham gia vào quá trình xây dựng dự án Expense Tracker).*
