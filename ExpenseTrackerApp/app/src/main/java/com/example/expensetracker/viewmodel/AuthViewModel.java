package com.example.expensetracker.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.usecase.GetCurrentUserUseCase;
import com.example.expensetracker.core.domain.usecase.ResetPasswordUseCase;
import com.example.expensetracker.core.domain.usecase.SignInUseCase;
import com.example.expensetracker.core.domain.usecase.SignOutUseCase;
import com.example.expensetracker.core.domain.usecase.SignUpUseCase;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

public class AuthViewModel extends ViewModel {

    private final SignInUseCase signInUseCase;
    private final SignUpUseCase signUpUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final SignOutUseCase signOutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final ExecutorService executor;

    private final MutableLiveData<Result<UserModel>> authState = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> resetState = new MutableLiveData<>();

    @Inject
    public AuthViewModel(SignInUseCase signInUseCase,
                         SignUpUseCase signUpUseCase,
                         ResetPasswordUseCase resetPasswordUseCase,
                         SignOutUseCase signOutUseCase,
                         GetCurrentUserUseCase getCurrentUserUseCase,
                         ExecutorService executor) {
        this.signInUseCase = signInUseCase;
        this.signUpUseCase = signUpUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.signOutUseCase = signOutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.executor = executor;
    }

    public LiveData<Result<UserModel>> getAuthState() {
        return authState;
    }

    public LiveData<Result<Void>> getResetState() {
        return resetState;
    }

    /** Signed-in user from the saved session, or null for guests. */
    public UserModel getCurrentUser() {
        return getCurrentUserUseCase.execute();
    }

    public void signIn(String email, String password) {
        authState.setValue(Result.loading());
        executor.execute(() -> authState.postValue(signInUseCase.execute(email, password)));
    }

    public void signUp(String displayName, String email, String password) {
        authState.setValue(Result.loading());
        executor.execute(() -> authState.postValue(signUpUseCase.execute(displayName, email, password)));
    }

    public void resetPassword(String email) {
        resetState.setValue(Result.loading());
        executor.execute(() -> resetState.postValue(resetPasswordUseCase.execute(email)));
    }

    public void signOut() {
        executor.execute(signOutUseCase::execute);
    }
}
