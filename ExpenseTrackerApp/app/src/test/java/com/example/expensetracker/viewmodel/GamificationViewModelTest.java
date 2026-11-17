package com.example.expensetracker.viewmodel;

import android.content.SharedPreferences;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.LeaderboardProfileModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import com.example.expensetracker.core.domain.repository.ILeaderboardRepository;
import com.example.expensetracker.core.domain.usecase.GetAllTransactionsUseCase;
import com.example.expensetracker.core.domain.usecase.SendAiChatPromptUseCase;
import com.example.expensetracker.viewmodel.GamificationViewModel.ChatMessage;
import com.example.expensetracker.viewmodel.GamificationViewModel.EvolutionStage;
import com.example.expensetracker.viewmodel.GamificationViewModel.LeaderboardEntry;
import com.example.expensetracker.viewmodel.GamificationViewModel.Mood;
import com.example.expensetracker.viewmodel.GamificationViewModel.PiggyUiState;
import com.example.expensetracker.viewmodel.GamificationViewModel.RankingMetric;
import com.example.expensetracker.viewmodel.GamificationViewModel.RankingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GamificationViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private GetAllTransactionsUseCase getAllTransactionsUseCase;
    private IAuthRepository authRepository;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private SendAiChatPromptUseCase sendAiChatPromptUseCase;
    private ILeaderboardRepository leaderboardRepository;
    private MutableLiveData<List<TransactionModel>> transactionsLiveData;
    private long fixedNowMillis;
    private GamificationViewModel viewModel;

    @Before
    public void setUp() {
        getAllTransactionsUseCase = mock(GetAllTransactionsUseCase.class);
        authRepository = mock(IAuthRepository.class);
        prefs = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);
        sendAiChatPromptUseCase = mock(SendAiChatPromptUseCase.class);
        leaderboardRepository = mock(ILeaderboardRepository.class);

        transactionsLiveData = new MutableLiveData<>();
        when(getAllTransactionsUseCase.execute()).thenReturn(transactionsLiveData);
        when(prefs.contains(anyString())).thenReturn(false);
        when(prefs.edit()).thenReturn(editor);
        when(editor.putLong(anyString(), anyLong())).thenReturn(editor);
        when(editor.putStringSet(anyString(), anySet())).thenReturn(editor);
        when(sendAiChatPromptUseCase.isConfigured()).thenReturn(false);

        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.NOVEMBER, 13, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        fixedNowMillis = cal.getTimeInMillis();

        viewModel = new GamificationViewModel(
                getAllTransactionsUseCase,
                authRepository,
                prefs,
                sendAiChatPromptUseCase,
                leaderboardRepository,
                Runnable::run,
                () -> fixedNowMillis
        );
    }

    @Test
    public void computeMood_returnsCorrectMoodForCustomAndDefaultThresholds() {
        assertEquals(Mood.HAPPY, GamificationViewModel.computeMood(0.0));
        assertEquals(Mood.HAPPY, GamificationViewModel.computeMood(80_000.0));
        assertEquals(Mood.WORRIED, GamificationViewModel.computeMood(80_001.0));
        assertEquals(Mood.WORRIED, GamificationViewModel.computeMood(100_000.0));
        assertEquals(Mood.SAD, GamificationViewModel.computeMood(100_001.0));

        assertEquals(Mood.HAPPY, GamificationViewModel.computeMood(160_000.0, 200_000.0));
        assertEquals(Mood.WORRIED, GamificationViewModel.computeMood(170_000.0, 200_000.0));
        assertEquals(Mood.SAD, GamificationViewModel.computeMood(205_000.0, 200_000.0));
    }

    @Test
    public void computeStage_returnsCorrectEvolutionStageForStreaks() {
        assertEquals(EvolutionStage.BABY, GamificationViewModel.computeStage(0));
        assertEquals(EvolutionStage.BABY, GamificationViewModel.computeStage(6));
        assertEquals(EvolutionStage.STUDENT, GamificationViewModel.computeStage(7));
        assertEquals(EvolutionStage.STUDENT, GamificationViewModel.computeStage(20));
        assertEquals(EvolutionStage.ADULT, GamificationViewModel.computeStage(21));
        assertEquals(EvolutionStage.ADULT, GamificationViewModel.computeStage(49));
        assertEquals(EvolutionStage.ROYAL, GamificationViewModel.computeStage(50));
        assertEquals(EvolutionStage.ROYAL, GamificationViewModel.computeStage(100));
    }

    @Test
    public void onPiggyTapped_rotatesSpeechBubbleText() {
        PiggyUiState initial = viewModel.getPiggyState().getValue();
        assertNotNull(initial);
        String firstSpeech = initial.speechBubbleText;

        viewModel.onPiggyTapped();
        PiggyUiState afterTap = viewModel.getPiggyState().getValue();
        assertNotNull(afterTap);
        assertNotEquals(firstSpeech, afterTap.speechBubbleText);
    }

    @Test
    public void transactionsUpdate_computesDailySpentStreakAndTotalSafeDays() {
        TransactionModel tMinus4 = expense(daysAgo(4), 50_000.0, "Food");
        TransactionModel tMinus3 = expense(daysAgo(3), 150_000.0, "Shopping");
        TransactionModel tMinus2 = expense(daysAgo(2), 30_000.0, "Food");
        TransactionModel tToday = expense(daysAgo(0), 90_000.0, "Transport");

        TransactionModel incomeToday = new TransactionModel.Builder()
                .amount(500_000.0)
                .type("INCOME")
                .timestamp(daysAgo(0))
                .build();
        TransactionModel deletedToday = expense(daysAgo(0), 200_000.0, "Food");
        deletedToday.setDeleted(true);

        transactionsLiveData.setValue(Arrays.asList(tMinus4, tMinus3, tMinus2, tToday, incomeToday, deletedToday));
        // Day -1 had no transaction, so checking in Day -1 keeps the streak alive
        viewModel.recordDailyCheckIn(daysAgo(1).getTime());

        PiggyUiState state = viewModel.getPiggyState().getValue();
        assertNotNull(state);
        assertEquals(90_000.0, state.dailySpent, 0.01);
        assertEquals(Mood.WORRIED, state.mood);
        assertEquals(3, state.streakDays);
        assertEquals(4, state.totalSafeDays);
    }

    @Test
    public void inactiveDayWithoutCheckInOrTransaction_breaksStreakButKeepsTotalSafeDays() {
        TransactionModel tMinus2 = expense(daysAgo(2), 30_000.0, "Food");
        TransactionModel tToday = expense(daysAgo(0), 40_000.0, "Food");

        transactionsLiveData.setValue(Arrays.asList(tMinus2, tToday));

        PiggyUiState state = viewModel.getPiggyState().getValue();
        assertNotNull(state);
        assertEquals(1, state.streakDays);
        assertEquals(2, state.totalSafeDays);
    }

    @Test
    public void suggestDailyBudget_computesRecommendedCapFromThreeStepInputs() {
        double suggested = GamificationViewModel.suggestDailyBudget(15_000_000.0, 6_000_000.0, 20);
        assertEquals(200_000.0, suggested, 0.01);
    }

    @Test
    public void addFriendByUsername_addsExistingAndNewHandleToFriendsLeaderboard() {
        assertTrue(viewModel.addFriendByUsername("@thanhtruc"));
        assertTrue(viewModel.addFriendByUsername("newfriend"));

        viewModel.setRankingScope(RankingScope.FRIENDS);
        List<LeaderboardEntry> friends = viewModel.getLeaderboardEntries().getValue();
        assertNotNull(friends);

        boolean hasThanhTruc = false;
        boolean hasNewFriend = false;
        for (LeaderboardEntry entry : friends) {
            if ("@thanhtruc".equals(entry.username)) hasThanhTruc = true;
            if ("@newfriend".equals(entry.username)) hasNewFriend = true;
        }
        assertTrue(hasThanhTruc);
        assertTrue(hasNewFriend);
    }

    @Test
    public void syncLeaderboardWithCloud_upsertsAndMergesRemoteProfiles() {
        when(authRepository.getCurrentUser()).thenReturn(new UserModel("u1", "khoi@example.com", "Khôi", "khoing"));
        viewModel.refreshUserIdentity();

        List<LeaderboardProfileModel> remoteProfiles = Arrays.asList(
                new LeaderboardProfileModel("u2", "Lan Anh", "@lananh", 42, 60, "ADULT")
        );
        when(leaderboardRepository.fetchTopProfiles(anyInt())).thenReturn(Result.success(remoteProfiles));

        viewModel.syncLeaderboardWithCloud();

        List<LeaderboardEntry> entries = viewModel.getLeaderboardEntries().getValue();
        assertNotNull(entries);
        boolean foundLanAnh = false;
        for (LeaderboardEntry e : entries) {
            if ("@lananh".equals(e.username) && e.streakDays == 42) {
                foundLanAnh = true;
            }
        }
        assertTrue(foundLanAnh);
    }

    @Test
    public void setDailyBudgetLimit_persistsLimitAndRecalculatesMoodAndStreak() {
        TransactionModel tMinus1 = expense(daysAgo(1), 150_000.0, "Food");
        TransactionModel tToday = expense(daysAgo(0), 120_000.0, "Food");
        transactionsLiveData.setValue(Arrays.asList(tMinus1, tToday));

        PiggyUiState before = viewModel.getPiggyState().getValue();
        assertNotNull(before);
        assertEquals(Mood.SAD, before.mood);
        assertEquals(0, before.streakDays);

        clearInvocations(editor);
        viewModel.setDailyBudgetLimit(200_000.0);

        verify(editor).putLong(
                eq(GamificationViewModel.KEY_DAILY_BUDGET_LIMIT),
                eq(Double.doubleToRawLongBits(200_000.0))
        );
        verify(editor).apply();

        PiggyUiState after = viewModel.getPiggyState().getValue();
        assertNotNull(after);
        assertEquals(200_000.0, after.dailyLimit, 0.01);
        assertEquals(Mood.HAPPY, after.mood);
        assertEquals(2, after.streakDays);
        assertEquals(2, after.totalSafeDays);
    }

    @Test
    public void refreshUserIdentity_updatesLeaderboardEntryForGuestAndSignedInUser() {
        LeaderboardEntry guestEntry = findCurrentUserEntry(viewModel.getLeaderboardEntries().getValue());
        assertNotNull(guestEntry);
        assertEquals(GamificationViewModel.GUEST_DISPLAY_NAME, guestEntry.displayName);
        assertEquals(GamificationViewModel.GUEST_HANDLE, guestEntry.username);

        when(authRepository.getCurrentUser()).thenReturn(new UserModel("u1", "khoi@example.com", "Khôi Nguyễn", "khoing"));
        viewModel.refreshUserIdentity();

        LeaderboardEntry signedInEntry = findCurrentUserEntry(viewModel.getLeaderboardEntries().getValue());
        assertNotNull(signedInEntry);
        assertEquals("Khôi Nguyễn", signedInEntry.displayName);
        assertEquals("@khoing", signedInEntry.username);
    }

    @Test
    public void setRankingMetricAndScope_sortsAndFiltersCorrectly() {
        viewModel.setRankingMetric(RankingMetric.STREAK);
        List<LeaderboardEntry> streakList = viewModel.getLeaderboardEntries().getValue();
        assertNotNull(streakList);
        assertTrue(streakList.size() > 1);
        for (int i = 0; i < streakList.size(); i++) {
            assertEquals(i + 1, streakList.get(i).rank);
            if (i > 0) {
                assertTrue(streakList.get(i - 1).streakDays >= streakList.get(i).streakDays);
            }
        }

        viewModel.setRankingMetric(RankingMetric.TOTAL_SAFE_DAYS);
        List<LeaderboardEntry> totalDaysList = viewModel.getLeaderboardEntries().getValue();
        assertNotNull(totalDaysList);
        assertEquals(streakList.size(), totalDaysList.size());
        for (int i = 0; i < totalDaysList.size(); i++) {
            assertEquals(i + 1, totalDaysList.get(i).rank);
            if (i > 0) {
                assertTrue(totalDaysList.get(i - 1).totalSafeDays >= totalDaysList.get(i).totalSafeDays);
            }
        }

        viewModel.setRankingScope(RankingScope.GLOBAL);
        List<LeaderboardEntry> globalList = viewModel.getLeaderboardEntries().getValue();
        assertNotNull(globalList);

        viewModel.setRankingScope(RankingScope.FRIENDS);
        List<LeaderboardEntry> friendsList = viewModel.getLeaderboardEntries().getValue();
        assertNotNull(friendsList);
        assertTrue(friendsList.size() < globalList.size());
        for (LeaderboardEntry entry : friendsList) {
            assertTrue(entry.isFriend || entry.isCurrentUser);
        }
    }

    @Test
    public void initialState_seedsGreetingChatMessage() {
        List<ChatMessage> messages = viewModel.getChatMessages().getValue();
        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertFalse(messages.get(0).isUser);
    }

    @Test
    public void sendChatMessage_usesGeminiReplyWhenConfiguredAndSuccessful() {
        when(sendAiChatPromptUseCase.isConfigured()).thenReturn(true);
        when(sendAiChatPromptUseCase.execute(anyString(), eq("Phân tích hôm nay")))
                .thenReturn(Result.success("Heo thấy hôm nay bạn tiêu rất hợp lý!"));

        viewModel.sendChatMessage("Phân tích hôm nay");

        List<ChatMessage> messages = viewModel.getChatMessages().getValue();
        assertNotNull(messages);
        assertEquals(3, messages.size());
        assertTrue(messages.get(1).isUser);
        assertEquals("Phân tích hôm nay", messages.get(1).text);
        assertFalse(messages.get(2).isUser);
        assertEquals("Heo thấy hôm nay bạn tiêu rất hợp lý!", messages.get(2).text);
        assertEquals(Boolean.FALSE, viewModel.getIsAiTyping().getValue());
    }

    @Test
    public void sendChatMessage_fallsBackToSmartLocalAdvisorWhenGeminiUnconfiguredOrFails() {
        transactionsLiveData.setValue(Arrays.asList(
                expense(daysAgo(0), 65_000.0, "Food"),
                expense(daysAgo(1), 180_000.0, "Shopping")
        ));
        when(sendAiChatPromptUseCase.isConfigured()).thenReturn(false);

        viewModel.sendChatMessage("🔥 Danh mục tốn nhất");

        List<ChatMessage> messages = viewModel.getChatMessages().getValue();
        assertNotNull(messages);
        assertEquals(3, messages.size());
        String reply = messages.get(2).text;
        assertTrue(reply.contains("Shopping"));
        assertTrue(reply.contains("180,000 ₫"));

        when(sendAiChatPromptUseCase.isConfigured()).thenReturn(true);
        when(sendAiChatPromptUseCase.execute(anyString(), eq("📊 Phân tích hôm nay")))
                .thenReturn(Result.error("HTTP_500", null));

        viewModel.sendChatMessage("📊 Phân tích hôm nay");
        messages = viewModel.getChatMessages().getValue();
        assertNotNull(messages);
        assertEquals(5, messages.size());
        assertTrue(messages.get(4).text.contains("65,000 ₫"));
    }

    private Date daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(fixedNowMillis);
        cal.add(Calendar.DAY_OF_YEAR, -days);
        return cal.getTime();
    }

    private static TransactionModel expense(Date timestamp, double amount, String category) {
        return new TransactionModel.Builder()
                .amount(amount)
                .category(category)
                .type("EXPENSE")
                .timestamp(timestamp)
                .build();
    }

    private static LeaderboardEntry findCurrentUserEntry(List<LeaderboardEntry> list) {
        if (list == null) {
            return null;
        }
        for (LeaderboardEntry entry : list) {
            if (entry.isCurrentUser) {
                return entry;
            }
        }
        return null;
    }
}
