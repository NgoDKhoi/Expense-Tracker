package com.example.expensetracker.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.expensetracker.core.domain.EmailNotConfirmedException;
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
import com.example.expensetracker.core.domain.usecase.VerifySignUpUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
    @Mock private VerifySignUpUseCase verifySignUpUseCase;

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
                signOutUseCase, getCurrentUserUseCase, executor, syncScheduler, syncDataUseCase, getSyncStatusUseCase,
                verifySignUpUseCase);
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
        when(resetPasswordUseCase.execute("a@b.com")).thenReturn(Result.success("a@b.com"));

        viewModel.resetPassword("a@b.com");

        assertTrue(viewModel.getResetState().getValue().isSuccess());
        assertEquals(AuthViewModel.CodeStep.Kind.RESET, viewModel.getCodeStep().getValue().kind);
        assertEquals("a@b.com", viewModel.getCodeStep().getValue().email);
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

    // --- Email codes ---

    private static <T> Result<T> needsCode(String email) {
        return Result.error("code", new EmailNotConfirmedException(email));
    }

    @Test
    public void signUp_needingCode_opensCodeStepWithoutErrorOrSync() {
        when(signUpUseCase.execute("An", "an", "a@b.com", "123456")).thenReturn(needsCode("a@b.com"));

        viewModel.signUp("An", "an", "a@b.com", "123456");

        assertEquals(AuthViewModel.CodeStep.Kind.SIGN_UP, viewModel.getCodeStep().getValue().kind);
        assertEquals("a@b.com", viewModel.getCodeStep().getValue().email);
        assertNull(viewModel.getAuthState().getValue());
        verify(syncScheduler, never()).requestSync();
        verify(verifySignUpUseCase, never()).resend(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    public void signIn_unconfirmed_resendsCodeAndOpensCodeStep() {
        when(signInUseCase.execute("an", "123456")).thenReturn(needsCode("a@b.com"));

        viewModel.signIn("an", "123456");

        verify(verifySignUpUseCase).resend("a@b.com");
        assertEquals("a@b.com", viewModel.getCodeStep().getValue().email);
    }

    @Test
    public void submitCode_success_signsInAndSyncs() {
        when(signUpUseCase.execute("An", "an", "a@b.com", "123456")).thenReturn(needsCode("a@b.com"));
        when(verifySignUpUseCase.execute("a@b.com", "654321"))
                .thenReturn(Result.success(new UserModel("1", "a@b.com", "An", "an")));
        viewModel.signUp("An", "an", "a@b.com", "123456");

        viewModel.submitCode("654321", "");

        assertTrue(viewModel.getAuthState().getValue().isSuccess());
        verify(syncScheduler).requestSync();
    }

    @Test
    public void submitCode_inResetStep_setsNewPassword() {
        when(resetPasswordUseCase.execute("an")).thenReturn(Result.success("a@b.com"));
        when(resetPasswordUseCase.confirm("a@b.com", "654321", "newpass"))
                .thenReturn(Result.success(new UserModel("1", "a@b.com", "An", "an")));
        viewModel.resetPassword("an");

        viewModel.submitCode("654321", "newpass");

        assertTrue(viewModel.getAuthState().getValue().isSuccess());
    }

    @Test
    public void leaveCodeStep_returnsToForm() {
        when(resetPasswordUseCase.execute("an")).thenReturn(Result.success("a@b.com"));
        viewModel.resetPassword("an");

        viewModel.leaveCodeStep();

        assertNull(viewModel.getCodeStep().getValue());
    }
}
