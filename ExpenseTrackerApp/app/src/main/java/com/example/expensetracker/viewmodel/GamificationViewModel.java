package com.example.expensetracker.viewmodel;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import com.example.expensetracker.core.domain.usecase.GetAllTransactionsUseCase;
import com.example.expensetracker.core.domain.usecase.SendAiChatPromptUseCase;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.LongSupplier;

import javax.inject.Inject;

/**
 * ViewModel driving the Virtual Piggy Pet state, Saving Leaderboard, and Gemini AI Piggy Assistant.
 */
public class GamificationViewModel extends ViewModel {

    public enum Mood {
        HAPPY,
        WORRIED,
        SAD
    }

    public enum EvolutionStage {
        BABY,
        STUDENT,
        ADULT,
        ROYAL
    }

    public enum RankingMetric {
        STREAK,
        TOTAL_SAFE_DAYS
    }

    public enum RankingScope {
        GLOBAL,
        FRIENDS
    }

    public static final double DEFAULT_DAILY_BUDGET_LIMIT = 100_000.0;
    public static final double HAPPY_RATIO = 0.80;
    public static final String KEY_DAILY_BUDGET_LIMIT = "gamification_daily_limit";
    public static final String GUEST_DISPLAY_NAME = "Bạn (Khách)";
    public static final String GUEST_HANDLE = "@guest";
    private static final int MAX_LOOKBACK_DAYS = 3650;

    public static final class PiggyUiState {
        public final double dailySpent;
        public final double dailyLimit;
        public final int streakDays;
        public final int totalSafeDays;
        public final Mood mood;
        public final EvolutionStage stage;
        public final String speechBubbleText;
        public final int budgetProgressPercent;

        public PiggyUiState(double dailySpent,
                            double dailyLimit,
                            int streakDays,
                            int totalSafeDays,
                            Mood mood,
                            EvolutionStage stage,
                            String speechBubbleText) {
            this.dailySpent = dailySpent;
            this.dailyLimit = dailyLimit;
            this.streakDays = streakDays;
            this.totalSafeDays = totalSafeDays;
            this.mood = mood;
            this.stage = stage;
            this.speechBubbleText = speechBubbleText;
            this.budgetProgressPercent = dailyLimit > 0
                    ? Math.min(100, (int) Math.round((dailySpent / dailyLimit) * 100))
                    : 0;
        }
    }

    public static final class LeaderboardEntry {
        public final int rank;
        public final String displayName;
        public final String username;
        public final int streakDays;
        public final int totalSafeDays;
        public final EvolutionStage stage;
        public final boolean isCurrentUser;
        public final boolean isFriend;

        public LeaderboardEntry(int rank,
                                String displayName,
                                String username,
                                int streakDays,
                                int totalSafeDays,
                                EvolutionStage stage,
                                boolean isCurrentUser,
                                boolean isFriend) {
            this.rank = rank;
            this.displayName = displayName;
            this.username = username;
            this.streakDays = streakDays;
            this.totalSafeDays = totalSafeDays;
            this.stage = stage;
            this.isCurrentUser = isCurrentUser;
            this.isFriend = isFriend;
        }
    }

    public static final class ChatMessage {
        public final String text;
        public final boolean isUser;
        public final long timestamp;

        public ChatMessage(String text, boolean isUser, long timestamp) {
            this.text = text;
            this.isUser = isUser;
            this.timestamp = timestamp;
        }
    }

    private static final class FinancialSnapshot {
        final double dailySpent;
        final double dailyLimit;
        final int streakDays;
        final int totalSafeDays;
        final Mood mood;
        final EvolutionStage stage;
        final double monthlyExpense;
        final double monthlyIncome;
        final String topCategoryName;
        final double topCategoryAmount;

