# Expense Tracker - Development Plan

As a senior Android developer and system architect, I have designed this development plan to meet the requirements of the Mobile Application Development (NT118) course. This plan focuses on a lightweight, offline-first native Android application using Java and standard Android components.

## 1. Project Architecture & Component Mapping

The application will follow the Model-View-ViewModel (MVVM) architecture pattern, ensuring separation of concerns and robust state management.

*   **Activity**: 
    *   `MainActivity`: A single-activity architecture hosting all Fragments. It will handle top-level routing, runtime permissions (Camera, Storage), and global UI elements like the Bottom Navigation Bar.
*   **Fragment**:
    *   `CameraHostFragment`: The central hub (horizontal ViewPager2 index 1) hosting a vertical ViewPager2.
    *   `AddExpenseFragment`: The camera view (vertical ViewPager2 index 0) containing CameraX preview and Locket-style UI.
    *   `DashboardFragment`: The left navigation hub (horizontal ViewPager2 index 0) containing an internal BottomNavigationView.
    *   `DashboardOverviewFragment`: The dashboard tab displaying a summary of expenses (PieChart).
    *   `PhotosFragment`: The right screen (horizontal ViewPager2 index 2) showing a grid view of all past receipts.
    *   `HistoryFragment`: The bank-style ledger tab inside DashboardFragment.
    *   `BudgetFragment`: The Wallets tab inside DashboardFragment.
    *   `SettingsFragment`: Application settings (accessible from Camera top header).
    *   `AiAssistantFragment`: The chat interface tab inside DashboardFragment (and accessible from Camera top header).
*   **Service**: 
    *   `SyncService` (JobIntentService or foreground Service): Responsible for background data synchronization. It will process pending expense records and upload them to a remote server (if applicable) when conditions are met.
*   **Broadcast Receiver**: 
    *   `NetworkChangeReceiver`: Listens for `CONNECTIVITY_ACTION`. When network access is restored, it triggers the `SyncService` to process the deferred offline sync queue.

## 2. UI/UX Flow (Locket-Style Camera-First Architecture)

The design will be highly immersive and minimalist, bringing the Camera to the forefront as the main entry point to reduce friction.

*   **Main Navigation (2D ViewPager2)**: 
    *   **Horizontal Axis**: A `ViewPager2` in `MainActivity` with 3 main screens:
        *   **Left (Index 0)**: `DashboardFragment` (The Management Hub)
        *   **Center (Index 1 - Default)**: `CameraHostFragment` (The Core Input)
        *   **Right (Index 2)**: `PhotosFragment` (The Receipt Grid)
    *   **Vertical Axis**: Swiping UP from `CameraHostFragment` will transition to a vertical `ViewPager2` showing individual past receipts in full screen (TikTok/Reels style).

*   **Center Screen (CameraHostFragment)**: 
    *   Full-screen CameraX `PreviewView`.
    *   **Top Overlay**: Settings icon (Left), Total Wallet Balance Dropdown (Center), AI Assistant Chat icon (Right).
    *   **Bottom Overlay**: A large orange (`@color/colorAccent`) capture button in the center, Flash toggle (Left), Flip Camera (Right).
    *   *Flow*: Snap -> Input Amount -> Save -> Form resets immediately for the next capture.

*   **Left Screen (DashboardFragment)**: 
    *   Contains its own internal `BottomNavigationView` with 4 tabs: **Dashboard** (PieChart), **Wallets**, **History** (Bank-style +/- ledger), and **AI Assistant**.
    *   A prominent orange FAB (`+`) to quickly Add a New Wallet.

*   **Right Screen (PhotosFragment)**: 
    *   A grid view of all past receipts/photos.

    > **Design tokens**: Dark mode palette `#0A0A0A` (background) – `#FF5722` (accent). Typography uses **Inter** or **Roboto**.

## 3. Local Database & State Management Strategy

*   **Local Database**: We will use **Room Persistence Library** as an abstraction layer over SQLite for robust and safe local storage.
    *   **Entity**: `ExpenseRecord` (id, amount, category, imageUri, timestamp, syncStatus).
    *   `syncStatus` will be an enum (`PENDING`, `SYNCED`) to manage the deferred sync logic.
    *   **DAO Methods** (suggested):
        * `insertExpense(ExpenseRecord expense)`
        * `getAllExpenses(): LiveData<List<ExpenseRecord>>`
        * `getPendingExpenses(): List<ExpenseRecord>`
        * `updateSyncStatus(id: Long, status: SyncStatus)`
        * `deleteExpense(id: Long)`
    *   Images will not be saved in the database as BLOBs; instead, they will be saved to the app's internal file storage, and only the local file URI (`imageUri`) will be stored in Room.
