package com.example.expensetracker.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.expensetracker.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptDetailFragment extends Fragment {

    private static final String ARG_AMOUNT = "arg_amount";
    private static final String ARG_CATEGORY = "arg_category";
    private static final String ARG_IMAGE_URI = "arg_image_uri";
    private static final String ARG_TIMESTAMP = "arg_timestamp";
    private static final String ARG_NOTE = "arg_note";

    private double amount;
    private String category;
    private String imageUri;
    private long timestamp;
    private String note;

    public static ReceiptDetailFragment newInstance(double amount, String category, String imageUri, long timestamp, String note) {
        ReceiptDetailFragment fragment = new ReceiptDetailFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_AMOUNT, amount);
        args.putString(ARG_CATEGORY, category);
        args.putString(ARG_IMAGE_URI, imageUri);
        args.putLong(ARG_TIMESTAMP, timestamp);
        args.putString(ARG_NOTE, note);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            amount = getArguments().getDouble(ARG_AMOUNT);
            category = getArguments().getString(ARG_CATEGORY);
            imageUri = getArguments().getString(ARG_IMAGE_URI);
            timestamp = getArguments().getLong(ARG_TIMESTAMP);
            note = getArguments().getString(ARG_NOTE);
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

        ImageView ivReceiptImage = view.findViewById(R.id.iv_receipt_image);
        TextView tvCategory = view.findViewById(R.id.tv_category);
        TextView tvAmount = view.findViewById(R.id.tv_amount);
        TextView tvDate = view.findViewById(R.id.tv_date);
        TextView tvNote = view.findViewById(R.id.tv_note);
        android.widget.ImageButton btnReturn = view.findViewById(R.id.btn_return);

        btnReturn.setOnClickListener(v -> {
            androidx.fragment.app.Fragment parent = getParentFragment();
            if (parent instanceof CameraHostFragment) {
                ((CameraHostFragment) parent).returnToCamera();
            } else {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        if (imageUri != null && !imageUri.isEmpty()) {
            ivReceiptImage.setImageURI(Uri.parse(imageUri));
        } else {
            // Fallback color if no image
            ivReceiptImage.setBackgroundColor(0xFF333333);
        }

        tvCategory.setText(category != null ? category : "Other");
        
        tvAmount.setText(String.format(Locale.getDefault(), "$%.2f", amount));

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(timestamp)));

        if (note != null && !note.trim().isEmpty()) {
            tvNote.setText(note);
            tvNote.setVisibility(View.VISIBLE);
        } else {
            tvNote.setVisibility(View.GONE);
        }
    }
}
