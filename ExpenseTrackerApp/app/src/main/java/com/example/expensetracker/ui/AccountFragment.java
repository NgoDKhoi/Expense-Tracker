package com.example.expensetracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

/** Shows the signed-in account and lets the user sign out. */
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

        view.findViewById(R.id.btn_sign_out).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dialog_sign_out_title)
                        .setMessage(R.string.dialog_sign_out_message)
                        .setNegativeButton(R.string.action_cancel, null)
                        .setPositiveButton(R.string.btn_sign_out, (dialog, which) -> {
                            viewModel.signOut();
                            Toast.makeText(requireContext(), R.string.msg_signed_out, Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().setFragmentResult(AuthFragment.RESULT_AUTH_CHANGED, Bundle.EMPTY);
                            close();
                        })
                        .show());
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
