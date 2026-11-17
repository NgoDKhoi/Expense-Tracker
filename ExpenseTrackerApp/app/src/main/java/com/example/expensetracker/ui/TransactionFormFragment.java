package com.example.expensetracker.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.usecase.DeleteCategoryUseCase;
import com.example.expensetracker.ui.widget.CaptureButtonView;
import com.example.expensetracker.util.CurrencyFormatter;
import com.example.expensetracker.viewmodel.AddTransactionViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class TransactionFormFragment extends Fragment {

    public static final String RESULT_TRANSACTION_UPDATED = "result_transaction_updated";
    public static final String RESULT_KEY_AMOUNT = "amount";
    public static final String RESULT_KEY_CATEGORY = "category";
    public static final String RESULT_KEY_IMAGE_URI = "imageUri";
    public static final String RESULT_KEY_TIMESTAMP = "timestamp";
    public static final String RESULT_KEY_NOTE = "note";
    public static final String RESULT_KEY_TYPE = "type";
    public static final String RESULT_KEY_WALLET_ID = "walletId";

    private static final String ARG_IMAGE_URI = "imageUri";
    private static final String ARG_WALLET_ID = "walletId";
    private static final String ARG_PREFILL_AMOUNT = "prefillAmount";
    private static final String ARG_PREFILL_TYPE = "prefillType";
    private static final String ARG_PREFILL_CATEGORY = "prefillCategory";
    private static final String ARG_PREFILL_NOTE = "prefillNote";
    private static final String ARG_DAY_OFFSET = "dayOffset";
    private static final String ARG_EDIT_ID = "editId";
    private static final String ARG_EDIT_UUID = "editUuid";
    private static final String ARG_EDIT_TIMESTAMP = "editTimestamp";
    private static final String ARG_EDIT_TITLE = "editTitle";

    private long editTransactionId = -1;
    private String editTransactionUuid;
    private String editTitle = "";
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
    private WalletModel currentSelectedWallet;
    private ExecutorService imageCopyExecutor;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                clearInputFocusAndHideKeyboard();
                if (uri != null) {
                    copySelectedImageAndRefresh(uri);
                }
            });

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
    private ImageButton btnRemoveReceiptPhoto;
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

    public static TransactionFormFragment newEditInstance(TransactionModel tx) {
        TransactionFormFragment fragment = new TransactionFormFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EDIT_ID, tx.getId());
        args.putString(ARG_EDIT_UUID, tx.getUuid());
        args.putString(ARG_EDIT_TITLE, tx.getTitle() != null ? tx.getTitle() : "");
        args.putLong(ARG_WALLET_ID, tx.getWalletId());
        args.putDouble(ARG_PREFILL_AMOUNT, tx.getAmount());
        args.putString(ARG_PREFILL_TYPE, tx.getType());
        args.putString(ARG_PREFILL_CATEGORY, tx.getCategory());
        args.putString(ARG_PREFILL_NOTE, tx.getNote());
        args.putString(ARG_IMAGE_URI, tx.getImageUri());
        if (tx.getTimestamp() != null) {
            args.putLong(ARG_EDIT_TIMESTAMP, tx.getTimestamp().getTime());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            editTransactionId = getArguments().getLong(ARG_EDIT_ID, -1);
            editTransactionUuid = getArguments().getString(ARG_EDIT_UUID);
            String titleArg = getArguments().getString(ARG_EDIT_TITLE);
            if (titleArg != null) {
                editTitle = titleArg;
            }
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
            long editTimestamp = getArguments().getLong(ARG_EDIT_TIMESTAMP, -1);
            if (editTimestamp > 0) {
                selectedDate = new Date(editTimestamp);
            } else {
                int dayOffset = getArguments().getInt(ARG_DAY_OFFSET, 0);
                if (dayOffset != 0) {
                    selectedDate = dateWithOffset(dayOffset);
                }
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
        btnRemoveReceiptPhoto = view.findViewById(R.id.btn_remove_receipt_photo);
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

        View llEditPhotoHint = view.findViewById(R.id.ll_edit_photo_hint);
        if (editTransactionId > 0) {
            if (llEditPhotoHint != null) {
                llEditPhotoHint.setVisibility(View.VISIBLE);
                llEditPhotoHint.setOnClickListener(v -> openGalleryPicker());
            }
            if (ivCapturedImage != null) {
                ivCapturedImage.setOnClickListener(v -> openGalleryPicker());
            }
            if (btnRemoveReceiptPhoto != null) {
                btnRemoveReceiptPhoto.setOnClickListener(v -> {
                    imageUri = null;
                    updatePreviewAndTypeToggleStyles();
                });
            }
        }
        updatePreviewAndTypeToggleStyles();

        if (editTransactionId <= 0) {
            etAmount.requestFocus();
        } else {
            clearInputFocusAndHideKeyboard();
        }
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
            updateWalletSelectorLabel(selected);
        });

        if (tvWalletSelector != null) {
            tvWalletSelector.setOnClickListener(v -> {
                if (cachedWallets.isEmpty()) {
                    Toast.makeText(requireContext(), "Please create a wallet first", Toast.LENGTH_SHORT).show();
                    return;
                }
                PopupMenu popup = new PopupMenu(requireContext(), tvWalletSelector);
                for (int i = 0; i < cachedWallets.size(); i++) {
                    WalletModel w = cachedWallets.get(i);
                    popup.getMenu().add(0, i, i, w.getName() + ": " + CurrencyFormatter.formatFull(requireContext(), w.getBalance()));
                }
                popup.setOnMenuItemClickListener(item -> {
                    WalletModel picked = cachedWallets.get(item.getItemId());
                    defaultWalletId = picked.getId();
                    updateWalletSelectorLabel(picked);
                    return true;
                });
                popup.show();
            });
        }
    }

    private void updateWalletSelectorLabel(WalletModel wallet) {
        if (tvWalletSelector != null && wallet != null && isAdded()) {
            currentSelectedWallet = wallet;
            tvWalletSelector.setText(wallet.getName() + ": "
                    + CurrencyFormatter.formatCompact(requireContext(), wallet.getBalance()) + " ▾");
            applyWalletSelectorStyle(wallet);
        }
    }

    private void applyWalletSelectorStyle(WalletModel wallet) {
        if (tvWalletSelector == null || wallet == null || !isAdded()) {
            return;
        }
        int walletColor = 0xFFFF5722;
        if (wallet.getColorHex() != null && !wallet.getColorHex().trim().isEmpty()) {
            try {
                walletColor = Color.parseColor(wallet.getColorHex());
            } catch (IllegalArgumentException ignored) {
            }
        }
        TextViewCompat.setCompoundDrawableTintList(tvWalletSelector, ColorStateList.valueOf(walletColor));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(999f);
        bg.setColor(0x99141414);
        int strokePx = Math.max(2, Math.round(1.5f * getResources().getDisplayMetrics().density));
        bg.setStroke(strokePx, walletColor);
        tvWalletSelector.setBackground(bg);
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
        if (editTransactionId > 0) {
            viewModel.updateExpense(editTransactionId, editTransactionUuid, amount, editTitle, selectedCategory,
                    imageUri, note, defaultWalletId, currentTransactionType, selectedDate);
            Bundle result = new Bundle();
            result.putDouble(RESULT_KEY_AMOUNT, amount);
            result.putString(RESULT_KEY_CATEGORY, selectedCategory);
            result.putString(RESULT_KEY_IMAGE_URI, imageUri);
            result.putLong(RESULT_KEY_TIMESTAMP, selectedDate.getTime());
            result.putString(RESULT_KEY_NOTE, note);
            result.putString(RESULT_KEY_TYPE, currentTransactionType);
            result.putLong(RESULT_KEY_WALLET_ID, defaultWalletId);
            getParentFragmentManager().setFragmentResult(RESULT_TRANSACTION_UPDATED, result);
            Toast.makeText(requireContext(), R.string.msg_transaction_updated, Toast.LENGTH_SHORT).show();
        } else {
            viewModel.saveExpense(amount, "", selectedCategory, imageUri, note, defaultWalletId, currentTransactionType, selectedDate);
            Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show();
        }
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void openGalleryPicker() {
        clearInputFocusAndHideKeyboard();
        pickImageLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        );
    }

    private void clearInputFocusAndHideKeyboard() {
        if (etAmount != null) {
            etAmount.clearFocus();
        }
        if (etNote != null) {
            etNote.clearFocus();
        }
        View root = getView();
        if (root != null) {
            root.requestFocus();
        }
        dismissKeyboard();
    }

    private void copySelectedImageAndRefresh(@NonNull Uri sourceUri) {
        if (!isAdded()) return;
        if (imageCopyExecutor == null || imageCopyExecutor.isShutdown()) {
            imageCopyExecutor = Executors.newSingleThreadExecutor();
        }
        final Context appContext = requireContext().getApplicationContext();
        imageCopyExecutor.execute(() -> {
            File outputDir = new File(appContext.getFilesDir(), "expense_images");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(new Date());
            File destFile = new File(outputDir, "IMG_" + ts + ".jpg");
            try (InputStream in = appContext.getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(destFile)) {
                if (in == null) {
                    throw new IOException("Null input stream");
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
                String newUri = Uri.fromFile(destFile).toString();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            imageUri = newUri;
                            updatePreviewAndTypeToggleStyles();
                            clearInputFocusAndHideKeyboard();
                            View root = getView();
                            if (root != null) {
                                root.post(this::clearInputFocusAndHideKeyboard);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.error_gallery_copy_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (imageCopyExecutor != null) {
            imageCopyExecutor.shutdown();
        }
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

        if (btnRemoveReceiptPhoto != null) {
            boolean hasCustomImage = editTransactionId > 0 && imageUri != null && !imageUri.trim().isEmpty();
            btnRemoveReceiptPhoto.setVisibility(hasCustomImage ? View.VISIBLE : View.GONE);
        }

        if (currentSelectedWallet != null) {
            applyWalletSelectorStyle(currentSelectedWallet);
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

            if (DeleteCategoryUseCase.isCustomCategory(category)) {
                chip.setOnLongClickListener(v -> {
                    confirmDeleteCategory(category);
                    return true;
                });
            }

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

    private void confirmDeleteCategory(CategoryModel category) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_delete_category_title, category.getName()))
                .setMessage(R.string.dialog_delete_category_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    viewModel.deleteCategory(category.getId());
                    if (category.getName() != null && category.getName().equalsIgnoreCase(selectedCategory)) {
                        selectedCategory = "Other";
                    }
                    Toast.makeText(requireContext(),
                            getString(R.string.msg_category_deleted, category.getName()),
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
