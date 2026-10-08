package com.example.expensetracker.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.expensetracker.core.domain.model.CategoryTotalModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.viewmodel.HomeViewModel;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class DashboardOverviewFragment extends Fragment {

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private HomeViewModel viewModel;
    private TransactionAdapter adapter;
    private TextView tvTotalExpense;
    private TextView tvTotalIncome;
    private TextView tvTotalBalance;
    private MaterialButtonToggleGroup toggleTimeFilter;
    private PieChart pieChart;
    private RecyclerView rvRecentExpenses;
    private boolean isChartInitialized = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(HomeViewModel.class);

        tvTotalExpense = view.findViewById(R.id.tv_total_expense);
        tvTotalIncome = view.findViewById(R.id.tv_total_income);
        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        toggleTimeFilter = view.findViewById(R.id.toggle_time_filter);
        pieChart = view.findViewById(R.id.pie_chart);
        rvRecentExpenses = view.findViewById(R.id.rv_recent_expenses);

        adapter = new TransactionAdapter();
        rvRecentExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentExpenses.setItemAnimator(new DefaultItemAnimator());
        rvRecentExpenses.setAdapter(adapter);

        // Swipe-to-delete with Undo
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
        itemTouchHelper.attachToRecyclerView(rvRecentExpenses);

        toggleTimeFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_filter_day) {
                    viewModel.setTimeFilter(HomeViewModel.TimeFilter.DAY);
                } else if (checkedId == R.id.btn_filter_month) {
                    viewModel.setTimeFilter(HomeViewModel.TimeFilter.MONTH);
                } else if (checkedId == R.id.btn_filter_year) {
                    viewModel.setTimeFilter(HomeViewModel.TimeFilter.YEAR);
                }
            }
        });

        viewModel.getRecentExpenses().observe(getViewLifecycleOwner(), expenses -> {
            adapter.setExpenses(expenses);
        });

        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), total -> {
            if (total == null) total = 0.0;
            tvTotalExpense.setText(formatFullCurrency(total));
        });

        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), total -> {
            if (total == null) total = 0.0;
            tvTotalIncome.setText(formatFullCurrency(total));
        });

        viewModel.getWallets().observe(getViewLifecycleOwner(), wallets -> {
            double totalBalance = 0;
            if (wallets != null) {
                for (WalletModel w : wallets) {
                    totalBalance += w.getBalance();
                }
            }
            tvTotalBalance.setText(getString(R.string.label_total_balance) + ": " + formatCompactCurrency(totalBalance));
        });

        viewModel.getCategoryTotals().observe(getViewLifecycleOwner(), categoryTotals -> {
            if (categoryTotals != null) {
                setupPieChart(categoryTotals);
            }
        });
    }

    private void setupPieChart(List<CategoryTotalModel> categoryTotals) {
        List<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        double totalSum = 0;
        for (CategoryTotalModel ct : categoryTotals) {
            totalSum += ct.getTotal();
        }

        for (CategoryTotalModel ct : categoryTotals) {
            double percentage = totalSum == 0 ? 0 : (ct.getTotal() / totalSum) * 100;
            String label = String.format(Locale.getDefault(), "%s %.0f%%", ct.getCategory(), percentage);
            entries.add(new PieEntry((float) ct.getTotal(), label));
            switch (ct.getCategory()) {
                case "Food":
                    colors.add(Color.parseColor("#4CAF50"));
                    break;
                case "Transport":
                    colors.add(Color.parseColor("#2196F3"));
                    break;
                case "Utilities":
                    colors.add(Color.parseColor("#FF9800"));
                    break;
                case "Shopping":
                    colors.add(Color.parseColor("#9C27B0"));
                    break;
                case "Entertainment":
                    colors.add(Color.parseColor("#E91E63"));
                    break;
                default:
                    colors.add(Color.parseColor("#607D8B"));
                    break;
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.getLegend().setWordWrapEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleColor(Color.TRANSPARENT);
        if (!isChartInitialized) {
            pieChart.animateY(600);
            isChartInitialized = true;
        } else {
            pieChart.notifyDataSetChanged();
            pieChart.invalidate();
        }
    }

    private String formatCompactCurrency(double amount) {
        Locale locale = Locale.US;
        if (amount >= 1_000_000_000) {
            return String.format(locale, "%.1fB ₫", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000) {
            return String.format(locale, "%.1fM ₫", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            return String.format(locale, "%.1fK ₫", amount / 1_000.0);
        } else {
            return formatFullCurrency(amount);
        }
    }

    private String formatFullCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(amount) + " ₫";
    }
}
