package com.example.expensetracker.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.usecase.DeleteTransactionUseCase;
import com.example.expensetracker.core.domain.usecase.GetCategoriesUseCase;
import com.example.expensetracker.util.CurrencyFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class ReceiptDetailFragment extends Fragment {

    private static final int MENU_ID_EDIT = 1;
    private static final int MENU_ID_SAVE_IMAGE = 2;
    private static final int MENU_ID_DELETE = 3;

    private static final String ARG_ID = "arg_id";
    private static final String ARG_UUID = "arg_uuid";
    private static final String ARG_WALLET_ID = "arg_wallet_id";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_AMOUNT = "arg_amount";
    private static final String ARG_CATEGORY = "arg_category";
    private static final String ARG_IMAGE_URI = "arg_image_uri";
    private static final String ARG_TIMESTAMP = "arg_timestamp";
    private static final String ARG_NOTE = "arg_note";
    private static final String ARG_TYPE = "arg_type";

    @Inject
    GetCategoriesUseCase getCategoriesUseCase;
    @Inject
    DeleteTransactionUseCase deleteTransactionUseCase;

    private long transactionId = -1;
    private String transactionUuid;
    private long walletId = -1;
    private String title = "";
    private double amount;
    private String category;
    private String imageUri;
    private long timestamp;
    private String note;
    private String type = "EXPENSE";
    private List<CategoryModel> latestCategories = new ArrayList<>();

    private ImageView ivReceiptImage;
    private View viewBottomScrim;
    private TextView tvCategory;
    private TextView tvAmount;
    private TextView tvDate;
    private TextView tvNote;
    private MaterialButton btnBackToCamera;
    private ExecutorService saveImageExecutor;

    public static ReceiptDetailFragment newInstance(TransactionModel tx) {
        ReceiptDetailFragment fragment = new ReceiptDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, tx.getId());
        args.putString(ARG_UUID, tx.getUuid());
        args.putLong(ARG_WALLET_ID, tx.getWalletId());
        args.putString(ARG_TITLE, tx.getTitle() != null ? tx.getTitle() : "");
        args.putDouble(ARG_AMOUNT, tx.getAmount());
        args.putString(ARG_CATEGORY, tx.getCategory());
        args.putString(ARG_IMAGE_URI, tx.getImageUri());
        args.putLong(ARG_TIMESTAMP, tx.getTimestamp() != null ? tx.getTimestamp().getTime() : System.currentTimeMillis());
        args.putString(ARG_NOTE, tx.getNote());
        args.putString(ARG_TYPE, tx.getType() != null ? tx.getType() : "EXPENSE");
        fragment.setArguments(args);
        return fragment;
    }

    public static ReceiptDetailFragment newInstance(double amount, String category, String imageUri, long timestamp, String note) {
        return newInstance(amount, category, imageUri, timestamp, note, "EXPENSE");
    }

    public static ReceiptDetailFragment newInstance(double amount, String category, String imageUri, long timestamp, String note, String type) {
        ReceiptDetailFragment fragment = new ReceiptDetailFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_AMOUNT, amount);
        args.putString(ARG_CATEGORY, category);
        args.putString(ARG_IMAGE_URI, imageUri);
        args.putLong(ARG_TIMESTAMP, timestamp);
        args.putString(ARG_NOTE, note);
        args.putString(ARG_TYPE, type != null ? type : "EXPENSE");
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            transactionId = getArguments().getLong(ARG_ID, -1);
            transactionUuid = getArguments().getString(ARG_UUID);
            walletId = getArguments().getLong(ARG_WALLET_ID, -1);
            title = getArguments().getString(ARG_TITLE, "");
            amount = getArguments().getDouble(ARG_AMOUNT);
            category = getArguments().getString(ARG_CATEGORY);
            imageUri = getArguments().getString(ARG_IMAGE_URI);
            timestamp = getArguments().getLong(ARG_TIMESTAMP);
            note = getArguments().getString(ARG_NOTE);
            type = getArguments().getString(ARG_TYPE, "EXPENSE");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_receipt_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);

        final boolean insideCameraHost = getParentFragment() instanceof CameraHostFragment;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int topPadding = insideCameraHost ? 0 : systemBars.top;
            v.setPadding(0, topPadding, 0, systemBars.bottom);
            return insets;
        });

        ivReceiptImage = view.findViewById(R.id.iv_receipt_image);
        viewBottomScrim = view.findViewById(R.id.view_bottom_scrim);
        tvCategory = view.findViewById(R.id.tv_category);
        tvAmount = view.findViewById(R.id.tv_amount);
        tvDate = view.findViewById(R.id.tv_date);
        tvNote = view.findViewById(R.id.tv_note);
        btnBackToCamera = view.findViewById(R.id.btn_back_to_camera);
        ImageButton btnReturn = view.findViewById(R.id.btn_return);
        ImageButton btnMore = view.findViewById(R.id.btn_receipt_more);

        View.OnClickListener returnToCameraListener = v -> {
            Fragment parent = getParentFragment();
            if (parent instanceof CameraHostFragment) {
                ((CameraHostFragment) parent).returnToCamera();
            } else {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        };

        btnReturn.setOnClickListener(v -> {
            Fragment parent = getParentFragment();
            if (parent instanceof CameraHostFragment) {
                if (getActivity() instanceof MainActivity
                        && ((MainActivity) getActivity()).handleReceiptTopBack()) {
                    return;
                }
                ((CameraHostFragment) parent).returnToCamera();
            } else {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        if (btnBackToCamera != null) {
            btnBackToCamera.setText(insideCameraHost ? R.string.action_back_to_camera : R.string.action_close_detail);
            btnBackToCamera.setIconResource(insideCameraHost ? R.drawable.ic_photo_camera : R.drawable.ic_close);
            btnBackToCamera.setOnClickListener(returnToCameraListener);
        }

        if (btnMore != null) {
            btnMore.setOnClickListener(this::showMoreOptionsMenu);
        }

        if (getCategoriesUseCase != null) {
            getCategoriesUseCase.execute("ALL").observe(getViewLifecycleOwner(), categories -> {
                latestCategories = categories != null ? categories : new ArrayList<>();
                bindViews();
            });
        }

        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                TransactionFormFragment.RESULT_TRANSACTION_UPDATED,
                getViewLifecycleOwner(),
                (key, bundle) -> {
                    amount = bundle.getDouble(TransactionFormFragment.RESULT_KEY_AMOUNT, amount);
                    category = bundle.getString(TransactionFormFragment.RESULT_KEY_CATEGORY, category);
                    imageUri = bundle.getString(TransactionFormFragment.RESULT_KEY_IMAGE_URI);
                    timestamp = bundle.getLong(TransactionFormFragment.RESULT_KEY_TIMESTAMP, timestamp);
                    note = bundle.getString(TransactionFormFragment.RESULT_KEY_NOTE, note);
                    type = bundle.getString(TransactionFormFragment.RESULT_KEY_TYPE, type);
                    walletId = bundle.getLong(TransactionFormFragment.RESULT_KEY_WALLET_ID, walletId);
                    bindViews();
                }
        );

        bindViews();
    }

    private void showMoreOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        if (transactionId > 0) {
            popup.getMenu().add(0, MENU_ID_EDIT, 0, R.string.action_edit);
        }
        popup.getMenu().add(0, MENU_ID_SAVE_IMAGE, 1, R.string.action_save_image);
        if (transactionId > 0) {
            popup.getMenu().add(0, MENU_ID_DELETE, 2, R.string.action_delete_transaction);
        }

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_ID_EDIT) {
                openEditForm();
                return true;
            } else if (item.getItemId() == MENU_ID_SAVE_IMAGE) {
                saveImageToGallery();
                return true;
            } else if (item.getItemId() == MENU_ID_DELETE) {
                confirmDeleteTransaction();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDeleteTransaction() {
        String amountText = CurrencyFormatter.formatFull(requireContext(), amount);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_transaction_title)
                .setMessage(getString(R.string.dialog_delete_transaction_message, amountText))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> performDeleteTransaction())
                .show();
    }

    private void performDeleteTransaction() {
        if (deleteTransactionUseCase == null || transactionId <= 0) {
            return;
        }
        TransactionModel tx = buildCurrentTransactionModel();
        final DeleteTransactionUseCase useCase = deleteTransactionUseCase;
        View anchorView = requireActivity().findViewById(android.R.id.content);

        useCase.execute(tx);

        if (!(getParentFragment() instanceof CameraHostFragment)) {
            requireActivity().getSupportFragmentManager().popBackStack();
        }

        if (anchorView != null) {
            Snackbar.make(anchorView, R.string.msg_transaction_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo, v -> useCase.undo(tx))
                    .setActionTextColor(0xFFFF5722)
                    .show();
        }
    }

    private void saveImageToGallery() {
        if (imageUri == null || imageUri.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_no_receipt_image_to_save, Toast.LENGTH_SHORT).show();
            return;
        }
        if (saveImageExecutor == null || saveImageExecutor.isShutdown()) {
            saveImageExecutor = Executors.newSingleThreadExecutor();
        }
        final Context appContext = requireContext().getApplicationContext();
        final Uri sourceUri = Uri.parse(imageUri);

        saveImageExecutor.execute(() -> {
            ContentResolver resolver = appContext.getContentResolver();
            String fileName = "receipt_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ExpenseTracker");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri destUri = null;
            try {
                destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (destUri == null) {
                    throw new IOException("Failed to create MediaStore record");
                }
                try (InputStream in = resolver.openInputStream(sourceUri);
                     OutputStream out = resolver.openOutputStream(destUri)) {
                    if (in == null || out == null) {
                        throw new IOException("Unable to open image streams");
                    }
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues completed = new ContentValues();
                    completed.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(destUri, completed, null, null);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.msg_receipt_image_saved, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                if (destUri != null) {
                    try {
                        resolver.delete(destUri, null, null);
                    } catch (Exception ignored) {
                    }
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.msg_save_image_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void bindViews() {
        if (!isAdded()) return;
        boolean isIncome = "INCOME".equalsIgnoreCase(type);
        int semanticColor = Color.parseColor(isIncome ? "#4CAF50" : "#FF5722");

        int defaultRes = isIncome
                ? R.drawable.bg_default_receipt_income
                : R.drawable.bg_default_receipt_expense;
        if (imageUri != null && !imageUri.trim().isEmpty()) {
            Glide.with(this)
                    .load(imageUri)
                    .placeholder(defaultRes)
                    .error(defaultRes)
                    .into(ivReceiptImage);
        } else {
            ivReceiptImage.setImageResource(defaultRes);
        }

        int categoryColor = resolveCategoryColor(category, type);
        int categoryFill = Color.argb(64, Color.red(categoryColor), Color.green(categoryColor), Color.blue(categoryColor));
        int strokePx = Math.max(2, Math.round(1.5f * getResources().getDisplayMetrics().density));

        tvCategory.setText(category != null ? category : "Other");
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(999f);
        badgeBg.setColor(categoryFill);
        badgeBg.setStroke(strokePx, categoryColor);
        tvCategory.setBackground(badgeBg);

        String prefix = isIncome ? "+ " : "- ";
        tvAmount.setText(prefix + CurrencyFormatter.formatFull(requireContext(), amount));
        tvAmount.setTextColor(semanticColor);

        if (btnBackToCamera != null) {
            btnBackToCamera.setBackgroundTintList(ColorStateList.valueOf(semanticColor));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(timestamp)));

        if (note != null && !note.trim().isEmpty()) {
            tvNote.setText(note);
            tvNote.setVisibility(View.VISIBLE);
            if (viewBottomScrim != null) {
                viewBottomScrim.setVisibility(View.VISIBLE);
            }
        } else {
            tvNote.setVisibility(View.GONE);
            if (viewBottomScrim != null) {
                viewBottomScrim.setVisibility(View.GONE);
            }
        }
    }

    private int resolveCategoryColor(String categoryName, String txType) {
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            String targetName = categoryName.trim();
            String fallbackHex = null;
            for (CategoryModel model : latestCategories) {
                if (model == null || model.getName() == null || model.getColorHex() == null) {
                    continue;
                }
                if (model.getName().trim().equalsIgnoreCase(targetName)) {
                    if (txType != null && txType.equalsIgnoreCase(model.getType())) {
                        try {
                            return Color.parseColor(model.getColorHex());
                        } catch (IllegalArgumentException ignored) {
                        }
                    } else if (fallbackHex == null) {
                        fallbackHex = model.getColorHex();
                    }
                }
            }
            if (fallbackHex != null) {
                try {
                    return Color.parseColor(fallbackHex);
                } catch (IllegalArgumentException ignored) {
                }
            }
            switch (targetName.toLowerCase(Locale.ROOT)) {
                case "food":
                case "salary":
                    return Color.parseColor("#4CAF50");
                case "drink":
                    return Color.parseColor("#00BCD4");
                case "transport":
                    return Color.parseColor("#2196F3");
                case "utilities":
                    return Color.parseColor("#FF9800");
                case "shopping":
                    return Color.parseColor("#9C27B0");
                case "entertainment":
                    return Color.parseColor("#E91E63");
                case "bonus":
                    return Color.parseColor("#8BC34A");
                case "investment":
                    return Color.parseColor("#CDDC39");
                default:
                    return Color.parseColor("#607D8B");
            }
        }
        return Color.parseColor("#607D8B");
    }

    private void openEditForm() {
        TransactionModel tx = buildCurrentTransactionModel();

        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .add(android.R.id.content, TransactionFormFragment.newEditInstance(tx))
                .addToBackStack(null)
                .commit();
    }

    private TransactionModel buildCurrentTransactionModel() {
        TransactionModel tx = new TransactionModel.Builder()
                .id(transactionId)
                .amount(amount)
                .title(title)
                .category(category)
                .imageUri(imageUri)
                .timestamp(new Date(timestamp))
                .note(note)
                .walletId(walletId)
                .type(type)
                .build();
        tx.setUuid(transactionUuid);
        return tx;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (saveImageExecutor != null) {
            saveImageExecutor.shutdown();
        }
    }
}
