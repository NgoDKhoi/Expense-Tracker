package com.example.expensetracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.viewmodel.AuthViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import javax.inject.Inject;

/** Sign in / sign up screen, opened from the avatar button on the camera screen. */
public class AuthFragment extends Fragment {

    /** Fragment result key sent whenever the signed-in user changes. */
    public static final String RESULT_AUTH_CHANGED = "auth_changed";

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private AuthViewModel viewModel;

    private boolean signUpMode = false;

    private TextInputLayout tilName;
    private TextInputLayout tilConfirm;
    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirm;
    private MaterialButton btnSubmit;
    private View btnForgot;
    private View progress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(AuthViewModel.class);

        tilName = view.findViewById(R.id.til_display_name);
        tilConfirm = view.findViewById(R.id.til_confirm_password);
        etName = view.findViewById(R.id.et_display_name);
        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        etConfirm = view.findViewById(R.id.et_confirm_password);
        btnSubmit = view.findViewById(R.id.btn_auth_submit);
        btnForgot = view.findViewById(R.id.btn_forgot_password);
        progress = view.findViewById(R.id.progress_auth);

        view.findViewById(R.id.btn_auth_back).setOnClickListener(v -> close());

        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_auth_mode);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) setSignUpMode(checkedId == R.id.btn_mode_sign_up);
        });
        setSignUpMode(false);

        btnSubmit.setOnClickListener(v -> submit());
        btnForgot.setOnClickListener(v -> {
            clearErrors();
            viewModel.resetPassword(text(etEmail));
        });

        viewModel.getAuthState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            setLoading(result.isLoading());
            if (result.isSuccess()) {
                Toast.makeText(requireContext(),
                        signUpMode ? R.string.msg_sign_up_success : R.string.msg_sign_in_success,
                        Toast.LENGTH_SHORT).show();
                getParentFragmentManager().setFragmentResult(RESULT_AUTH_CHANGED, Bundle.EMPTY);
                close();
            } else if (result.isError()) {
                showError(result.getMessage());
            }
        });

        viewModel.getResetState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            setLoading(result.isLoading());
            if (result.isSuccess()) {
                Snackbar.make(view, getString(R.string.msg_reset_sent, text(etEmail)), Snackbar.LENGTH_LONG).show();
            } else if (result.isError()) {
                showError(result.getMessage());
            }
        });
    }

    private void setSignUpMode(boolean signUp) {
        signUpMode = signUp;
        int visibleInSignUp = signUp ? View.VISIBLE : View.GONE;
        tilName.setVisibility(visibleInSignUp);
        tilConfirm.setVisibility(visibleInSignUp);
        btnForgot.setVisibility(signUp ? View.GONE : View.VISIBLE);
        btnSubmit.setText(signUp ? R.string.auth_btn_sign_up : R.string.auth_tab_sign_in);
        clearErrors();
    }

    private void submit() {
        clearErrors();
        if (signUpMode) {
            if (!text(etPassword).equals(text(etConfirm))) {
                tilConfirm.setError(getString(R.string.error_password_mismatch));
                return;
            }
            viewModel.signUp(text(etName), text(etEmail), text(etPassword));
        } else {
            viewModel.signIn(text(etEmail), text(etPassword));
        }
    }

    private void showError(String message) {
        if (message != null && getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void clearErrors() {
        tilConfirm.setError(null);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
        btnForgot.setEnabled(!loading);
    }

    private void close() {
        getParentFragmentManager().popBackStack();
    }

    private static String text(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
}
