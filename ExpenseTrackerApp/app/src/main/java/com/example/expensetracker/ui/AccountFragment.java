package com.example.expensetracker.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.usecase.DeleteCategoryUseCase;
import com.example.expensetracker.util.CurrencyFormatter;
import com.example.expensetracker.viewmodel.AuthViewModel;
import com.example.expensetracker.viewmodel.HomeViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Unified Account & App Settings screen: shows signed-in profile/sync or Guest sign-in CTA,
 * plus Currency Switcher (₫ / $ / €) and Category Management.
 */
public class AccountFragment extends Fragment {

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private AuthViewModel authViewModel;
    private HomeViewModel homeViewModel;

    private TextView tvAccountInitial;
    private TextView tvAccountName;
    private TextView tvAccountEmail;
    private Button btnSignIn;
    private View llSyncSection;
    private TextView tvSyncStatus;
    private Button btnSyncNow;
    private Button btnSignOut;
    private TextView tvCurrencyValue;
    private TextView tvCurrencyBadge;
    private TextView tvCategoriesCount;

    private List<CategoryModel> latestCategories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        authViewModel = new ViewModelProvider(this, viewModelFactory).get(AuthViewModel.class);
        homeViewModel = new ViewModelProvider(this, viewModelFactory).get(HomeViewModel.class);

        view.findViewById(R.id.btn_account_back).setOnClickListener(v -> close());

        tvAccountInitial = view.findViewById(R.id.tv_account_initial);
        tvAccountName = view.findViewById(R.id.tv_account_name);
        tvAccountEmail = view.findViewById(R.id.tv_account_email);
        btnSignIn = view.findViewById(R.id.btn_account_sign_in);
        llSyncSection = view.findViewById(R.id.ll_account_sync_section);
        tvSyncStatus = view.findViewById(R.id.tv_sync_status);
        btnSyncNow = view.findViewById(R.id.btn_sync_now);
        btnSignOut = view.findViewById(R.id.btn_sign_out);
        tvCurrencyValue = view.findViewById(R.id.tv_setting_currency_value);
        tvCurrencyBadge = view.findViewById(R.id.tv_setting_currency_badge);
        tvCategoriesCount = view.findViewById(R.id.tv_setting_categories_count);

        bindUserSection();
        renderCurrencySummary();

        // Listen via childFragmentManager so AddTransactionFragment's listener on parent FragmentManager is untouched.
        getChildFragmentManager().setFragmentResultListener(
                AuthFragment.RESULT_AUTH_CHANGED, getViewLifecycleOwner(),
                (key, result) -> {
                    bindUserSection();
                    getParentFragmentManager().setFragmentResult(AuthFragment.RESULT_AUTH_CHANGED, Bundle.EMPTY);
                });

        btnSignIn.setOnClickListener(v ->
                new AuthFragment().show(getChildFragmentManager(), AuthFragment.TAG));

        authViewModel.getSyncInfo().observe(getViewLifecycleOwner(), info -> {
            if (info != null) {
                tvSyncStatus.setText(describe(info));
            }
        });

        btnSyncNow.setOnClickListener(v -> authViewModel.syncNow());
        authViewModel.getSyncNowState().observe(getViewLifecycleOwner(), result -> {
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
        authViewModel.getSignOutState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            btnSignOut.setEnabled(!result.isLoading());
            btnSyncNow.setEnabled(!result.isLoading());
            if (result.isSuccess()) {
                Toast.makeText(requireContext(), R.string.msg_signed_out, Toast.LENGTH_SHORT).show();
                getParentFragmentManager().setFragmentResult(AuthFragment.RESULT_AUTH_CHANGED, Bundle.EMPTY);
                bindUserSection();
            } else if (result.isError()) {
                Toast.makeText(requireContext(), getString(R.string.msg_sign_out_sync_failed, result.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });

        homeViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            latestCategories = categories != null ? categories : new ArrayList<>();
            tvCategoriesCount.setText(getString(R.string.settings_categories_count, latestCategories.size()));
        });

        view.findViewById(R.id.row_setting_currency).setOnClickListener(v -> showCurrencyPickerDialog());
        view.findViewById(R.id.row_setting_categories).setOnClickListener(v -> showManageCategoriesDialog());
    }

    private void bindUserSection() {
        UserModel user = authViewModel.getCurrentUser();
        if (user == null) {
            tvAccountInitial.setText("?");
            tvAccountName.setText(R.string.account_guest_name);
            tvAccountEmail.setText(R.string.auth_guest_note);
            btnSignIn.setVisibility(View.VISIBLE);
            llSyncSection.setVisibility(View.GONE);
            btnSignOut.setVisibility(View.GONE);
        } else {
            tvAccountInitial.setText(initialOf(user));
            tvAccountName.setText(user.getDisplayName());
            String username = user.getUsername();
            tvAccountEmail.setText(
                    username == null || username.isEmpty() ? user.getEmail() : "@" + username + "  ·  " + user.getEmail());
            btnSignIn.setVisibility(View.GONE);
            llSyncSection.setVisibility(View.VISIBLE);
            btnSignOut.setVisibility(View.VISIBLE);
            authViewModel.loadSyncInfo();
        }
    }

