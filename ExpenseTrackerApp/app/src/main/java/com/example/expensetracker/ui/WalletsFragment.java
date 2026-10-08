package com.example.expensetracker.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.usecase.DeleteWalletUseCase;
import com.example.expensetracker.core.domain.usecase.GetAllWalletsUseCase;
import com.example.expensetracker.core.domain.usecase.InsertWalletUseCase;
import com.example.expensetracker.core.domain.usecase.UpdateWalletUseCase;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class WalletsFragment extends Fragment {

    @Inject
    GetAllWalletsUseCase getAllWalletsUseCase;
    @Inject
    InsertWalletUseCase insertWalletUseCase;
    @Inject
    UpdateWalletUseCase updateWalletUseCase;
    @Inject
    DeleteWalletUseCase deleteWalletUseCase;

    private WalletAdapter adapter;
    private View emptyStateView;
    private RecyclerView rvWallets;

    private final String[] presetColors = {
            "#FF5722", "#4CAF50", "#2196F3", "#9C27B0",
            "#FF9800", "#00BCD4", "#E91E63", "#607D8B"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);

        rvWallets = view.findViewById(R.id.rv_wallets);
        ImageButton btnAddWallet = view.findViewById(R.id.btn_add_wallet);
        emptyStateView = view.findViewById(R.id.empty_state_wallets);

        setupEmptyState();

        rvWallets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvWallets.setItemAnimator(new DefaultItemAnimator());
        adapter = new WalletAdapter(new ArrayList<>(), this::showEditWalletDialog);
        rvWallets.setAdapter(adapter);

        getAllWalletsUseCase.execute().observe(getViewLifecycleOwner(), wallets -> {
            if (wallets == null || wallets.isEmpty()) {
                adapter.setWallets(new ArrayList<>());
                emptyStateView.setVisibility(View.VISIBLE);
                rvWallets.setVisibility(View.GONE);
            } else {
                adapter.setWallets(wallets);
                emptyStateView.setVisibility(View.GONE);
                rvWallets.setVisibility(View.VISIBLE);
            }
        });

        btnAddWallet.setOnClickListener(v -> showAddWalletDialog());
    }

    private void setupEmptyState() {
        if (emptyStateView == null) return;
        ImageView ivIcon = emptyStateView.findViewById(R.id.iv_empty_icon);
        TextView tvTitle = emptyStateView.findViewById(R.id.tv_empty_title);
        TextView tvDesc = emptyStateView.findViewById(R.id.tv_empty_desc);
        MaterialButton btnAction = emptyStateView.findViewById(R.id.btn_empty_action);

        ivIcon.setImageResource(R.drawable.ic_wallet);
        tvTitle.setText(R.string.empty_wallets_title);
        tvDesc.setText(R.string.empty_wallets_desc);
        btnAction.setText(R.string.btn_add_wallet);
        btnAction.setVisibility(View.VISIBLE);
        btnAction.setOnClickListener(v -> showAddWalletDialog());
    }

    private void showAddWalletDialog() {
        showWalletBottomSheet(null);
    }

    private void showEditWalletDialog(WalletModel wallet) {
        showWalletBottomSheet(wallet);
    }

    private void showWalletBottomSheet(@Nullable WalletModel walletToEdit) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_wallet, null);
        dialog.setContentView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_wallet_title);
        TextInputEditText etName = dialogView.findViewById(R.id.et_wallet_name);
        TextInputEditText etBalance = dialogView.findViewById(R.id.et_wallet_balance);
        ChipGroup chipGroupColors = dialogView.findViewById(R.id.chip_group_wallet_colors);
        MaterialButton btnDelete = dialogView.findViewById(R.id.btn_delete_wallet);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_wallet);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_wallet);

        boolean isEditing = walletToEdit != null;
        tvTitle.setText(isEditing ? R.string.dialog_title_edit_wallet : R.string.dialog_title_add_wallet);
        btnSave.setText(isEditing ? R.string.btn_save : R.string.action_create);

        final String[] selectedColor = {isEditing && walletToEdit.getColorHex() != null ? walletToEdit.getColorHex() : presetColors[0]};

        if (isEditing) {
            etName.setText(walletToEdit.getName());
            etBalance.setText(String.valueOf(walletToEdit.getBalance()));
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                deleteWalletUseCase.execute(walletToEdit.getId());
                dialog.dismiss();
                Toast.makeText(requireContext(), "Đã xóa ví: " + walletToEdit.getName(), Toast.LENGTH_SHORT).show();
            });
        }

        // Color chips
        for (String hex : presetColors) {
            Chip colorChip = new Chip(requireContext());
            colorChip.setCheckable(true);
            colorChip.setText("   ");
            int c = Color.parseColor(hex);
            colorChip.setChipBackgroundColor(ColorStateList.valueOf(c));
            colorChip.setChipStrokeColor(ColorStateList.valueOf(Color.WHITE));
            colorChip.setChipStrokeWidth(3f);

            if (hex.equalsIgnoreCase(selectedColor[0])) {
                colorChip.setChecked(true);
            }

            colorChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedColor[0] = hex;
                }
            });
            chipGroupColors.addView(colorChip);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String balanceStr = etBalance.getText() != null ? etBalance.getText().toString().trim() : "";

            if (name.isEmpty()) {
                etName.setError(getString(R.string.error_wallet_name_required));
                return;
            }

            double balance = 0.0;
            if (!balanceStr.isEmpty()) {
                try {
                    balance = Double.parseDouble(balanceStr);
                } catch (NumberFormatException e) {
                    etBalance.setError("Số dư không hợp lệ");
                    return;
                }
            }

            if (isEditing) {
                walletToEdit.setName(name);
                walletToEdit.setBalance(balance);
                walletToEdit.setColorHex(selectedColor[0]);
                updateWalletUseCase.execute(walletToEdit);
                Toast.makeText(requireContext(), "Đã cập nhật: " + name, Toast.LENGTH_SHORT).show();
            } else {
                WalletModel newWallet = new WalletModel(0, name, balance, selectedColor[0], "account_balance_wallet");
                insertWalletUseCase.execute(newWallet);
                Toast.makeText(requireContext(), "Đã tạo ví: " + name, Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private interface OnWalletClickListener {
        void onWalletClick(WalletModel wallet);
    }

    private static class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {

        private List<WalletModel> wallets;
        private final OnWalletClickListener clickListener;

        public WalletAdapter(List<WalletModel> wallets, OnWalletClickListener clickListener) {
            this.wallets = wallets;
            this.clickListener = clickListener;
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

            NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
            format.setMinimumFractionDigits(0);
            format.setMaximumFractionDigits(2);
            String formattedBalance = format.format(wallet.getBalance()) + " ₫";
            holder.tvBalance.setText(formattedBalance);

            // Apply color bar
            int colorInt = Color.parseColor(wallet.getColorHex() != null ? wallet.getColorHex() : "#FF5722");
            holder.viewColorBar.setBackgroundColor(colorInt);
            holder.ivWalletIcon.setImageTintList(ColorStateList.valueOf(colorInt));

            holder.btnOptions.setOnClickListener(v -> clickListener.onWalletClick(wallet));
            holder.itemView.setOnClickListener(v -> clickListener.onWalletClick(wallet));
        }

        @Override
        public int getItemCount() {
            return wallets == null ? 0 : wallets.size();
        }

        static class WalletViewHolder extends RecyclerView.ViewHolder {
            View viewColorBar;
            ImageView ivWalletIcon;
            TextView tvName;
            TextView tvBalance;
            ImageButton btnOptions;

            public WalletViewHolder(@NonNull View itemView) {
                super(itemView);
                viewColorBar = itemView.findViewById(R.id.view_wallet_color_bar);
                ivWalletIcon = itemView.findViewById(R.id.iv_wallet_icon);
                tvName = itemView.findViewById(R.id.tv_wallet_name);
                tvBalance = itemView.findViewById(R.id.tv_wallet_balance);
                btnOptions = itemView.findViewById(R.id.btn_wallet_options);
            }
        }
    }
}
