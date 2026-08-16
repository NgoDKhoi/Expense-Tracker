package com.example.expensetracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.expensetracker.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.text.InputType;
import androidx.lifecycle.ViewModelProvider;
import com.example.expensetracker.ExpenseTrackerApplication;
// imports removed
import com.example.expensetracker.viewmodel.HomeViewModel;

public class DashboardFragment extends Fragment {

    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAddWallet;
    @javax.inject.Inject
    ViewModelProvider.Factory viewModelFactory;
    private HomeViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(HomeViewModel.class);

        bottomNav = view.findViewById(R.id.dashboard_bottom_nav);
        fabAddWallet = view.findViewById(R.id.fab_add_wallet);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_overview) {
                selectedFragment = new DashboardOverviewFragment();
            } else if (itemId == R.id.nav_wallets) {
                selectedFragment = new BudgetFragment();
            } else if (itemId == R.id.nav_history) {
                // TODO: Create Bank-style History Fragment later, using PhotosFragment temporarily or a placeholder
                selectedFragment = new Fragment(); // Placeholder
            } else if (itemId == R.id.nav_ai) {
                selectedFragment = new AiAssistantFragment();
            }

            if (selectedFragment != null) {
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.dashboard_fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_overview);
        }

        fabAddWallet.setOnClickListener(v -> showAddWalletDialog());
    }

    private void showAddWalletDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Wallet");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Wallet Name");
        layout.addView(nameInput);

        final EditText balanceInput = new EditText(requireContext());
        balanceInput.setHint("Initial Balance");
        balanceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(balanceInput);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String balanceStr = balanceInput.getText().toString();
            if (!name.isEmpty() && !balanceStr.isEmpty()) {
                double balance = Double.parseDouble(balanceStr);
                viewModel.insertWallet(name, balance);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}

