# Quy tắc lập trình cho Expense Tracker (Enterprise & High-Grade Guidelines)

## 1. Mục tiêu Cốt lõi
Dự án này là đồ án đại học nhưng **PHẢI ĐƯỢC THIẾT KẾ THEO CHUẨN DOANH NGHIỆP TỐI ĐA** nhằm mục đích đạt điểm tuyệt đối. Bất kỳ sự thỏa hiệp nào về kiến trúc (như bỏ qua UseCase hay DI) đều không được phép.

## 2. Tech Stack (Công nghệ sử dụng)
- **Ngôn ngữ:** Java (Java 17 compatibility). TUYỆT ĐỐI KHÔNG SỬ DỤNG KOTLIN (theo yêu cầu đặc thù của đồ án).
- **UI:** XML Layouts, thiết kế minimalist, zero-lag.
- **Database:** Room Persistence Library.
- **Networking:** Retrofit.
- **Xử lý nền:** `Service` / `JobIntentService` + `BroadcastReceiver` cho kiến trúc Offline-first.

## 3. Kiến trúc Enterprise (Clean Architecture)
- **Kiến trúc 3 Lớp (Strict Clean Architecture):** BẮT BUỘC phải chia rõ ràng:
  - `Data Layer`: Chứa Repositories Impl, Room DAOs, Retrofit API, Entities.
  - `Domain Layer`: **BẮT BUỘC PHẢI CÓ** các `UseCase` (Interactors) cho mọi tác vụ, kể cả CRUD cơ bản (ví dụ: `InsertExpenseUseCase`). Lớp này chỉ chứa interface của Repository, tuyệt đối không chứa Android Framework dependencies.
  - `Presentation Layer`: ViewModels, Fragments/Activities, State classes.
- **Single-Activity Architecture:** Dùng 1 `MainActivity` duy nhất làm host cho Navigation Component.
- **Package Structure:** Tổ chức theo Layer (`data`, `domain`, `presentation`) rồi bên trong chia theo Feature, hoặc chia theo Feature rồi chia Layer. Phải giữ cấu trúc siêu gọn gàng.

## 4. OOP, SOLID & Design Principles (Khắt khe)
- **Tuân thủ tuyệt đối OOP:**
  - *Đóng gói:* Tất cả biến nội bộ là `private`. Sử dụng `getter/setter`. Trạng thái của class không được rò rỉ ra ngoài.
  - *Đa hình & Kế thừa:* Áp dụng Base classes cho ViewModels/Fragments nếu có logic lặp lại.
  - *Trừu tượng:* Mọi giao tiếp giữa các Layer (đặc biệt từ Domain xuống Data) **BẮT BUỘC** thông qua Interface (Dependency Inversion Principle).
- **SOLID:** Vi phạm bất kỳ nguyên lý SOLID nào (như tạo God Object) sẽ bị đánh giá là code lỗi.
- **Dependency Injection (DI):** **BẮT BUỘC** sử dụng thư viện DI chuẩn công nghiệp (khuyến khích dùng **Dagger/Hilt** cho Java). Tuyệt đối cấm sử dụng từ khóa `new` để khởi tạo UseCase, Repository, hay ViewModel bên trong UI.

## 5. Coding Conventions, Quality & Error Handling
- **Tên biến/Hàm:** Rõ ràng, mang tính tự định nghĩa (self-documenting). Không viết tắt.
- **Error Handling:** Tránh crash bằng mọi giá. Các layer giao tiếp với nhau bằng pattern `Result<T>` (Success, Error, Loading). Bắt toàn bộ exception (IOException, SQLException) ở tầng Data và chuyển hóa thành Error State ở tầng UI.
- **Logging:** Sử dụng `Log.d`, `Log.e` với tag rõ ràng. Không dùng `System.out.print`.

## 6. Enterprise Testing Standard
- **Unit Tests (BẮT BUỘC):** Phải viết Unit Test bằng **JUnit4** và **Mockito**.
- Bắt buộc phải có bài test cho toàn bộ logic tại tầng `Domain Layer` (Các UseCases) và `Presentation Layer` (Các ViewModels). Code không có test là code không đạt chuẩn doanh nghiệp.

## 7. Quy trình làm việc bắt buộc đối với AI Agent
- Đọc kỹ file này và `plan.md` trước khi thao tác.
- **KHÔNG ĐƯỢC "ĐI TẮT":** Không được bỏ qua UseCase hay Interface chỉ vì task đơn giản. Phải giữ đúng boilerplate của Clean Architecture để mô phỏng dự án lớn.
- **Quy tắc "Chia để trị" (Divide & Conquer):** KHÔNG gộp quá nhiều logic phức tạp vào một lần phản hồi (response) duy nhất để tránh tràn bộ nhớ ngữ cảnh và sinh ảo giác. AI cần chủ động chia nhỏ tính năng thành nhiều lượt tương tác ngắn gọn.
- **Quy trình Gỡ lỗi / Code tính năng mới (3 Bước Độc Lập):** Tuyệt đối KHÔNG tự ý vội vàng lao vào viết hay sửa code ngay lập tức. AI PHẢI tuân thủ nghiêm ngặt chu trình sau qua nhiều lượt chat:
  1. *Phân tích:* Xác định nguyên nhân cốt lõi của lỗi hoặc phân tích yêu cầu nghiệp vụ.
  2. *Lên kế hoạch:* Đề xuất phương án giải quyết (Tạo file Implementation Plan) và **DỪNG LẠI** chờ người dùng phê duyệt (Approve).
  3. *Thực thi:* Chỉ tiến hành sửa đổi code sau khi người dùng đã chốt phương án ở bước 2. Tuyệt đối không làm gộp cả 3 bước trong cùng 1 lần chat.
- **Cập nhật tài liệu (Single Source of Truth):** Tự động cập nhật tiến độ vào file `PROJECT_CONTEXT.md` ngay khi xong task. Nếu quá trình thực thi có phát sinh thay đổi so với thiết kế ban đầu, AI PHẢI tự động cập nhật lại `plan.md` để đồng bộ code và tài liệu.
