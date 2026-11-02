package com.example.expensetracker.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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

    private final SignInUseCase signInUseCase;
    private final SignUpUseCase signUpUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final SignOutUseCase signOutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final ExecutorService executor;
    private final ISyncScheduler syncScheduler;
    private final SyncDataUseCase syncDataUseCase;
    private final GetSyncStatusUseCase getSyncStatusUseCase;

    private final MutableLiveData<Result<UserModel>> authState = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> resetState = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> signOutState = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> syncNowState = new MutableLiveData<>();
    private final MutableLiveData<SyncInfo> syncInfo = new MutableLiveData<>();
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
                         GetSyncStatusUseCase getSyncStatusUseCase) {
        this.signInUseCase = signInUseCase;
        this.signUpUseCase = signUpUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.signOutUseCase = signOutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.executor = executor;
        this.syncScheduler = syncScheduler;
        this.syncDataUseCase = syncDataUseCase;
        this.getSyncStatusUseCase = getSyncStatusUseCase;
    }

    public LiveData<Result<UserModel>> getAuthState() {
        return authState;
    }

    public LiveData<Result<Void>> getResetState() {
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

    /** Signed-in user from the saved session, or null for guests. */
    public UserModel getCurrentUser() {
        return getCurrentUserUseCase.execute();
    }

    public void signIn(String emailOrUsername, String password) {
        authState.setValue(Result.loading());
        executor.execute(() -> publishAuth(signInUseCase.execute(emailOrUsername, password)));
    }

    public void signUp(String displayName, String username, String email, String password) {
        authState.setValue(Result.loading());
        executor.execute(() -> publishAuth(signUpUseCase.execute(displayName, username, email, password)));
    }

    public void resetPassword(String emailOrUsername) {
        resetState.setValue(Result.loading());
        executor.execute(() -> resetState.postValue(resetPasswordUseCase.execute(emailOrUsername)));
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

    private void publishAuth(Result<UserModel> result) {
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