        FinancialSnapshot(double dailySpent,
                          double dailyLimit,
                          int streakDays,
                          int totalSafeDays,
                          Mood mood,
                          EvolutionStage stage,
                          double monthlyExpense,
                          double monthlyIncome,
                          String topCategoryName,
                          double topCategoryAmount) {
            this.dailySpent = dailySpent;
            this.dailyLimit = dailyLimit;
            this.streakDays = streakDays;
            this.totalSafeDays = totalSafeDays;
            this.mood = mood;
            this.stage = stage;
            this.monthlyExpense = monthlyExpense;
            this.monthlyIncome = monthlyIncome;
            this.topCategoryName = topCategoryName;
            this.topCategoryAmount = topCategoryAmount;
        }
    }

    private static final String[] HAPPY_SPEECHES = {
            "Ủn ỉn! Hôm nay bạn chi tiêu siêu kỷ luật, bụng mình ấm no rồi! 🐷✨",
            "Tuyệt vời! Bạn vẫn còn dư dả hạn mức ngày, cứ thế phát huy nhé!",
            "Giữ vững chuỗi Streak này để mình sớm lên cấp Hoàng Gia nha!"
    };

    private static final String[] WORRIED_SPEECHES = {
            "Ối! Hôm nay tiêu hơn 80% hạn mức ngày rồi, phanh lại chút bạn ơi! 😅",
            "Cẩn thận nha! Chỉ còn một khoản nhỏ nữa là đứt chuỗi Streak đó!",
            "Hãy cân nhắc kỹ trước khi rút ví chiều tối nay nhé!"
    };

    private static final String[] SAD_SPEECHES = {
            "Hu hu... Hôm nay vượt hạn mức ngày mất rồi, mình đói meo! 😭",
            "Vung tay quá trán rồi! Ngày mai chúng mình cùng làm lại từ đầu nhé!",
            "Hãy kiểm tra lại mục Lịch sử xem khoản nào có thể cắt giảm vào ngày mai nha!"
    };

    private final GetAllTransactionsUseCase getAllTransactionsUseCase;
    private final IAuthRepository authRepository;
    private final SharedPreferences prefs;
    private final SendAiChatPromptUseCase sendAiChatPromptUseCase;
    private final Executor executor;
    private final LongSupplier clock;
    private final LiveData<List<TransactionModel>> transactionsSource;
    private final Observer<List<TransactionModel>> transactionsObserver = this::onTransactionsUpdated;

    private final MutableLiveData<PiggyUiState> piggyState = new MutableLiveData<>();
    private final MutableLiveData<List<LeaderboardEntry>> leaderboardEntries = new MutableLiveData<>();
    private final MutableLiveData<RankingMetric> currentMetric = new MutableLiveData<>(RankingMetric.STREAK);
    private final MutableLiveData<RankingScope> currentScope = new MutableLiveData<>(RankingScope.GLOBAL);
    private final MutableLiveData<List<ChatMessage>> chatMessages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isAiTyping = new MutableLiveData<>(false);

    private final List<LeaderboardEntry> mockPool = new ArrayList<>();
    private final List<ChatMessage> messageHistory = new ArrayList<>();
    private final Object chatLock = new Object();

    private List<TransactionModel> latestTransactions = Collections.emptyList();
    private boolean hasUserSentChat = false;

    private double dailyLimit = DEFAULT_DAILY_BUDGET_LIMIT;
    private double dailySpent = 0.0;
    private int streakDays = 0;
    private int totalSafeDays = 0;
    private String currentUserDisplayName = GUEST_DISPLAY_NAME;
    private String currentUserHandle = GUEST_HANDLE;
    private int speechIndex = 0;
    private RankingMetric metric = RankingMetric.STREAK;
    private RankingScope scope = RankingScope.GLOBAL;

    @Inject
    public GamificationViewModel(GetAllTransactionsUseCase getAllTransactionsUseCase,
                                 IAuthRepository authRepository,
                                 SharedPreferences prefs,
                                 SendAiChatPromptUseCase sendAiChatPromptUseCase,
                                 ExecutorService executorService) {
        this(getAllTransactionsUseCase, authRepository, prefs, sendAiChatPromptUseCase, executorService, System::currentTimeMillis);
    }

