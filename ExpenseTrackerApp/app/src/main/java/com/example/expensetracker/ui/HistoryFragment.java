package com.example.expensetracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.viewmodel.HomeViewModel;

public class HistoryFragment extends Fragment {

    @javax.inject.Inject
    androidx.lifecycle.ViewModelProvider.Factory viewModelFactory;
    private HomeViewModel viewModel;
    private TransactionAdapter adapter;
    private RecyclerView rvHistory;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        rvHistory = view.findViewById(R.id.rv_history);
        tvEmpty = view.findViewById(R.id.tv_empty);
        
        adapter = new TransactionAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);
        
        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(HomeViewModel.class);
        
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            adapter.setExpenses(expenses);
            if (expenses == null || expenses.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvHistory.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvHistory.setVisibility(View.VISIBLE);
            }
        });
    }
}

