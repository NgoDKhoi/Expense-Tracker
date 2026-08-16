package com.example.expensetracker.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.usecase.GetAllWalletsUseCase;
import com.example.expensetracker.core.domain.usecase.InsertWalletUseCase;
// import removed
import android.widget.ImageButton;
import java.text.NumberFormat;
import java.util.Locale;

import java.util.ArrayList;
import java.util.List;

public class BudgetFragment extends Fragment {

    private WalletAdapter adapter;
    @javax.inject.Inject
    GetAllWalletsUseCase getAllWalletsUseCase;
    @javax.inject.Inject
    InsertWalletUseCase insertWalletUseCase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);

        RecyclerView rvWallets = view.findViewById(R.id.rv_wallets);
        ImageButton btnAddWallet = view.findViewById(R.id.btn_add_wallet);

        rvWallets.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WalletAdapter(new ArrayList<>());
        rvWallets.setAdapter(adapter);

        getAllWalletsUseCase.execute().observe(getViewLifecycleOwner(), wallets -> {
            if (wallets != null) {
                adapter.setWallets(wallets);
            }
        });

        btnAddWallet.setOnClickListener(v -> showAddWalletDialog());
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
            String name = nameInput.getText().toString().trim();
            String balanceStr = balanceInput.getText().toString().trim();

            if (name.isEmpty() || balanceStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double balance = Double.parseDouble(balanceStr);
                WalletModel wallet = new WalletModel();
                wallet.setName(name);
                wallet.setBalance(balance);
                insertWalletUseCase.execute(wallet);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid balance", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private static class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {

        private List<WalletModel> wallets;

        public WalletAdapter(List<WalletModel> wallets) {
            this.wallets = wallets;
        }

        public void setWallets(List<WalletModel> wallets) {
            this.wallets = wallets;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public WalletViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet, parent, false);
            return new WalletViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WalletViewHolder holder, int position) {
            WalletModel wallet = wallets.get(position);
            holder.tvName.setText(wallet.getName());
            
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
            String formattedBalance = format.format(wallet.getBalance());
            holder.tvBalance.setText(formattedBalance);
        }

        @Override
        public int getItemCount() {
            return wallets.size();
        }

        static class WalletViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvBalance;

            public WalletViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_wallet_name);
                tvBalance = itemView.findViewById(R.id.tv_wallet_balance);
            }
        }
    }
}