    private void renderCurrencySummary() {
        String symbol = CurrencyFormatter.getSymbol(requireContext());
        tvCurrencyBadge.setText(symbol);
        if (CurrencyFormatter.SYMBOL_USD.equals(symbol)) {
            tvCurrencyValue.setText(R.string.currency_option_usd);
        } else if (CurrencyFormatter.SYMBOL_EUR.equals(symbol)) {
            tvCurrencyValue.setText(R.string.currency_option_eur);
        } else {
            tvCurrencyValue.setText(R.string.currency_option_vnd);
        }
    }

    private void showCurrencyPickerDialog() {
        final String[] symbols = {
                CurrencyFormatter.SYMBOL_VND,
                CurrencyFormatter.SYMBOL_USD,
                CurrencyFormatter.SYMBOL_EUR
        };
        final String[] labels = {
                getString(R.string.currency_option_vnd),
                getString(R.string.currency_option_usd),
                getString(R.string.currency_option_eur)
        };
        String current = CurrencyFormatter.getSymbol(requireContext());
        int checkedItem = 0;
        for (int i = 0; i < symbols.length; i++) {
            if (symbols[i].equals(current)) {
                checkedItem = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_currency_title)
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    String chosen = symbols[which];
                    CurrencyFormatter.setSymbol(requireContext(), chosen);
                    renderCurrencySummary();
                    dialog.dismiss();
                    Toast.makeText(requireContext(),
                            getString(R.string.msg_currency_updated, chosen),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showManageCategoriesDialog() {
        List<CategoryModel> categories = new ArrayList<>(latestCategories);
        CharSequence[] items = new CharSequence[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            CategoryModel cat = categories.get(i);
            boolean custom = DeleteCategoryUseCase.isCustomCategory(cat);
            items[i] = cat.getName() + " [" + cat.getType() + "]" + (custom ? " • Tự tạo (xoá)" : " • Mặc định");
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_manage_categories_title)
                .setPositiveButton(R.string.action_add_category, (d, w) -> showAddCategoryBottomSheet())
                .setNegativeButton(R.string.action_cancel, null);

        if (!categories.isEmpty()) {
            builder.setItems(items, (dialog, which) -> {
                CategoryModel picked = categories.get(which);
                if (DeleteCategoryUseCase.isCustomCategory(picked)) {
                    confirmDeleteCategory(picked);
                } else {
                    Toast.makeText(requireContext(), R.string.msg_default_category_protected, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            builder.setMessage(R.string.label_category);
        }
        builder.show();
    }

    private void confirmDeleteCategory(CategoryModel category) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_delete_category_title, category.getName()))
                .setMessage(R.string.dialog_delete_category_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    homeViewModel.deleteCategory(category.getId());
                    Toast.makeText(requireContext(),
                            getString(R.string.msg_category_deleted, category.getName()),
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showAddCategoryBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        dialog.setContentView(dialogView);

        TextInputEditText etName = dialogView.findViewById(R.id.et_new_category_name);
        MaterialButtonToggleGroup typeToggle = dialogView.findViewById(R.id.toggle_dialog_type);
        ChipGroup colorGroup = dialogView.findViewById(R.id.chip_group_colors);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_category);
        MaterialButton btnCreate = dialogView.findViewById(R.id.btn_create_category);

        final String[] presetColors = {
                "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
                "#E91E63", "#00BCD4", "#FF5722", "#607D8B"
        };
        final String[] selectedColor = {presetColors[0]};

        for (int i = 0; i < presetColors.length; i++) {
            String hex = presetColors[i];
            Chip colorChip = new Chip(requireContext());
            colorChip.setCheckable(true);
            colorChip.setText("   ");
            colorChip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor(hex)));
            colorChip.setChipStrokeColor(ColorStateList.valueOf(Color.WHITE));
            colorChip.setChipStrokeWidth(3f);
            if (i == 0) {
                colorChip.setChecked(true);
            }
            colorChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedColor[0] = hex;
                }
            });
            colorGroup.addView(colorChip);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnCreate.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                etName.setError(getString(R.string.error_category_name_required));
                return;
            }
            String type = typeToggle.getCheckedButtonId() == R.id.btn_dialog_type_income ? "INCOME" : "EXPENSE";
            homeViewModel.addCategory(name, type, selectedColor[0], "custom");
            dialog.dismiss();
            Toast.makeText(requireContext(), "Đã tạo: " + name, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    /** Warns when some changes are not backed up yet, since signing out wipes this device. */
    private void confirmSignOut() {
        AuthViewModel.SyncInfo info = authViewModel.getSyncInfo().getValue();
        int pending = info != null ? info.pendingChanges : 0;
        if (pending > 0) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.dialog_sign_out_pending_title, pending))
                    .setMessage(R.string.dialog_sign_out_pending_message)
                    .setPositiveButton(R.string.action_sync_then_sign_out, (d, w) -> authViewModel.signOut(true))
                    .setNegativeButton(R.string.action_sign_out_anyway, (d, w) -> authViewModel.signOut(false))
                    .setNeutralButton(R.string.action_cancel, null)
                    .show();
        } else {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_sign_out_title)
                    .setMessage(R.string.dialog_sign_out_message)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.btn_sign_out, (d, w) -> authViewModel.signOut(false))
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
