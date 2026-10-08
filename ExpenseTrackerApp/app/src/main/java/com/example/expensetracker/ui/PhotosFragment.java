package com.example.expensetracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.usecase.GetAllTransactionsUseCase;

import java.util.ArrayList;

import javax.inject.Inject;

public class PhotosFragment extends Fragment {

    @Inject
    GetAllTransactionsUseCase getAllTransactionsUseCase;
    private PhotoAdapter adapter;
    private View emptyStateView;
    private RecyclerView rvPhotos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);

        rvPhotos = view.findViewById(R.id.rv_photos);
        emptyStateView = view.findViewById(R.id.empty_state_photos);

        setupEmptyState();

        rvPhotos.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rvPhotos.setItemAnimator(new DefaultItemAnimator());

        adapter = new PhotoAdapter(requireContext(), new ArrayList<>(), expense -> {
            long time = expense.getTimestamp() != null ? expense.getTimestamp().getTime() : System.currentTimeMillis();
            ReceiptDetailFragment detailFragment = ReceiptDetailFragment.newInstance(
                    expense.getAmount(),
                    expense.getCategory(),
                    expense.getImageUri(),
                    time,
                    expense.getNote()
            );

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvPhotos.setAdapter(adapter);

        getAllTransactionsUseCase.execute().observe(getViewLifecycleOwner(), expenses -> {
            adapter.setExpenses(expenses);
            if (adapter.getItemCount() == 0) {
                emptyStateView.setVisibility(View.VISIBLE);
                rvPhotos.setVisibility(View.GONE);
            } else {
                emptyStateView.setVisibility(View.GONE);
                rvPhotos.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupEmptyState() {
        if (emptyStateView == null) return;
        ImageView ivIcon = emptyStateView.findViewById(R.id.iv_empty_icon);
        TextView tvTitle = emptyStateView.findViewById(R.id.tv_empty_title);
        TextView tvDesc = emptyStateView.findViewById(R.id.tv_empty_desc);

        ivIcon.setImageResource(R.drawable.ic_photo_camera);
        tvTitle.setText(R.string.empty_photos_title);
        tvDesc.setText(R.string.empty_photos_desc);
    }
}
