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
import androidx.lifecycle.ViewModelProvider;
import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.viewmodel.HomeViewModel;

public class DashboardFragment extends Fragment {

    private BottomNavigationView bottomNav;
    private FloatingActionButton fabPetAi;
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
        fabPetAi = view.findViewById(R.id.fab_pet_ai);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_overview) {
                selectedFragment = new DashboardOverviewFragment();
                if (fabPetAi != null) fabPetAi.show();
            } else if (itemId == R.id.nav_wallets) {
                selectedFragment = new WalletsFragment();
                if (fabPetAi != null) fabPetAi.show();
            } else if (itemId == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
                if (fabPetAi != null) fabPetAi.show();
            } else if (itemId == R.id.nav_ai) {
                selectedFragment = new AiAssistantFragment();
                if (fabPetAi != null) fabPetAi.hide();
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

        if (fabPetAi != null) {
            fabPetAi.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_ai));
        }
    }
}
