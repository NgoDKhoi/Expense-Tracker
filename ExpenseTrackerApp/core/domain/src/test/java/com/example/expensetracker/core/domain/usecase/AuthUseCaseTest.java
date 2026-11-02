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

    // --- Sign up ---

    @Test
    public void signUp_validInput_normalizesAndCallsRepository() {
        UserModel user = new UserModel("1", "a@b.com", "Việt Hoàng", "viethoang");
        when(repository.findEmailByUsername("viethoang")).thenReturn(Result.success(null));
        when(repository.signUp("a@b.com", "123456", "Việt Hoàng", "viethoang")).thenReturn(Result.success(user));

        Result<UserModel> result = signUp.execute("  Việt Hoàng ", " VietHoang ", " a@b.com ", "123456");

        assertTrue(result.isSuccess());
        assertEquals("viethoang", result.getData().getUsername());
    }

    @Test
    public void signUp_takenUsername_failsWithoutCreatingAccount() {
        when(repository.findEmailByUsername("viethoang")).thenReturn(Result.success("other@b.com"));

        Result<UserModel> result = signUp.execute("An", "viethoang", "a@b.com", "123456");

        assertEquals("Tên đăng nhập đã được sử dụng", result.getMessage());
        verify(repository, never()).signUp(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void signUp_usernameWithAccentOrSpace_fails() {
        assertTrue(signUp.execute("An", "việt hoàng", "a@b.com", "123456").isError());
        assertTrue(signUp.execute("An", "ab", "a@b.com", "123456").isError());
        verify(repository, never()).findEmailByUsername(anyString());
    }

    @Test
    public void signUp_emptyName_fails() {
        Result<UserModel> result = signUp.execute(" ", "viethoang", "a@b.com", "123456");

        assertTrue(result.isError());
        verify(repository, never()).signUp(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void signUp_shortPassword_fails() {
        Result<UserModel> result = signUp.execute("An", "viethoang", "a@b.com", "12345");

        assertEquals("Mật khẩu phải có ít nhất 6 ký tự", result.getMessage());
    }

    // --- Sign in ---

    @Test
    public void signIn_withEmail_skipsUsernameLookup() {
        when(repository.signIn("a@b.com", "123456")).thenReturn(Result.success(new UserModel("1", "a@b.com", "An", "")));

        assertTrue(signIn.execute(" a@b.com ", "123456").isSuccess());
        verify(repository, never()).findEmailByUsername(anyString());
    }

    @Test
    public void signIn_withUsername_signsInWithItsEmail() {
        when(repository.findEmailByUsername("viethoang")).thenReturn(Result.success("a@b.com"));
        when(repository.signIn("a@b.com", "123456")).thenReturn(Result.success(new UserModel("1", "a@b.com", "An", "viethoang")));

        Result<UserModel> result = signIn.execute("VietHoang", "123456");

        assertTrue(result.isSuccess());
    }

    @Test
    public void signIn_unknownUsername_fails() {
        when(repository.findEmailByUsername("ghost")).thenReturn(Result.success(null));

        Result<UserModel> result = signIn.execute("ghost", "123456");

        assertEquals("Không tìm thấy tài khoản có tên đăng nhập này", result.getMessage());
        verify(repository, never()).signIn(anyString(), anyString());
    }

    @Test
    public void signIn_invalidEmail_fails() {
        Result<UserModel> result = signIn.execute("a@b", "123456");

        assertEquals("Email không hợp lệ", result.getMessage());
    }

    @Test
    public void signIn_emptyPassword_fails() {
        assertTrue(signIn.execute("a@b.com", "").isError());
        verify(repository, never()).signIn(anyString(), anyString());
    }

    @Test
    public void signIn_lookupErrorIsPassedThrough() {
        when(repository.findEmailByUsername("viethoang")).thenReturn(Result.error("Không có kết nối mạng. Vui lòng thử lại.", null));

        assertEquals("Không có kết nối mạng. Vui lòng thử lại.", signIn.execute("viethoang", "123456").getMessage());
    }

    // --- Password reset ---

    @Test
    public void resetPassword_withUsername_sendsToItsEmail() {
        when(repository.findEmailByUsername("viethoang")).thenReturn(Result.success("a@b.com"));
        when(repository.sendPasswordReset("a@b.com")).thenReturn(Result.success(null));

        assertTrue(resetPassword.execute("viethoang").isSuccess());
    }

    @Test
    public void resetPassword_empty_fails() {
        assertTrue(resetPassword.execute("").isError());
        verify(repository, never()).sendPasswordReset(anyString());
    }
}
