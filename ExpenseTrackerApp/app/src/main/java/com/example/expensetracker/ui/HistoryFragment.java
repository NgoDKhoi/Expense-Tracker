package com.example.expensetracker.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.viewmodel.HomeViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class HistoryFragment extends Fragment {

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private HomeViewModel viewModel;

    private TransactionAdapter adapter;
    private RecyclerView rvHistory;
    private View emptyStateView;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private ChipGroup chipGroupFilters;

    private List<TransactionModel> allTransactions = new ArrayList<>();
    private String currentSearchQuery = "";
    private String currentTypeFilter = "ALL"; // ALL, EXPENSE, INCOME

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(HomeViewModel.class);

        rvHistory = view.findViewById(R.id.rv_history);
        emptyStateView = view.findViewById(R.id.empty_state_history);
        etSearch = view.findViewById(R.id.et_search_history);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        chipGroupFilters = view.findViewById(R.id.chip_group_history_filters);

        setupEmptyState();
        setupRecyclerView();
        setupSearchAndFilters();

        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            allTransactions = expenses != null ? expenses : new ArrayList<>();
            applyFilters();
        });
    }

    private void setupEmptyState() {
        if (emptyStateView == null) return;
        ImageView ivIcon = emptyStateView.findViewById(R.id.iv_empty_icon);
        TextView tvTitle = emptyStateView.findViewById(R.id.tv_empty_title);
        TextView tvDesc = emptyStateView.findViewById(R.id.tv_empty_desc);

        ivIcon.setImageResource(R.drawable.ic_note);
        tvTitle.setText(R.string.empty_history_title);
        tvDesc.setText(R.string.empty_history_desc);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setItemAnimator(new DefaultItemAnimator());
        rvHistory.setAdapter(adapter);

        // Swipe-to-delete with Undo Snackbar
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                TransactionModel itemToDelete = adapter.getItemAt(position);

                if (itemToDelete != null) {
                    adapter.removeItem(position);
                    viewModel.deleteTransaction(itemToDelete);

                    Snackbar.make(requireView(), R.string.msg_transaction_deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.action_undo, v -> {
                                adapter.restoreItem(itemToDelete, position);
                                viewModel.undoDeleteTransaction(itemToDelete);
                            })
                            .setActionTextColor(0xFFFF5722)
                            .show();
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(rvHistory);
    }

    private void setupSearchAndFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));

        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_filter_expense)) {
                currentTypeFilter = "EXPENSE";
            } else if (checkedIds.contains(R.id.chip_filter_income)) {
                currentTypeFilter = "INCOME";
            } else {
                currentTypeFilter = "ALL";
            }
            applyFilters();
        });
    }

    private void applyFilters() {
        List<TransactionModel> filtered = new ArrayList<>();

        for (TransactionModel tx : allTransactions) {
            // Type filter
            boolean matchesType = "ALL".equalsIgnoreCase(currentTypeFilter) ||
                    (tx.getType() != null && tx.getType().equalsIgnoreCase(currentTypeFilter));
            if (!matchesType) continue;

            // Search query filter (matches title, category, or note)
            if (!currentSearchQuery.isEmpty()) {
                String title = tx.getTitle() != null ? tx.getTitle().toLowerCase() : "";
                String category = tx.getCategory() != null ? tx.getCategory().toLowerCase() : "";
                String note = tx.getNote() != null ? tx.getNote().toLowerCase() : "";

                boolean matchesQuery = title.contains(currentSearchQuery) ||
                        category.contains(currentSearchQuery) ||
                        note.contains(currentSearchQuery);

                if (!matchesQuery) continue;
            }

            filtered.add(tx);
        }

        adapter.setExpenses(filtered);

        if (filtered.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
        }
    }
}