*   **State Management**: 
    *   **ViewModel**: Each Fragment will have a dedicated ViewModel (e.g., `AddExpenseViewModel`) to hold UI data and survive configuration changes (like screen rotations).
    *   **LiveData**: ViewModels will expose data to the XML UI via `LiveData`. The UI will observe these LiveData objects and update automatically when the underlying database changes.
    *   **Repository Pattern**: A central `ExpenseRepository` will act as the single source of truth, mediating between the Room DAO and the remote API/SyncService.

## 4. API/AI Integration Strategy

*   **Deferred/Offline Sync**:
    1.  When a user saves an expense, it is immediately written to the Room database with `syncStatus = PENDING`.
    2.  If the device is offline, the operation is complete from the user's perspective (instant feedback).
    3.  When the `NetworkChangeReceiver` detects internet connectivity, it wakes up the `SyncService`.
    4.  The Service queries Room for all `PENDING` records and attempts to upload them via a REST API (using **Retrofit**).
    5.  On success, the status is updated to `SYNCED`.
*   **AI Budgeting Assistant**:
    *   We will integrate a lightweight cloud AI API (e.g., Gemini API via HTTP).
    *   **Endpoint**: `POST https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent`
    *   **Payload**: `{ "model": "gemini-pro", "prompt": "Give budgeting advice based on the following expense summary: {json}" }`
    *   **Authentication**: API key stored in `local.properties` (retrieved at runtime, never committed).
    *   **Error handling**: Retry with exponential back‑off up to 3 attempts; fallback to static advice if API unavailable.
    *   The `AiAssistantFragment` will trigger a request to the `ExpenseRepository` to fetch an aggregated summary of the month's expenses (e.g., total spent per category).
    *   This JSON payload is sent to the AI API with a prompt asking for budgeting advice.
    *   The response is parsed and displayed on the screen. Network calls will be made asynchronously using Java's `ExecutorService` or Kotlin Coroutines (if permitted, otherwise standard callbacks/RxJava).

## 5. Step-by-Step Development Phases

### Phase 1: Foundation & Setup [DONE]
*   [x] Initialize Android Studio project (Java).
*   [x] Configure `build.gradle` with dependencies (Room, Retrofit, CameraX, Glide, MPAndroidChart).
*   [x] Create the core XML layouts (`activity_main.xml`, `fragment_dashboard.xml`, etc.).
*   [x] Initialize Git repository with proper `.gitignore`.
*   [x] Add basic app theme (`styles.xml`) with dark‑mode colors.
*   [x] Configure AndroidManifest with required permissions (CAMERA, INTERNET, READ/WRITE_STORAGE).

### Phase 2: Refactoring Architecture (Locket-style UX) (Current Task)
*   Implement `ViewPager2` in `MainActivity` with horizontal paging for `DashboardFragment`, `CameraHostFragment`, and `PhotosFragment`.
*   Update `activity_main.xml` to remove the global `BottomNavigationView` and `FloatingActionButton`.
*   Refactor `DashboardFragment` (formerly `HomeFragment`) to contain its own internal `BottomNavigationView` (Dashboard, Wallets, History, AI Assistant) and an orange `+` FAB for creating wallets.
*   Create `CameraHostFragment` hosting a vertical `ViewPager2`. Index 0 is the camera (`AddExpenseFragment`), and scrolling up reveals past receipts (`ReceiptDetailFragment`).
*   Redesign `AddExpenseFragment` with the Locket-style Capture UI: Full-screen CameraX, Top Header (Settings, Balance, AI Chat), and Bottom Overlay (Orange Capture Button, Flash, Flip).

### Phase 3: Deferred Syncing & Background Tasks
*   Implement `NetworkChangeReceiver` and `SyncService`.
*   Set up Retrofit for API communication (if a mock backend is used, otherwise simulate the upload process).
*   Implement the background queueing logic to handle `PENDING` to `SYNCED` state transitions.

### Phase 4: UI/UX Flow & Data Display
*   Complete the `PhotosFragment` using `RecyclerView` and `Glide` to load the captured thumbnails efficiently.
*   Implement the ViewModel and LiveData observation to ensure the UI updates instantly when a new expense is logged.

### Phase 5: AI Integration & Polish
*   Implement the AI API integration in the `AiAssistantFragment` / Chat Bubble.
*   Conduct performance profiling: Ensure camera startup is instantaneous and UI does not drop frames.
*   Finalize styling, colors, and minimalist aesthetic in `colors.xml` and `styles.xml`.

### Phase 6: Testing, CI & Polish
*   Write unit tests for DAO and ViewModel using JUnit4 & Mockito.
*   Write UI tests for Fragments using Espresso.
*   Configure GitHub Actions workflow.
*   Perform accessibility audit (TalkBack, contentDescription).
*   Final code review and documentation update.