    GamificationViewModel(GetAllTransactionsUseCase getAllTransactionsUseCase,
                          IAuthRepository authRepository,
                          SharedPreferences prefs,
                          SendAiChatPromptUseCase sendAiChatPromptUseCase,
                          Executor executor,
                          LongSupplier clock) {
        this.getAllTransactionsUseCase = getAllTransactionsUseCase;
        this.authRepository = authRepository;
        this.prefs = prefs;
        this.sendAiChatPromptUseCase = sendAiChatPromptUseCase;
        this.executor = executor != null ? executor : Runnable::run;
        this.clock = clock != null ? clock : System::currentTimeMillis;

        if (prefs != null && prefs.contains(KEY_DAILY_BUDGET_LIMIT)) {
            long bits = prefs.getLong(KEY_DAILY_BUDGET_LIMIT, Double.doubleToRawLongBits(DEFAULT_DAILY_BUDGET_LIMIT));
            double loaded = Double.longBitsToDouble(bits);
            if (loaded > 0) {
                this.dailyLimit = loaded;
            }
        }

        refreshUserIdentityInternal();
        initMockLeaderboard();
        publishPiggyState();
        publishLeaderboard();
        seedOrRefreshGreeting();

        this.transactionsSource = getAllTransactionsUseCase != null ? getAllTransactionsUseCase.execute() : null;
        if (this.transactionsSource != null) {
            this.transactionsSource.observeForever(transactionsObserver);
        }
    }

    @Override
    protected void onCleared() {
        if (transactionsSource != null) {
            transactionsSource.removeObserver(transactionsObserver);
        }
        super.onCleared();
    }

    public static Mood computeMood(double dailySpent) {
        return computeMood(dailySpent, DEFAULT_DAILY_BUDGET_LIMIT);
    }

    public static Mood computeMood(double dailySpent, double dailyLimit) {
        double safeLimit = dailyLimit > 0 ? dailyLimit : DEFAULT_DAILY_BUDGET_LIMIT;
        if (dailySpent <= safeLimit * HAPPY_RATIO) {
            return Mood.HAPPY;
        }
        if (dailySpent <= safeLimit) {
            return Mood.WORRIED;
        }
        return Mood.SAD;
    }

    public static EvolutionStage computeStage(int streakDays) {
        if (streakDays < 7) {
            return EvolutionStage.BABY;
        }
        if (streakDays < 21) {
            return EvolutionStage.STUDENT;
        }
        if (streakDays < 50) {
            return EvolutionStage.ADULT;
        }
        return EvolutionStage.ROYAL;
    }

    public LiveData<PiggyUiState> getPiggyState() {
        return piggyState;
    }

    public LiveData<List<LeaderboardEntry>> getLeaderboardEntries() {
        return leaderboardEntries;
    }

    public LiveData<RankingMetric> getCurrentMetric() {
        return currentMetric;
    }

    public LiveData<RankingScope> getCurrentScope() {
        return currentScope;
    }

    public LiveData<List<ChatMessage>> getChatMessages() {
        return chatMessages;
    }

    public LiveData<Boolean> getIsAiTyping() {
        return isAiTyping;
    }

    public void onPiggyTapped() {
        String[] speeches = speechesFor(computeMood(dailySpent, dailyLimit));
        speechIndex = (speechIndex + 1) % speeches.length;
        publishPiggyState();
    }

    public void setDailyBudgetLimit(double newLimit) {
        if (newLimit <= 0) {
            return;
        }
        this.dailyLimit = newLimit;
        if (prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            if (editor != null) {
                editor.putLong(KEY_DAILY_BUDGET_LIMIT, Double.doubleToRawLongBits(newLimit)).apply();
            }
        }
        this.speechIndex = 0;
        recalculateFromTransactions();
    }

    public void refreshUserIdentity() {
        refreshUserIdentityInternal();
        updateCurrentUserInMockPool();
        publishLeaderboard();
    }

    public void setRankingMetric(RankingMetric newMetric) {
        if (newMetric == null || this.metric == newMetric) {
            return;
        }
        this.metric = newMetric;
        currentMetric.setValue(newMetric);
        publishLeaderboard();
    }

    public void setRankingScope(RankingScope newScope) {
        if (newScope == null || this.scope == newScope) {
            return;
        }
        this.scope = newScope;
        currentScope.setValue(newScope);
        publishLeaderboard();
    }

