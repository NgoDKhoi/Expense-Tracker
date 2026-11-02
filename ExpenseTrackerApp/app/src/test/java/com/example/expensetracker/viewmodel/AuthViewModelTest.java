package com.example.expensetracker.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.usecase.GetCurrentUserUseCase;
import com.example.expensetracker.core.domain.usecase.ResetPasswordUseCase;
import com.example.expensetracker.core.domain.usecase.SignInUseCase;
import com.example.expensetracker.core.domain.usecase.SignOutUseCase;
import com.example.expensetracker.core.domain.usecase.SignUpUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
                signOutUseCase, getCurrentUserUseCase, executor);
    }

    @Test
    public void signIn_success_publishesUser() {
        UserModel user = new UserModel("1", "a@b.com", "An");
        when(signInUseCase.execute("a@b.com", "123456")).thenReturn(Result.success(user));

        viewModel.signIn("a@b.com", "123456");

        assertTrue(viewModel.getAuthState().getValue().isSuccess());
        assertEquals("a@b.com", viewModel.getAuthState().getValue().getData().getEmail());
    }

    @Test
    public void signUp_error_publishesMessage() {
        when(signUpUseCase.execute("An", "a@b.com", "123456"))
                .thenReturn(Result.error("Email đã được đăng ký", null));

        viewModel.signUp("An", "a@b.com", "123456");

        assertTrue(viewModel.getAuthState().getValue().isError());
        assertEquals("Email đã được đăng ký", viewModel.getAuthState().getValue().getMessage());
    }

    @Test
    public void resetPassword_success_publishesToResetState() {
        when(resetPasswordUseCase.execute("a@b.com")).thenReturn(Result.success(null));

        viewModel.resetPassword("a@b.com");

        assertTrue(viewModel.getResetState().getValue().isSuccess());
    }

    @Test
    public void signOut_callsUseCase() {
        viewModel.signOut();

        verify(signOutUseCase).execute();
    }
}
