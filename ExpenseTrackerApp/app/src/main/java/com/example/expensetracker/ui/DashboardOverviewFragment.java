package com.example.expensetracker.ui;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.CategoryTotalModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.util.CurrencyFormatter;
import com.example.expensetracker.viewmodel.HomeViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

public class DashboardOverviewFragment extends Fragment {

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private HomeViewModel viewModel;
    private TransactionAdapter adapter;
    private TextView tvTotalExpense;
    private TextView tvTotalIncome;
    private TextView tvTotalBalance;
    private TextView tvNetSavings;
    private MaterialButtonToggleGroup toggleTimeFilter;
    private MaterialButtonToggleGroup toggleChartType;
    private PieChart pieChart;
    private BarChart barChart;
    private RecyclerView rvRecentExpenses;

    private boolean isChartInitialized = false;
    private boolean showBarChart = false;
    private double latestTotalExpense = 0.0;
    private double latestTotalIncome = 0.0;
    private double latestTotalBalance = 0.0;
    private List<CategoryTotalModel> latestCategoryTotals = new ArrayList<>();
    private List<CategoryModel> latestCategories = new ArrayList<>();
    private final Map<String, String> categoryColorMap = new HashMap<>();
    private List<TransactionModel> latestAllTransactions = new ArrayList<>();
    private SharedPreferences currencyPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener currencyPrefsListener;

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
        tvNetSavings = view.findViewById(R.id.tv_net_savings);
        toggleTimeFilter = view.findViewById(R.id.toggle_time_filter);
        toggleChartType = view.findViewById(R.id.toggle_chart_type);
        pieChart = view.findViewById(R.id.pie_chart);
        barChart = view.findViewById(R.id.bar_chart);
        rvRecentExpenses = view.findViewById(R.id.rv_recent_expenses);

        adapter = new TransactionAdapter();
        adapter.setOnTransactionClickListener(this::openEditTransaction);
        rvRecentExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentExpenses.setItemAnimator(new DefaultItemAnimator());
        rvRecentExpenses.setAdapter(adapter);

        // Swipe left to delete (with Undo), swipe right to edit
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                TransactionModel item = adapter.getItemAt(position);
                if (item == null) return;

                if (direction == ItemTouchHelper.RIGHT) {
                    adapter.notifyItemChanged(position);
                    openEditTransaction(item);
                    return;
                }

                adapter.removeItem(position);
                viewModel.deleteTransaction(item);

