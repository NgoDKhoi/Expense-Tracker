package com.example.expensetracker.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

public class AuthViewModel extends ViewModel {

    /** What the account page shows about backup. */
    public static final class SyncInfo {
        public final int pendingChanges;
        public final long lastSyncedAt;

        public SyncInfo(int pendingChanges, long lastSyncedAt) {
            this.pendingChanges = pendingChanges;
            this.lastSyncedAt = lastSyncedAt;
        }
    }

    /** The sheet is waiting for the code mailed to {@link #email}. */
    public static final class CodeStep {
        public enum Kind { SIGN_UP, RESET }

        public final Kind kind;
        public final String email;

        public CodeStep(Kind kind, String email) {
            this.kind = kind;
            this.email = email;
        }
    }

    private final SignInUseCase signInUseCase;
    private final SignUpUseCase signUpUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final SignOutUseCase signOutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final ExecutorService executor;
    private final ISyncScheduler syncScheduler;
    private final SyncDataUseCase syncDataUseCase;
    private final GetSyncStatusUseCase getSyncStatusUseCase;
    private final VerifySignUpUseCase verifySignUpUseCase;

    private final MutableLiveData<Result<UserModel>> authState = new MutableLiveData<>();
    private final MutableLiveData<Result<String>> resetState = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> signOutState = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> syncNowState = new MutableLiveData<>();
    private final MutableLiveData<SyncInfo> syncInfo = new MutableLiveData<>();
    private final MutableLiveData<CodeStep> codeStep = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> resendState = new MutableLiveData<>();
    // Set before authState is posted, so the sign-in screen can read it on success.
    private volatile int unsyncedOnSignIn;

    @Inject
    public AuthViewModel(SignInUseCase signInUseCase,
                         SignUpUseCase signUpUseCase,
                         ResetPasswordUseCase resetPasswordUseCase,
                         SignOutUseCase signOutUseCase,
                         GetCurrentUserUseCase getCurrentUserUseCase,
                         ExecutorService executor,
                         ISyncScheduler syncScheduler,
                         SyncDataUseCase syncDataUseCase,
                         GetSyncStatusUseCase getSyncStatusUseCase,
                         VerifySignUpUseCase verifySignUpUseCase) {
        this.signInUseCase = signInUseCase;
        this.signUpUseCase = signUpUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.signOutUseCase = signOutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.executor = executor;
        this.syncScheduler = syncScheduler;
        this.syncDataUseCase = syncDataUseCase;
        this.getSyncStatusUseCase = getSyncStatusUseCase;
        this.verifySignUpUseCase = verifySignUpUseCase;
    }

    public LiveData<Result<UserModel>> getAuthState() {
        return authState;
    }

    public LiveData<Result<String>> getResetState() {
        return resetState;
    }

    public LiveData<Result<Void>> getSignOutState() {
        return signOutState;
    }

    public LiveData<Result<Void>> getSyncNowState() {
        return syncNowState;
    }

    public LiveData<SyncInfo> getSyncInfo() {
        return syncInfo;
    }

    /** Null when the sheet shows the normal sign in / sign up form. */
    public LiveData<CodeStep> getCodeStep() {
        return codeStep;
    }

    public LiveData<Result<Void>> getResendState() {
        return resendState;
    }

    /** Signed-in user from the saved session, or null for guests. */
    public UserModel getCurrentUser() {
        return getCurrentUserUseCase.execute();
    }

    public void signIn(String emailOrUsername, String password) {
        authState.setValue(Result.loading());
        executor.execute(() -> {
            Result<UserModel> result = signInUseCase.execute(emailOrUsername, password);
            String email = unconfirmedEmail(result);
            // The code from sign up may have expired by now, so mail a fresh one.
            if (email != null) verifySignUpUseCase.resend(email);
            publishAuth(result);
        });
    }

    public void signUp(String displayName, String username, String email, String password) {
        authState.setValue(Result.loading());
        executor.execute(() -> publishAuth(signUpUseCase.execute(displayName, username, email, password)));
    }

