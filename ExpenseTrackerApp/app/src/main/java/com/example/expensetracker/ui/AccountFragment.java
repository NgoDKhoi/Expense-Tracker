package com.example.expensetracker.ui;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.viewmodel.AuthViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import javax.inject.Inject;

/** Shows the signed-in account, its backup status, and lets the user sync or sign out. */
public class AccountFragment extends Fragment {

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(AuthViewModel.class);

        view.findViewById(R.id.btn_account_back).setOnClickListener(v -> close());

        UserModel user = viewModel.getCurrentUser();
        if (user == null) {
            close();
            return;
        }
        ((TextView) view.findViewById(R.id.tv_account_initial)).setText(initialOf(user));
        ((TextView) view.findViewById(R.id.tv_account_name)).setText(user.getDisplayName());
        String username = user.getUsername();
        ((TextView) view.findViewById(R.id.tv_account_email)).setText(
                username == null || username.isEmpty() ? user.getEmail() : "@" + username + "  ·  " + user.getEmail());

        TextView tvSyncStatus = view.findViewById(R.id.tv_sync_status);
        Button btnSyncNow = view.findViewById(R.id.btn_sync_now);
        Button btnSignOut = view.findViewById(R.id.btn_sign_out);

        viewModel.getSyncInfo().observe(getViewLifecycleOwner(), info -> tvSyncStatus.setText(describe(info)));
        viewModel.loadSyncInfo();

        btnSyncNow.setOnClickListener(v -> viewModel.syncNow());
        viewModel.getSyncNowState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            btnSyncNow.setEnabled(!result.isLoading());
            btnSyncNow.setText(result.isLoading() ? R.string.msg_syncing : R.string.btn_sync_now);
            if (result.isSuccess()) {
                Toast.makeText(requireContext(), R.string.msg_sync_done, Toast.LENGTH_SHORT).show();
            } else if (result.isError()) {
                Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnSignOut.setOnClickListener(v -> confirmSignOut());
        viewModel.getSignOutState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            btnSignOut.setEnabled(!result.isLoading());
            btnSyncNow.setEnabled(!result.isLoading());
            if (result.isSuccess()) {
                Toast.makeText(requireContext(), R.string.msg_signed_out, Toast.LENGTH_SHORT).show();
                getParentFragmentManager().setFragmentResult(AuthFragment.RESULT_AUTH_CHANGED, Bundle.EMPTY);
                close();
            } else if (result.isError()) {
                Toast.makeText(requireContext(), getString(R.string.msg_sign_out_sync_failed, result.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Warns when some changes are not backed up yet, since signing out wipes this device. */
    private void confirmSignOut() {
        AuthViewModel.SyncInfo info = viewModel.getSyncInfo().getValue();
        int pending = info != null ? info.pendingChanges : 0;
        if (pending > 0) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.dialog_sign_out_pending_title, pending))
                    .setMessage(R.string.dialog_sign_out_pending_message)
                    .setPositiveButton(R.string.action_sync_then_sign_out, (d, w) -> viewModel.signOut(true))
                    .setNegativeButton(R.string.action_sign_out_anyway, (d, w) -> viewModel.signOut(false))
                    .setNeutralButton(R.string.action_cancel, null)
                    .show();
        } else {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_sign_out_title)
                    .setMessage(R.string.dialog_sign_out_message)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.btn_sign_out, (d, w) -> viewModel.signOut(false))
                    .show();
        }
    }

    private String describe(AuthViewModel.SyncInfo info) {
        long now = System.currentTimeMillis();
        CharSequence when = now - info.lastSyncedAt < DateUtils.MINUTE_IN_MILLIS
                ? getString(R.string.sync_just_now) // instead of "0 minutes ago"
                : DateUtils.getRelativeTimeSpanString(info.lastSyncedAt, now, DateUtils.MINUTE_IN_MILLIS);
        String last = info.lastSyncedAt == 0 ? getString(R.string.sync_never) : getString(R.string.sync_last, when);
        String pending = info.pendingChanges == 0
                ? getString(R.string.sync_all_backed_up)
                : getString(R.string.sync_pending, info.pendingChanges);
        return last + "\n" + pending;
    }

    /** First letter of the display name (or email) in upper case, for the avatar. */
    static String initialOf(UserModel user) {
        String source = user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()
                ? user.getDisplayName().trim()
                : user.getEmail();
        if (source == null || source.isEmpty()) return "?";
        return source.substring(0, source.offsetByCodePoints(0, 1)).toUpperCase();
    }

    private void close() {
        getParentFragmentManager().popBackStack();
    }
}
