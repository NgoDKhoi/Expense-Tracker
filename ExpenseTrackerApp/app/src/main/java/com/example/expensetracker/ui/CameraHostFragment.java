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
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.viewmodel.HomeViewModel;
import java.util.ArrayList;
import java.util.List;

public class CameraHostFragment extends Fragment {

    private ViewPager2 verticalViewPager;
    @javax.inject.Inject
    androidx.lifecycle.ViewModelProvider.Factory viewModelFactory;
    private HomeViewModel viewModel;
    private CameraPagerAdapter adapter;

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

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(HomeViewModel.class);

        // Fetch all expenses to allow vertical scrolling of history
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                adapter.setExpenses(expenses);
            }
        });
    }

    public void returnToCamera() {
        if (verticalViewPager != null) {
            verticalViewPager.setCurrentItem(0, true);
        }
    }

    private static class CameraPagerAdapter extends FragmentStateAdapter {

        private List<TransactionModel> expenses = new ArrayList<>();

        public CameraPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        public void setExpenses(List<TransactionModel> expenses) {
            this.expenses = expenses;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new AddTransactionFragment();
            } else {
                TransactionModel record = expenses.get(position - 1);
                long time = record.getTimestamp() != null ? record.getTimestamp().getTime() : System.currentTimeMillis();
                return ReceiptDetailFragment.newInstance(
                        record.getAmount(),
                        record.getCategory(),
                        record.getImageUri(),
                        time,
                        record.getNote()
                );
            }
        }

        @Override
        public int getItemCount() {
            return 1 + expenses.size();
        }
    }
}