    public void resetPassword(String emailOrUsername) {
        resetState.setValue(Result.loading());
        executor.execute(() -> {
            Result<String> result = resetPasswordUseCase.execute(emailOrUsername);
            if (result.isSuccess()) codeStep.postValue(new CodeStep(CodeStep.Kind.RESET, result.getData()));
            resetState.postValue(result);
        });
    }

    /** newPassword is only used for a password reset. */
    public void submitCode(String code, String newPassword) {
        CodeStep step = codeStep.getValue();
        if (step == null) return;
        authState.setValue(Result.loading());
        executor.execute(() -> publishAuth(step.kind == CodeStep.Kind.RESET
                ? resetPasswordUseCase.confirm(step.email, code, newPassword)
                : verifySignUpUseCase.execute(step.email, code)));
    }

    public void resendCode() {
        CodeStep step = codeStep.getValue();
        if (step == null) return;
        resendState.setValue(Result.loading());
        executor.execute(() -> {
            if (step.kind == CodeStep.Kind.RESET) {
                Result<String> sent = resetPasswordUseCase.execute(step.email);
                resendState.postValue(sent.isSuccess() ? Result.success(null) : Result.error(sent.getMessage(), sent.getException()));
            } else {
                resendState.postValue(verifySignUpUseCase.resend(step.email));
            }
        });
    }

    public void leaveCodeStep() {
        codeStep.setValue(null);
    }

    /** Expenses made on this device before signing in; when > 0 the user picks merge or discard. */
    public int getUnsyncedOnSignIn() {
        return unsyncedOnSignIn;
    }

    /** Keep the device's data: it is still pending, so the next sync uploads it into this account. */
    public void mergeLocalData() {
        syncScheduler.requestSync();
    }

    /** Drop the device's data, then download the account's own. */
    public void discardLocalData() {
        executor.execute(() -> {
            syncDataUseCase.clearLocalData();
            syncScheduler.requestSync();
        });
    }

    /** syncFirst: try to back up pending changes before the local data is wiped; stays signed in if that fails. */
    public void signOut(boolean syncFirst) {
        signOutState.setValue(Result.loading());
        executor.execute(() -> {
            if (syncFirst) {
                Result<Void> sync = syncDataUseCase.execute();
                if (!sync.isSuccess()) {
                    signOutState.postValue(sync);
                    return;
                }
            }
            signOutUseCase.execute();
            signOutState.postValue(Result.success(null));
        });
    }

    public void loadSyncInfo() {
        executor.execute(this::postSyncInfo);
    }

    public void syncNow() {
        syncNowState.setValue(Result.loading());
        executor.execute(() -> {
            syncNowState.postValue(syncDataUseCase.execute());
            postSyncInfo();
        });
    }

    private void postSyncInfo() {
        syncInfo.postValue(new SyncInfo(getSyncStatusUseCase.pendingChanges(), getSyncStatusUseCase.lastSyncedAt()));
    }

    private static String unconfirmedEmail(Result<?> result) {
        return result.getException() instanceof EmailNotConfirmedException
                ? ((EmailNotConfirmedException) result.getException()).getEmail()
                : null;
    }

    private void publishAuth(Result<UserModel> result) {
        String unconfirmed = unconfirmedEmail(result);
        if (unconfirmed != null) {
            // Not a failure: switch the sheet to the code step. Null ends the spinner without a message.
            codeStep.postValue(new CodeStep(CodeStep.Kind.SIGN_UP, unconfirmed));
            authState.postValue(null);
            return;
        }
        if (result.isSuccess()) {
            unsyncedOnSignIn = getSyncStatusUseCase.unsyncedTransactions();
            // With data on the device, wait for the user's merge / discard choice before syncing,
            // and cancel a sync queued by an earlier edit so it can't merge before the user decides.
            if (unsyncedOnSignIn == 0) syncScheduler.requestSync();
            else syncScheduler.cancelAll();
        }
        authState.postValue(result);
    }
}