                Snackbar.make(requireView(), R.string.msg_transaction_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_undo, v -> {
                            adapter.restoreItem(item, position);
                            viewModel.undoDeleteTransaction(item);
                        })
                        .setActionTextColor(0xFFFF5722)
                        .show();
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
                if (showBarChart) {
                    setupBarChart();
                }
            }
        });

        toggleChartType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                showBarChart = (checkedId == R.id.btn_chart_bar);
                pieChart.setVisibility(showBarChart ? View.GONE : View.VISIBLE);
                barChart.setVisibility(showBarChart ? View.VISIBLE : View.GONE);
                if (showBarChart) {
                    setupBarChart();
                } else {
                    setupPieChart(latestCategoryTotals);
                }
            }
        });

        viewModel.getRecentExpenses().observe(getViewLifecycleOwner(), expenses -> adapter.setExpenses(expenses));

        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), total -> {
            latestTotalExpense = total != null ? total : 0.0;
            renderTotalsAndSavings();
        });

        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), total -> {
            latestTotalIncome = total != null ? total : 0.0;
            renderTotalsAndSavings();
        });

        viewModel.getWallets().observe(getViewLifecycleOwner(), wallets -> {
            double totalBalance = 0;
            if (wallets != null) {
                for (WalletModel w : wallets) {
                    totalBalance += w.getBalance();
                }
            }
            latestTotalBalance = totalBalance;
            tvTotalBalance.setText(getString(R.string.label_total_balance) + ": "
                    + CurrencyFormatter.formatCompact(requireContext(), latestTotalBalance));
        });

        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            latestCategories = categories != null ? categories : new ArrayList<>();
            categoryColorMap.clear();
            for (CategoryModel cat : latestCategories) {
                if (cat == null || cat.getName() == null || cat.getColorHex() == null) continue;
                String key = cat.getName().trim().toLowerCase(Locale.ROOT);
                if ("EXPENSE".equalsIgnoreCase(cat.getType()) || !categoryColorMap.containsKey(key)) {
                    categoryColorMap.put(key, cat.getColorHex());
                }
            }
            if (!showBarChart) {
                setupPieChart(latestCategoryTotals);
            }
        });

        viewModel.getCategoryTotals().observe(getViewLifecycleOwner(), categoryTotals -> {
            latestCategoryTotals = categoryTotals != null ? categoryTotals : new ArrayList<>();
            if (!showBarChart) {
                setupPieChart(latestCategoryTotals);
            }
        });

        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), transactions -> {
            latestAllTransactions = transactions != null ? transactions : new ArrayList<>();
            if (showBarChart) {
                setupBarChart();
            }
        });

        currencyPrefs = requireContext().getSharedPreferences(CurrencyFormatter.PREFS_NAME, Context.MODE_PRIVATE);
        currencyPrefsListener = (prefs, key) -> {
            if (CurrencyFormatter.KEY_CURRENCY_SYMBOL.equals(key) && isAdded()) {
                refreshCurrencyUi();
            }
        };
        currencyPrefs.registerOnSharedPreferenceChangeListener(currencyPrefsListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currencyPrefs != null && currencyPrefsListener != null) {
            currencyPrefs.unregisterOnSharedPreferenceChangeListener(currencyPrefsListener);
        }
    }

    private void renderTotalsAndSavings() {
        if (!isAdded()) return;
        tvTotalExpense.setText(CurrencyFormatter.formatFull(requireContext(), latestTotalExpense));
        tvTotalIncome.setText(CurrencyFormatter.formatFull(requireContext(), latestTotalIncome));

        double net = latestTotalIncome - latestTotalExpense;
        String formattedNet = CurrencyFormatter.formatFull(requireContext(), Math.abs(net));
        if (net >= 0) {
            tvNetSavings.setText(getString(R.string.label_net_savings_positive, formattedNet));
            tvNetSavings.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvNetSavings.setText(getString(R.string.label_net_savings_negative, formattedNet));
            tvNetSavings.setTextColor(Color.parseColor("#FF5722"));
        }
    }

    private void refreshCurrencyUi() {
        tvTotalBalance.setText(getString(R.string.label_total_balance) + ": "
                + CurrencyFormatter.formatCompact(requireContext(), latestTotalBalance));
        renderTotalsAndSavings();
        adapter.notifyDataSetChanged();
    }

    private void setupPieChart(List<CategoryTotalModel> categoryTotals) {
        if (categoryTotals == null || categoryTotals.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataTextColor(Color.LTGRAY);
            pieChart.invalidate();
            return;
        }

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
            colors.add(resolveCategoryColor(ct.getCategory()));
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

    private int resolveCategoryColor(String categoryName) {
        if (categoryName != null) {
            String hex = categoryColorMap.get(categoryName.trim().toLowerCase(Locale.ROOT));
            if (hex != null) {
                try {
                    return Color.parseColor(hex);
                } catch (IllegalArgumentException ignored) {
                    // Fallback below
                }
            }
            switch (categoryName) {
                case "Food":
                    return Color.parseColor("#4CAF50");
                case "Drink":
                    return Color.parseColor("#00BCD4");
                case "Transport":
                    return Color.parseColor("#2196F3");
                case "Utilities":
                    return Color.parseColor("#FF9800");
                case "Shopping":
                    return Color.parseColor("#9C27B0");
                case "Entertainment":
                    return Color.parseColor("#E91E63");
                default:
                    return Color.parseColor("#607D8B");
            }
        }
        return Color.parseColor("#607D8B");
    }

    private void setupBarChart() {
        List<HomeViewModel.TrendBucket> buckets = HomeViewModel.buildTrendBuckets(
                latestAllTransactions,
                viewModel.getCurrentTimeFilter(),
                System.currentTimeMillis()
        );
        if (buckets.isEmpty()) {
            barChart.clear();
            barChart.invalidate();
            return;
        }

        List<BarEntry> incomeEntries = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < buckets.size(); i++) {
            HomeViewModel.TrendBucket bucket = buckets.get(i);
            labels.add(bucket.label);
            incomeEntries.add(new BarEntry(i, (float) bucket.income));
            expenseEntries.add(new BarEntry(i, (float) bucket.expense));
        }

        BarDataSet incomeSet = new BarDataSet(incomeEntries, getString(R.string.chart_legend_income));
        incomeSet.setColor(Color.parseColor("#4CAF50"));
        incomeSet.setDrawValues(false);

        BarDataSet expenseSet = new BarDataSet(expenseEntries, getString(R.string.chart_legend_expense));
        expenseSet.setColor(Color.parseColor("#FF5722"));
        expenseSet.setDrawValues(false);

        float barWidth = 0.34f;
        float barSpace = 0.04f;
        float groupSpace = 0.24f;

        BarData barData = new BarData(incomeSet, expenseSet);
        barData.setBarWidth(barWidth);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setCenterAxisLabels(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(barData.getGroupWidth(groupSpace, barSpace) * buckets.size());
        xAxis.setLabelCount(buckets.size());
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);

        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisLeft().setGridColor(0x22FFFFFF);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setTextColor(Color.WHITE);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(false);

        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.animateY(450);
        barChart.invalidate();
    }

    private void openEditTransaction(TransactionModel tx) {
        if (tx == null || !isAdded()) return;
        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .add(android.R.id.content, TransactionFormFragment.newEditInstance(tx))
                .addToBackStack(null)
                .commit();
    }
}
