package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthUseCaseTest {

    @Mock
    private IAuthRepository repository;

    private SignUpUseCase signUp;
    private SignInUseCase signIn;
    private ResetPasswordUseCase resetPassword;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        signUp = new SignUpUseCase(repository);
        signIn = new SignInUseCase(repository);
        resetPassword = new ResetPasswordUseCase(repository);
    }

    @Test
    public void signUp_validInput_trimsAndCallsRepository() {
        UserModel user = new UserModel("1", "a@b.com", "An");
        when(repository.signUp("a@b.com", "123456", "An")).thenReturn(Result.success(user));

        Result<UserModel> result = signUp.execute("  An ", " a@b.com ", "123456");

        assertTrue(result.isSuccess());
        assertEquals("An", result.getData().getDisplayName());
    }

    @Test
    public void signUp_emptyName_failsWithoutCallingRepository() {
        Result<UserModel> result = signUp.execute(" ", "a@b.com", "123456");

        assertTrue(result.isError());
        verify(repository, never()).signUp(anyString(), anyString(), anyString());
    }

    @Test
    public void signUp_shortPassword_fails() {
        Result<UserModel> result = signUp.execute("An", "a@b.com", "12345");

        assertTrue(result.isError());
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự", result.getMessage());
        verify(repository, never()).signUp(anyString(), anyString(), anyString());
    }

    @Test
    public void signIn_invalidEmail_fails() {
        Result<UserModel> result = signIn.execute("not-an-email", "123456");

        assertTrue(result.isError());
        assertEquals("Email không hợp lệ", result.getMessage());
        verify(repository, never()).signIn(anyString(), anyString());
    }

    @Test
    public void signIn_emptyPassword_fails() {
        Result<UserModel> result = signIn.execute("a@b.com", "");

        assertTrue(result.isError());
        verify(repository, never()).signIn(anyString(), anyString());
    }

    @Test
    public void signIn_repositoryErrorIsPassedThrough() {
        when(repository.signIn("a@b.com", "wrongpass"))
                .thenReturn(Result.error("Sai email hoặc mật khẩu", null));

        Result<UserModel> result = signIn.execute("a@b.com", "wrongpass");

        assertEquals("Sai email hoặc mật khẩu", result.getMessage());
    }

    @Test
    public void resetPassword_emptyEmail_fails() {
        Result<Void> result = resetPassword.execute("");

        assertTrue(result.isError());
        verify(repository, never()).sendPasswordReset(anyString());
    }
}