    public void sendChatMessage(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return;
        }
        String query = rawInput.trim();
        hasUserSentChat = true;

        synchronized (chatLock) {
            messageHistory.add(new ChatMessage(query, true, clock.getAsLong()));
            chatMessages.postValue(new ArrayList<>(messageHistory));
        }

        FinancialSnapshot snapshot = buildFinancialSnapshot();
        String systemInstruction = buildSystemInstruction(snapshot);
        String fallbackReply = buildLocalPiggyReply(query, snapshot);

        isAiTyping.postValue(true);

        executor.execute(() -> {
            String reply = fallbackReply;
            if (sendAiChatPromptUseCase != null && sendAiChatPromptUseCase.isConfigured()) {
                Result<String> result = sendAiChatPromptUseCase.execute(systemInstruction, query);
                if (result != null && result.isSuccess()
                        && result.getData() != null
                        && !result.getData().trim().isEmpty()) {
                    reply = result.getData().trim();
                }
            }
            synchronized (chatLock) {
                messageHistory.add(new ChatMessage(reply, false, clock.getAsLong()));
                chatMessages.postValue(new ArrayList<>(messageHistory));
            }
            isAiTyping.postValue(false);
        });
    }

    private void onTransactionsUpdated(List<TransactionModel> transactions) {
        latestTransactions = transactions != null ? transactions : Collections.emptyList();
        recalculateFromTransactions();
    }

    private void recalculateFromTransactions() {
        long todayStart = startOfDayMillis(clock.getAsLong());
        Map<Long, Double> expenseByDay = new HashMap<>();
        long earliestDayStart = Long.MAX_VALUE;

        for (TransactionModel t : latestTransactions) {
            if (t == null || t.isDeleted() || t.getTimestamp() == null) {
                continue;
            }
            long dayStart = startOfDayMillis(t.getTimestamp().getTime());
            if (dayStart > todayStart) {
                continue;
            }
            if (dayStart < earliestDayStart) {
                earliestDayStart = dayStart;
            }
            if (t.getType() == null || "EXPENSE".equalsIgnoreCase(t.getType())) {
                double current = expenseByDay.getOrDefault(dayStart, 0.0);
                expenseByDay.put(dayStart, current + t.getAmount());
            }
        }

        this.dailySpent = expenseByDay.getOrDefault(todayStart, 0.0);

        if (earliestDayStart == Long.MAX_VALUE) {
            this.streakDays = 0;
            this.totalSafeDays = 0;
        } else {
            Calendar minCal = Calendar.getInstance();
            minCal.setTimeInMillis(todayStart);
            minCal.add(Calendar.DAY_OF_YEAR, -MAX_LOOKBACK_DAYS);
            long minAllowedStart = startOfDayMillis(minCal.getTimeInMillis());
            if (earliestDayStart < minAllowedStart) {
                earliestDayStart = minAllowedStart;
            }

            int streak = 0;
            int safeDays = 0;
            Calendar cursor = Calendar.getInstance();
            cursor.setTimeInMillis(earliestDayStart);

            while (true) {
                long dayKey = startOfDayMillis(cursor.getTimeInMillis());
                if (dayKey > todayStart) {
                    break;
                }
                double spent = expenseByDay.getOrDefault(dayKey, 0.0);
                if (spent <= dailyLimit) {
                    streak++;
                    safeDays++;
                } else {
                    streak = 0;
                }
                cursor.add(Calendar.DAY_OF_YEAR, 1);
            }

            this.streakDays = streak;
            this.totalSafeDays = safeDays;
        }

        updateCurrentUserInMockPool();
        publishPiggyState();
        publishLeaderboard();
        if (!hasUserSentChat) {
            seedOrRefreshGreeting();
        }
    }

    private void seedOrRefreshGreeting() {
        Mood mood = computeMood(dailySpent, dailyLimit);
        String moodText;
        switch (mood) {
            case WORRIED:
                moodText = "Mình đang hơi lo lắng vì hôm nay bạn tiêu " + formatCurrency(dailySpent)
                        + " / " + formatCurrency(dailyLimit) + " rồi đó 😅.";
                break;
            case SAD:
                moodText = "Hu hu, hôm nay bạn tiêu " + formatCurrency(dailySpent)
                        + ", vượt mất hạn mức " + formatCurrency(dailyLimit) + " rồi 😭.";
                break;
            case HAPPY:
            default:
                moodText = "Ủn ỉn chào bạn! Hôm nay bạn mới tiêu " + formatCurrency(dailySpent)
                        + " / " + formatCurrency(dailyLimit) + ", giữ Streak " + streakDays + " ngày đỉnh lắm! 🐷✨";
                break;
        }
        String greeting = moodText + " Hỏi mình bất cứ điều gì hoặc chọn gợi ý bên dưới nhé!";
        synchronized (chatLock) {
            ChatMessage greetingMsg = new ChatMessage(greeting, false, clock.getAsLong());
            if (messageHistory.isEmpty()) {
                messageHistory.add(greetingMsg);
            } else if (!messageHistory.get(0).isUser) {
                messageHistory.set(0, greetingMsg);
            }
            chatMessages.postValue(new ArrayList<>(messageHistory));
        }
    }

    private FinancialSnapshot buildFinancialSnapshot() {
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(clock.getAsLong());
        int currentYear = nowCal.get(Calendar.YEAR);
        int currentMonth = nowCal.get(Calendar.MONTH);

        double monthlyExpense = 0.0;
        double monthlyIncome = 0.0;
        Map<String, Double> categorySums = new HashMap<>();

        Calendar txCal = Calendar.getInstance();
        for (TransactionModel t : latestTransactions) {
            if (t == null || t.isDeleted() || t.getTimestamp() == null) {
                continue;
            }
            txCal.setTimeInMillis(t.getTimestamp().getTime());
            if (txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonth) {
                if ("INCOME".equalsIgnoreCase(t.getType())) {
                    monthlyIncome += t.getAmount();
                } else {
                    monthlyExpense += t.getAmount();
                    String cat = (t.getCategory() != null && !t.getCategory().trim().isEmpty())
                            ? t.getCategory().trim()
                            : "Khác";
                    categorySums.put(cat, categorySums.getOrDefault(cat, 0.0) + t.getAmount());
                }
            }
        }

        String topCategoryName = "Chưa có";
        double topCategoryAmount = 0.0;
        for (Map.Entry<String, Double> entry : categorySums.entrySet()) {
            if (entry.getValue() > topCategoryAmount) {
                topCategoryAmount = entry.getValue();
                topCategoryName = entry.getKey();
            }
        }

        return new FinancialSnapshot(
                dailySpent,
                dailyLimit,
                streakDays,
                totalSafeDays,
                computeMood(dailySpent, dailyLimit),
                computeStage(streakDays),
                monthlyExpense,
                monthlyIncome,
                topCategoryName,
                topCategoryAmount
        );
    }

    private static String buildSystemInstruction(FinancialSnapshot s) {
        return "Bạn là Heo Đất AI — trợ lý tài chính cá nhân thông minh, dí dỏm và tận tâm trong ứng dụng Expense Tracker. "
                + "Hãy trả lời ngắn gọn (tối đa 4 câu), thân thiện bằng tiếng Việt (hoặc theo ngôn ngữ câu hỏi) dựa trên số liệu thật sau đây của người dùng:\n"
                + "- Chi tiêu hôm nay: " + formatCurrency(s.dailySpent) + " / Hạn mức ngày: " + formatCurrency(s.dailyLimit) + "\n"
                + "- Tâm trạng Heo: " + s.mood.name() + ", Cấp tiến hóa: " + s.stage.name() + "\n"
                + "- Chuỗi Streak hiện tại: " + s.streakDays + " ngày liên tục, Tổng ngày đạt chuẩn: " + s.totalSafeDays + " ngày\n"
                + "- Tổng chi tiêu tháng này: " + formatCurrency(s.monthlyExpense) + ", Tổng thu nhập tháng này: " + formatCurrency(s.monthlyIncome) + "\n"
                + "- Danh mục chi nhiều nhất tháng: " + s.topCategoryName + " (" + formatCurrency(s.topCategoryAmount) + ")";
    }

    static String buildLocalPiggyReply(String query, FinancialSnapshot s) {
        String q = query.toLowerCase(Locale.ROOT);
        double remainingToday = s.dailyLimit - s.dailySpent;

        if (q.contains("hôm nay") || q.contains("today")) {
            if (remainingToday >= 0) {
                return "📊 Hôm nay bạn đã chi " + formatCurrency(s.dailySpent)
                        + " trên hạn mức " + formatCurrency(s.dailyLimit) + ". "
                        + "Bạn vẫn còn dư " + formatCurrency(remainingToday)
                        + " để giữ an toàn chuỗi Streak " + s.streakDays + " ngày nhé! 🐷";
            } else {
                return "📊 Hôm nay bạn đã chi " + formatCurrency(s.dailySpent)
                        + ", vượt hạn mức " + formatCurrency(Math.abs(remainingToday)) + ". "
                        + "Hạn chế rút ví thêm tối nay và ngày mai khởi động lại chuỗi mới cùng mình nha! 💪";
            }
        }

        if (q.contains("danh mục") || q.contains("tốn") || q.contains("top") || q.contains("category")) {
            if (s.topCategoryAmount <= 0) {
                return "🔥 Tháng này bạn chưa phát sinh khoản chi tiêu nào trong sổ cái, ví vẫn nguyên vẹn 100%! 🐷";
            }
            return "🔥 Trong tháng này, danh mục tốn nhiều nhất của bạn là \"" + s.topCategoryName
                    + "\" với tổng cộng " + formatCurrency(s.topCategoryAmount)
                    + " (trên tổng chi tháng " + formatCurrency(s.monthlyExpense)
                    + "). Thử đặt mục tiêu cắt giảm 15% nhóm này xem sao!";
        }

        if (q.contains("streak") || q.contains("mẹo") || q.contains("tips")) {
            return "💡 Bạn đang có chuỗi Streak " + s.streakDays + " ngày (tổng " + s.totalSafeDays + " ngày đạt chuẩn). "
                    + "Mẹo nhỏ: Hãy giữ mức chi mỗi ngày dưới " + formatCurrency(s.dailyLimit * HAPPY_RATIO)
                    + " (80% hạn mức) để phòng phát sinh đột xuất và giúp mình tiến hóa nhanh hơn! ✨";
        }

        if (q.contains("ví") || q.contains("sức khỏe") || q.contains("tháng") || q.contains("health")) {
            double net = s.monthlyIncome - s.monthlyExpense;
            String netStatus = net >= 0
                    ? "đang dương " + formatCurrency(net) + " (rất tốt!)"
                    : "đang âm " + formatCurrency(Math.abs(net)) + " (cần thắt chặt chi tiêu!)";
            return "🐷 Sức khỏe ví tháng này: Thu nhập " + formatCurrency(s.monthlyIncome)
                    + ", Chi tiêu " + formatCurrency(s.monthlyExpense)
                    + " → Dòng tiền " + netStatus + ".";
        }

        return "🐷 Heo Đất báo cáo nhanh: Hôm nay tiêu " + formatCurrency(s.dailySpent)
                + "/" + formatCurrency(s.dailyLimit)
                + ", Streak " + s.streakDays + " ngày. "
                + "Nhóm tốn nhất tháng là \"" + s.topCategoryName + "\" (" + formatCurrency(s.topCategoryAmount) + "). "
                + "Bạn muốn mình phân tích kỹ mục nào thêm không?";
    }

    static long startOfDayMillis(long epochMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epochMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void refreshUserIdentityInternal() {
        UserModel user = authRepository != null ? authRepository.getCurrentUser() : null;
        if (user != null) {
            String name = user.getDisplayName();
            if (name != null && !name.trim().isEmpty()) {
                currentUserDisplayName = name.trim();
            } else if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                currentUserDisplayName = user.getEmail().trim();
            } else {
                currentUserDisplayName = GUEST_DISPLAY_NAME;
            }

            String uname = user.getUsername();
            if (uname != null && !uname.trim().isEmpty()) {
                String clean = uname.trim();
                currentUserHandle = clean.startsWith("@") ? clean : "@" + clean;
            } else if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                currentUserHandle = user.getEmail().trim();
            } else {
                currentUserHandle = GUEST_HANDLE;
            }
        } else {
            currentUserDisplayName = GUEST_DISPLAY_NAME;
            currentUserHandle = GUEST_HANDLE;
        }
    }

    private void initMockLeaderboard() {
        mockPool.clear();
        mockPool.add(new LeaderboardEntry(0, "Minh Anh", "@minhanh", 56, 78, EvolutionStage.ROYAL, false, true));
        mockPool.add(new LeaderboardEntry(0, "Việt Hoàng", "@viethoang", 28, 64, EvolutionStage.ADULT, false, true));
        mockPool.add(new LeaderboardEntry(0, currentUserDisplayName, currentUserHandle, streakDays, totalSafeDays, computeStage(streakDays), true, true));
        mockPool.add(new LeaderboardEntry(0, "Thanh Trúc", "@thanhtruc", 22, 41, EvolutionStage.ADULT, false, false));
        mockPool.add(new LeaderboardEntry(0, "Quốc Bảo", "@quocbao", 19, 85, EvolutionStage.STUDENT, false, false));
        mockPool.add(new LeaderboardEntry(0, "Gia Huy", "@giahuy", 9, 27, EvolutionStage.STUDENT, false, true));
        mockPool.add(new LeaderboardEntry(0, "Thảo Vy", "@thaovy", 5, 19, EvolutionStage.BABY, false, false));
    }

    private void updateCurrentUserInMockPool() {
        for (int i = 0; i < mockPool.size(); i++) {
            LeaderboardEntry e = mockPool.get(i);
            if (e.isCurrentUser) {
                mockPool.set(i, new LeaderboardEntry(
                        0,
                        currentUserDisplayName,
                        currentUserHandle,
                        streakDays,
                        totalSafeDays,
                        computeStage(streakDays),
                        true,
                        e.isFriend
                ));
                break;
            }
        }
    }

    private void publishPiggyState() {
        Mood mood = computeMood(dailySpent, dailyLimit);
        EvolutionStage stage = computeStage(streakDays);
        String[] speeches = speechesFor(mood);
        String speech = speeches[speechIndex % speeches.length];
        piggyState.setValue(new PiggyUiState(
                dailySpent,
                dailyLimit,
                streakDays,
                totalSafeDays,
                mood,
                stage,
                speech
        ));
    }

    private void publishLeaderboard() {
        List<LeaderboardEntry> filtered = new ArrayList<>();
        for (LeaderboardEntry entry : mockPool) {
            if (scope == RankingScope.GLOBAL || entry.isFriend || entry.isCurrentUser) {
                filtered.add(entry);
            }
        }

        Comparator<LeaderboardEntry> comparator = metric == RankingMetric.STREAK
                ? (a, b) -> Integer.compare(b.streakDays, a.streakDays)
                : (a, b) -> Integer.compare(b.totalSafeDays, a.totalSafeDays);
        Collections.sort(filtered, comparator);

        List<LeaderboardEntry> ranked = new ArrayList<>(filtered.size());
        for (int i = 0; i < filtered.size(); i++) {
            LeaderboardEntry raw = filtered.get(i);
            ranked.add(new LeaderboardEntry(
                    i + 1,
                    raw.displayName,
                    raw.username,
                    raw.streakDays,
                    raw.totalSafeDays,
                    raw.stage,
                    raw.isCurrentUser,
                    raw.isFriend
            ));
        }
        leaderboardEntries.setValue(ranked);
    }

    private static String[] speechesFor(Mood mood) {
        switch (mood) {
            case WORRIED:
                return WORRIED_SPEECHES;
            case SAD:
                return SAD_SPEECHES;
            case HAPPY:
            default:
                return HAPPY_SPEECHES;
        }
    }

    private static String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(0);
        return format.format(amount) + " ₫";
    }
}
