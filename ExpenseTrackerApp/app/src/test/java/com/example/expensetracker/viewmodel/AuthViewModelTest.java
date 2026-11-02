package com.example.expensetracker.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.ISyncScheduler;
import com.example.expensetracker.core.domain.usecase.GetCurrentUserUseCase;
import com.example.expensetracker.core.domain.usecase.GetSyncStatusUseCase;
import com.example.expensetracker.core.domain.usecase.ResetPasswordUseCase;
import com.example.expensetracker.core.domain.usecase.SignInUseCase;
import com.example.expensetracker.core.domain.usecase.SignOutUseCase;
import com.example.expensetracker.core.domain.usecase.SignUpUseCase;
import com.example.expensetracker.core.domain.usecase.SyncDataUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private SignInUseCase signInUseCase;
    @Mock private SignUpUseCase signUpUseCase;
    @Mock private ResetPasswordUseCase resetPasswordUseCase;
    @Mock private SignOutUseCase signOutUseCase;
    @Mock private GetCurrentUserUseCase getCurrentUserUseCase;
    @Mock private ExecutorService executor;
    @Mock private ISyncScheduler syncScheduler;
    @Mock private SyncDataUseCase syncDataUseCase;
    @Mock private GetSyncStatusUseCase getSyncStatusUseCase;

    private AuthViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        // Run background work inline so results are visible right away.
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        viewModel = new AuthViewModel(signInUseCase, signUpUseCase, resetPasswordUseCase,
                signOutUseCase, getCurrentUserUseCase, executor, syncScheduler, syncDataUseCase, getSyncStatusUseCase);
    }

    @Test
    public void signIn_success_publishesUser() {
        UserModel user = new UserModel("1", "a@b.com", "An", "an");
        when(signInUseCase.execute("a@b.com", "123456")).thenReturn(Result.success(user));

        viewModel.signIn("a@b.com", "123456");

        assertTrue(viewModel.getAuthState().getValue().isSuccess());
        assertEquals("a@b.com", viewModel.getAuthState().getValue().getData().getEmail());
        verify(syncScheduler).requestSync();
    }

    @Test
    public void signUp_error_publishesMessage() {
        when(signUpUseCase.execute("An", "an", "a@b.com", "123456"))
                .thenReturn(Result.error("Email đã được đăng ký", null));

        viewModel.signUp("An", "an", "a@b.com", "123456");

        assertTrue(viewModel.getAuthState().getValue().isError());
        verify(syncScheduler, never()).requestSync();
        assertEquals("Email đã được đăng ký", viewModel.getAuthState().getValue().getMessage());
    }

    @Test
    public void resetPassword_success_publishesToResetState() {
        when(resetPasswordUseCase.execute("a@b.com")).thenReturn(Result.success(null));

        viewModel.resetPassword("a@b.com");

        assertTrue(viewModel.getResetState().getValue().isSuccess());
    }

    @Test
    public void signIn_withExpensesOnDevice_waitsForMergeChoice() {
        when(signInUseCase.execute("a@b.com", "123456"))
                .thenReturn(Result.success(new UserModel("1", "a@b.com", "An", "an")));
        when(getSyncStatusUseCase.unsyncedTransactions()).thenReturn(3);

        viewModel.signIn("a@b.com", "123456");

        assertEquals(3, viewModel.getUnsyncedOnSignIn());
        verify(syncScheduler, never()).requestSync();
        verify(syncScheduler).cancelAll();
    }

    @Test
    public void discardLocalData_wipesThenSyncs() {
        viewModel.discardLocalData();

        InOrder order = inOrder(syncDataUseCase, syncScheduler);
        order.verify(syncDataUseCase).clearLocalData();
        order.verify(syncScheduler).requestSync();
    }

    @Test
    public void signOut_withoutSync_callsUseCase() {
        viewModel.signOut(false);

        verify(signOutUseCase).execute();
        verify(syncDataUseCase, never()).execute();
        assertTrue(viewModel.getSignOutState().getValue().isSuccess());
    }

    @Test
    public void signOut_syncFirstFails_staysSignedIn() {
        when(syncDataUseCase.execute()).thenReturn(Result.error("offline", null));

        viewModel.signOut(true);

        verify(signOutUseCase, never()).execute();
        assertTrue(viewModel.getSignOutState().getValue().isError());
    }

    @Test
    public void signOut_syncFirstSucceeds_signsOut() {
        when(syncDataUseCase.execute()).thenReturn(Result.success(null));

        viewModel.signOut(true);

        verify(signOutUseCase).execute();
    }
}
