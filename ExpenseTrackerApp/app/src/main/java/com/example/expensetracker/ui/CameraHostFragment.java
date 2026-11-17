package com.example.expensetracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CameraHostFragment extends Fragment {

    private ViewPager2 verticalViewPager;
    @javax.inject.Inject
    ViewModelProvider.Factory viewModelFactory;
    private HomeViewModel viewModel;
    private CameraPagerAdapter adapter;
    private List<TransactionModel> latestExpenses = new ArrayList<>();
    private long pendingTransactionId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera_host, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        verticalViewPager = view.findViewById(R.id.vertical_view_pager);
        verticalViewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        adapter = new CameraPagerAdapter(this);
        verticalViewPager.setAdapter(adapter);
        verticalViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0 && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).clearOpenedReceiptFromPhotos();
                }
            }
        });

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(HomeViewModel.class);

        // Fetch all expenses to allow vertical scrolling of history
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                latestExpenses = expenses;
                adapter.setExpenses(expenses);
                if (pendingTransactionId != -1) {
                    scrollToTransactionInternal(pendingTransactionId);
                }
            }
        });
    }

    public boolean isOnCameraPage() {
        return verticalViewPager == null || verticalViewPager.getCurrentItem() == 0;
    }

    public void scrollToTransaction(long transactionId) {
        if (!scrollToTransactionInternal(transactionId)) {
            pendingTransactionId = transactionId;
        }
    }

    private boolean scrollToTransactionInternal(long transactionId) {
        if (verticalViewPager == null || latestExpenses == null || latestExpenses.isEmpty()) {
            return false;
        }
        for (int i = 0; i < latestExpenses.size(); i++) {
            if (latestExpenses.get(i).getId() == transactionId) {
                pendingTransactionId = -1;
                final int targetPage = 1 + i;
                verticalViewPager.post(() -> {
                    if (verticalViewPager != null) {
                        verticalViewPager.setCurrentItem(targetPage, false);
                    }
                });
                return true;
            }
        }
        return false;
    }

    public void returnToCamera() {
        pendingTransactionId = -1;
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).clearOpenedReceiptFromPhotos();
        }
        if (verticalViewPager != null) {
            verticalViewPager.setCurrentItem(0, true);
        }
    }

    public void resetToCameraImmediate() {
        pendingTransactionId = -1;
        if (verticalViewPager != null && verticalViewPager.getCurrentItem() != 0) {
            verticalViewPager.setCurrentItem(0, false);
        }
    }

    private static class CameraPagerAdapter extends FragmentStateAdapter {

        private List<TransactionModel> expenses = new ArrayList<>();

        public CameraPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        public void setExpenses(List<TransactionModel> expenses) {
            this.expenses = expenses != null ? expenses : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new AddTransactionFragment();
            } else {
                TransactionModel record = expenses.get(position - 1);
                return ReceiptDetailFragment.newInstance(record);
            }
        }

        @Override
        public int getItemCount() {
            return 1 + expenses.size();
        }

        @Override
        public long getItemId(int position) {
            if (position == 0) {
                return 0L;
            }
            return stableContentId(expenses.get(position - 1));
        }

        @Override
        public boolean containsItem(long itemId) {
            if (itemId == 0L) {
                return true;
            }
            for (TransactionModel tx : expenses) {
                if (stableContentId(tx) == itemId) {
                    return true;
                }
            }
            return false;
        }

        private static long stableContentId(TransactionModel tx) {
            if (tx == null) {
                return 1L;
            }
            int hash = Objects.hash(
                    tx.getId(),
                    tx.getUpdatedAt(),
                    tx.getAmount(),
                    tx.getCategory(),
                    tx.getNote(),
                    tx.getImageUri(),
                    tx.getType(),
                    tx.getWalletId(),
                    tx.getTimestamp() != null ? tx.getTimestamp().getTime() : 0L
            );
            return (hash & 0x7FFFFFFFL) + 1L;
        }
    }
}
