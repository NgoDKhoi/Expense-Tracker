package com.example.expensetracker.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.viewmodel.AddTransactionViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class TransactionFormFragment extends Fragment {

    private static final String ARG_IMAGE_URI = "imageUri";
    private static final String ARG_WALLET_ID = "walletId";

    private String imageUri;
    private String selectedCategory = "Other";
    private String currentTransactionType = "EXPENSE";
    private long defaultWalletId = -1;
    private Date selectedDate = new Date();

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private AddTransactionViewModel viewModel;

    private EditText etAmount;
    private EditText etNote;
    private MaterialButton btnDate;
    private MaterialButtonToggleGroup toggleTransactionType;
    private View svCategories;
    private ChipGroup chipGroupCategories;
    private View llPostCaptureActions;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static TransactionFormFragment newInstance(String imageUri, long walletId) {
        TransactionFormFragment fragment = new TransactionFormFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URI, imageUri);
        args.putLong(ARG_WALLET_ID, walletId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUri = getArguments().getString(ARG_IMAGE_URI);
            defaultWalletId = getArguments().getLong(ARG_WALLET_ID, -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(AddTransactionViewModel.class);

        ImageView ivCapturedImage = view.findViewById(R.id.iv_captured_image);
        if (imageUri != null) {
            ivCapturedImage.setImageURI(Uri.parse(imageUri));
        }

        etAmount = view.findViewById(R.id.et_amount);
        etNote = view.findViewById(R.id.et_note);
        btnDate = view.findViewById(R.id.btn_date);
        toggleTransactionType = view.findViewById(R.id.toggle_transaction_type);
        svCategories = view.findViewById(R.id.sv_categories);
        chipGroupCategories = view.findViewById(R.id.chip_group_categories);
        llPostCaptureActions = view.findViewById(R.id.ll_post_capture_actions);

        // Handle WindowInsets for Edge-to-Edge and Keyboard (IME)
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottomPadding = Math.max(imeInsets.bottom, systemBars.bottom);
            v.setPadding(0, systemBars.top, 0, bottomPadding);
            return WindowInsetsCompat.CONSUMED;
        });


        etAmount.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                etNote.requestFocus();
                return true;
            }
            return false;
        });

        etNote.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dismissKeyboard();
                etNote.clearFocus();
                return true;
            }
            return false;
        });

        setupDatePicker();
        setupTransactionTypeToggle();
        observeCategories();

        view.findViewById(R.id.btn_retake).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        com.example.expensetracker.ui.widget.CaptureButtonView btnSave = view.findViewById(R.id.btn_save);
        if (btnSave != null) {
            android.graphics.drawable.Drawable checkIcon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_check);
            btnSave.setCenterIcon(checkIcon, android.graphics.Color.parseColor("#FF5722"));
            btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                etAmount.setError("Amount is required");
                etAmount.requestFocus();
                return;
            }
            if (defaultWalletId == -1) {
                Toast.makeText(requireContext(), "Please select or create a wallet first", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid number");
                return;
            }

            String note = etNote.getText().toString().trim();
            viewModel.saveExpense(amount, "", selectedCategory, imageUri, note, defaultWalletId, currentTransactionType, selectedDate);
            Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        }

        etAmount.requestFocus();
    }

    private void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    private void setupDatePicker() {
        updateDateButtonText();
        btnDate.setOnClickListener(v -> {
            dismissKeyboard();
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Chọn ngày")
                    .setSelection(selectedDate.getTime())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    selectedDate = new Date(selection);
                    updateDateButtonText();
                }
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });
    }

    private void updateDateButtonText() {
        if (isSameDay(selectedDate, new Date())) {
            btnDate.setText(R.string.label_today);
        } else {
            btnDate.setText(dateFormat.format(selectedDate));
        }
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private void setupTransactionTypeToggle() {
        toggleTransactionType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_type_expense) {
                    currentTransactionType = "EXPENSE";
                } else if (checkedId == R.id.btn_type_income) {
                    currentTransactionType = "INCOME";
                }
                observeCategories();
            }
        });
    }

    private void observeCategories() {
        viewModel.getCategories(currentTransactionType).observe(getViewLifecycleOwner(), categories -> {
            renderCategoryChips(categories);
        });
    }

    private void renderCategoryChips(@Nullable List<CategoryModel> categories) {
        chipGroupCategories.removeAllViews();
        if (categories == null || categories.isEmpty()) {
            return;
        }

        boolean matchedSelection = false;
        for (CategoryModel category : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(category.getName());
            chip.setCheckable(true);
            chip.setTextColor(Color.WHITE);

            int colorInt;
            try {
                colorInt = Color.parseColor(category.getColorHex());
            } catch (Exception e) {
                colorInt = 0xFF607D8B; // fallback color
            }

            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
            };
            int[] backgroundColors = new int[]{
                    colorInt,
                    0x33252525
            };
            int[] strokeColors = new int[]{
                    colorInt,
                    0x55FFFFFF
            };
            chip.setChipBackgroundColor(new ColorStateList(states, backgroundColors));
            chip.setChipStrokeColor(new ColorStateList(states, strokeColors));
            chip.setChipStrokeWidth(2f);

            if (category.getName().equalsIgnoreCase(selectedCategory)) {
                chip.setChecked(true);
                matchedSelection = true;
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedCategory = category.getName();
                }
            });

            chipGroupCategories.addView(chip);
        }

        // Default to first category if current selectedCategory doesn't match
        if (!matchedSelection && !categories.isEmpty()) {
            Chip firstChip = (Chip) chipGroupCategories.getChildAt(0);
            if (firstChip != null) {
                firstChip.setChecked(true);
                selectedCategory = categories.get(0).getName();
            }
        }

        // Add special "+ Tạo mới" action chip at the end
        Chip addChip = new Chip(requireContext());
        addChip.setText(R.string.btn_add_category);
        addChip.setCheckable(false);
        addChip.setChipIconResource(R.drawable.ic_add);
        addChip.setChipIconTint(ColorStateList.valueOf(Color.WHITE));
        addChip.setTextColor(Color.WHITE);
        addChip.setChipBackgroundColor(ColorStateList.valueOf(0x33FF5722));
        addChip.setChipStrokeColor(ColorStateList.valueOf(0xFFFF5722));
        addChip.setChipStrokeWidth(2f);
        addChip.setOnClickListener(v -> showAddCategoryBottomSheet());
        chipGroupCategories.addView(addChip);
    }

    private void showAddCategoryBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        dialog.setContentView(dialogView);

        TextInputEditText etName = dialogView.findViewById(R.id.et_new_category_name);
        MaterialButtonToggleGroup typeToggle = dialogView.findViewById(R.id.toggle_dialog_type);
        ChipGroup colorGroup = dialogView.findViewById(R.id.chip_group_colors);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_category);
        MaterialButton btnCreate = dialogView.findViewById(R.id.btn_create_category);

        if ("INCOME".equals(currentTransactionType)) {
            typeToggle.check(R.id.btn_dialog_type_income);
        } else {
            typeToggle.check(R.id.btn_dialog_type_expense);
        }

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
            int c = Color.parseColor(hex);
            colorChip.setChipBackgroundColor(ColorStateList.valueOf(c));
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
            viewModel.addCategory(name, type, selectedColor[0], "custom");
            selectedCategory = name;
            dialog.dismiss();
            Toast.makeText(requireContext(), "Đã tạo: " + name, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}
