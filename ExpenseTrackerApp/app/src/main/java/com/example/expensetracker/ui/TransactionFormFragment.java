package com.example.expensetracker.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.ui.widget.CaptureButtonView;
import com.example.expensetracker.viewmodel.AddTransactionViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class TransactionFormFragment extends Fragment {

    private static final String ARG_IMAGE_URI = "imageUri";
    private static final String ARG_WALLET_ID = "walletId";
    private static final String ARG_PREFILL_AMOUNT = "prefillAmount";
    private static final String ARG_PREFILL_TYPE = "prefillType";
    private static final String ARG_PREFILL_CATEGORY = "prefillCategory";
    private static final String ARG_PREFILL_NOTE = "prefillNote";
    private static final String ARG_DAY_OFFSET = "dayOffset";

    private String imageUri;
    private String selectedCategory = "Other";
    private String currentTransactionType = "EXPENSE";
    private long defaultWalletId = -1;
    private double prefillAmount = 0.0;
    private String prefillNote;
    private Date selectedDate = new Date();
    private List<CategoryModel> cachedExpenseCategories = new ArrayList<>();
    private List<CategoryModel> cachedIncomeCategories = new ArrayList<>();
    private List<WalletModel> cachedWallets = new ArrayList<>();

    private final ActivityResultLauncher<Intent> formVoiceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty() && matches.get(0) != null && !matches.get(0).trim().isEmpty()) {
                        applySpokenExpense(matches.get(0));
                    }
                }
            });

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private AddTransactionViewModel viewModel;

    private ImageView ivCapturedImage;
    private MaterialButton btnTypeExpense;
    private MaterialButton btnTypeIncome;
    private TextView tvWalletSelector;
    private EditText etAmount;
    private EditText etNote;
    private MaterialButton btnDate;
    private CaptureButtonView btnSave;
    private ChipGroup chipGroupCategories;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static TransactionFormFragment newInstance(String imageUri, long walletId) {
        TransactionFormFragment fragment = new TransactionFormFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URI, imageUri);
        args.putLong(ARG_WALLET_ID, walletId);
        fragment.setArguments(args);
        return fragment;
    }

    public static TransactionFormFragment newVoiceInstance(long walletId,
                                                           double prefillAmount,
                                                           String prefillType,
                                                           String prefillCategory,
                                                           String prefillNote) {
        return newVoiceInstance(walletId, prefillAmount, prefillType, prefillCategory, prefillNote, 0);
    }

    public static TransactionFormFragment newVoiceInstance(long walletId,
                                                           double prefillAmount,
                                                           String prefillType,
                                                           String prefillCategory,
                                                           String prefillNote,
                                                           int dayOffset) {
        TransactionFormFragment fragment = new TransactionFormFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_WALLET_ID, walletId);
        args.putDouble(ARG_PREFILL_AMOUNT, prefillAmount);
        args.putString(ARG_PREFILL_TYPE, prefillType);
        args.putString(ARG_PREFILL_CATEGORY, prefillCategory);
        args.putString(ARG_PREFILL_NOTE, prefillNote);
        args.putInt(ARG_DAY_OFFSET, dayOffset);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUri = getArguments().getString(ARG_IMAGE_URI);
            defaultWalletId = getArguments().getLong(ARG_WALLET_ID, -1);
            prefillAmount = getArguments().getDouble(ARG_PREFILL_AMOUNT, 0.0);
            String typeArg = getArguments().getString(ARG_PREFILL_TYPE);
            if (typeArg != null && !typeArg.isEmpty()) {
                currentTransactionType = typeArg;
            }
            String catArg = getArguments().getString(ARG_PREFILL_CATEGORY);
            if (catArg != null && !catArg.isEmpty()) {
                selectedCategory = catArg;
            }
            prefillNote = getArguments().getString(ARG_PREFILL_NOTE);
            int dayOffset = getArguments().getInt(ARG_DAY_OFFSET, 0);
            if (dayOffset != 0) {
                selectedDate = dateWithOffset(dayOffset);
            }
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

        ivCapturedImage = view.findViewById(R.id.iv_captured_image);
        etAmount = view.findViewById(R.id.et_amount);
        etNote = view.findViewById(R.id.et_note);
        btnDate = view.findViewById(R.id.btn_date);
        btnTypeExpense = view.findViewById(R.id.btn_type_expense);
        btnTypeIncome = view.findViewById(R.id.btn_type_income);
        tvWalletSelector = view.findViewById(R.id.tv_form_wallet_selector);
        chipGroupCategories = view.findViewById(R.id.chip_group_categories);
        btnSave = view.findViewById(R.id.btn_save);

        updatePreviewAndTypeToggleStyles();

        if (prefillAmount > 0) {
            String rawFormatted = prefillAmount == Math.floor(prefillAmount)
                    ? String.valueOf((long) prefillAmount)
                    : String.valueOf(prefillAmount);
            etAmount.setText(rawFormatted);
            etAmount.setSelection(etAmount.getText().length());
        }
        if (prefillNote != null && !prefillNote.isEmpty()) {
            etNote.setText(prefillNote);
        }

        // Handle WindowInsets for Edge-to-Edge and Keyboard (IME)
        View llPostCaptureActions = view.findViewById(R.id.ll_post_capture_actions);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            boolean keyboardOpen = imeInsets.bottom > systemBars.bottom;

            if (llPostCaptureActions != null) {
                llPostCaptureActions.setVisibility(keyboardOpen ? View.GONE : View.VISIBLE);
            }
            int bottomPadding = Math.max(imeInsets.bottom, systemBars.bottom);
            v.setPadding(0, systemBars.top, 0, bottomPadding);
            return WindowInsetsCompat.CONSUMED;
        });

        etAmount.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                dismissKeyboard();
                etAmount.clearFocus();
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

        setupWalletSelector();
        setupDatePicker();
        setupTransactionTypeToggle();
        viewModel.getCategories("EXPENSE").observe(getViewLifecycleOwner(), cats ->
                cachedExpenseCategories = cats != null ? cats : new ArrayList<>());
        viewModel.getCategories("INCOME").observe(getViewLifecycleOwner(), cats ->
                cachedIncomeCategories = cats != null ? cats : new ArrayList<>());
        observeCategories();

        view.findViewById(R.id.btn_retake).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        View btnFormVoice = view.findViewById(R.id.btn_form_voice);
        if (btnFormVoice != null) {
            btnFormVoice.setOnClickListener(v -> {
                dismissKeyboard();
                launchFormVoiceInput();
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> handleSaveClicked());
        }

        etAmount.requestFocus();
    }

    private void setupWalletSelector() {
        viewModel.getWallets().observe(getViewLifecycleOwner(), wallets -> {
            if (wallets == null || wallets.isEmpty()) {
                cachedWallets = new ArrayList<>();
                defaultWalletId = -1;
                if (tvWalletSelector != null) {
                    tvWalletSelector.setText("+ Wallet");
                }
                return;
            }
            cachedWallets = wallets;
            WalletModel selected = wallets.get(0);
            if (defaultWalletId != -1) {
                for (WalletModel w : wallets) {
                    if (w.getId() == defaultWalletId) {
                        selected = w;
                        break;
                    }
                }
            }
            defaultWalletId = selected.getId();
            if (tvWalletSelector != null) {
                tvWalletSelector.setText(selected.getName() + " ▾");
            }
        });

        if (tvWalletSelector != null) {
            tvWalletSelector.setOnClickListener(v -> {
                if (cachedWallets.isEmpty()) {
                    Toast.makeText(requireContext(), "Please create a wallet first", Toast.LENGTH_SHORT).show();
                    return;
                }
                PopupMenu popup = new PopupMenu(requireContext(), tvWalletSelector);
                NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
                format.setMinimumFractionDigits(0);
                format.setMaximumFractionDigits(0);
                for (int i = 0; i < cachedWallets.size(); i++) {
                    WalletModel w = cachedWallets.get(i);
                    popup.getMenu().add(0, i, i, w.getName() + ": " + format.format(w.getBalance()) + " ₫");
                }
                popup.setOnMenuItemClickListener(item -> {
                    WalletModel picked = cachedWallets.get(item.getItemId());
                    defaultWalletId = picked.getId();
                    tvWalletSelector.setText(picked.getName() + " ▾");
                    return true;
                });
                popup.show();
            });
        }
    }

    private void handleSaveClicked() {
        String amountStr = etAmount.getText().toString().replace(",", "").trim();
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
        if (amount <= 0) {
            etAmount.setError("Amount must be greater than 0");
            return;
        }

        String note = etNote.getText().toString().trim();
        viewModel.saveExpense(amount, "", selectedCategory, imageUri, note, defaultWalletId, currentTransactionType, selectedDate);
        Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void updatePreviewAndTypeToggleStyles() {
        boolean isIncome = "INCOME".equalsIgnoreCase(currentTransactionType);
        int activeColor = Color.parseColor(isIncome ? "#4CAF50" : "#FF5722");

        if (imageUri != null && !imageUri.trim().isEmpty()) {
            ivCapturedImage.setPadding(0, 0, 0, 0);
            ivCapturedImage.setImageTintList(null);
            ivCapturedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivCapturedImage.setImageURI(Uri.parse(imageUri));
        } else {
            ivCapturedImage.setPadding(0, 0, 0, 0);
            ivCapturedImage.setImageTintList(null);
            ivCapturedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivCapturedImage.setImageResource(isIncome
                    ? R.drawable.bg_default_receipt_income
                    : R.drawable.bg_default_receipt_expense);
        }

        if (btnTypeExpense != null && btnTypeIncome != null) {
            int inactiveTextColor = Color.parseColor("#B0B0B0");
            btnTypeExpense.setChecked(!isIncome);
            btnTypeExpense.setBackgroundTintList(ColorStateList.valueOf(isIncome ? Color.TRANSPARENT : activeColor));
            btnTypeExpense.setTextColor(isIncome ? inactiveTextColor : Color.WHITE);

            btnTypeIncome.setChecked(isIncome);
            btnTypeIncome.setBackgroundTintList(ColorStateList.valueOf(isIncome ? activeColor : Color.TRANSPARENT));
            btnTypeIncome.setTextColor(isIncome ? Color.WHITE : inactiveTextColor);
        }

        if (btnSave != null) {
            Drawable checkIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check);
            btnSave.setCenterIcon(checkIcon, activeColor);
        }

        if (btnDate != null) {
            btnDate.setIconTint(ColorStateList.valueOf(activeColor));
        }
    }

    private void setupTransactionTypeToggle() {
        btnTypeExpense.setOnClickListener(v -> switchTransactionType("EXPENSE"));
        btnTypeIncome.setOnClickListener(v -> switchTransactionType("INCOME"));
    }

    private void switchTransactionType(String newType) {
        if (newType.equalsIgnoreCase(currentTransactionType)) {
            return;
        }
        currentTransactionType = newType;
        updatePreviewAndTypeToggleStyles();
        observeCategories();
    }

    private static Date dateWithOffset(int dayOffset) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, dayOffset);
        return cal.getTime();
    }

    private void launchFormVoiceInput() {
        try {
            formVoiceLauncher.launch(VoiceInputHelper.createSpeechIntent(requireContext()));
        } catch (ActivityNotFoundException e) {
            VoiceInputHelper.showTextFallbackDialog(requireContext(), this::applySpokenExpense);
        }
    }

    private void applySpokenExpense(String spokenText) {
        List<CategoryModel> combined = new ArrayList<>(cachedExpenseCategories);
        combined.addAll(cachedIncomeCategories);
        AddTransactionViewModel.ParsedVoiceExpense parsed =
                AddTransactionViewModel.parseVoiceInput(spokenText, combined);

        if (parsed.amount > 0) {
            String raw = parsed.amount == Math.floor(parsed.amount)
                    ? String.valueOf((long) parsed.amount)
                    : String.valueOf(parsed.amount);
            etAmount.setText(raw);
            etAmount.setSelection(etAmount.getText().length());
        }
        if (parsed.note != null && !parsed.note.isEmpty()) {
            etNote.setText(parsed.note);
        }
        selectedDate = dateWithOffset(parsed.dayOffset);
        updateDateButtonText();

        currentTransactionType = parsed.type;
        selectedCategory = parsed.category;
        updatePreviewAndTypeToggleStyles();
        observeCategories();
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
                colorInt = 0xFF607D8B;
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

        if (!matchedSelection && !categories.isEmpty()) {
            Chip firstChip = (Chip) chipGroupCategories.getChildAt(0);
            if (firstChip != null) {
                firstChip.setChecked(true);
                selectedCategory = categories.get(0).getName();
            }
        }

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
