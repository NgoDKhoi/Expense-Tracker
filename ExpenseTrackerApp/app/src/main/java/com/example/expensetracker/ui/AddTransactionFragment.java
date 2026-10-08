package com.example.expensetracker.ui;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.viewmodel.AddTransactionViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class AddTransactionFragment extends Fragment {

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    
    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private AddTransactionViewModel viewModel;
    
    @Inject
    com.example.expensetracker.core.domain.usecase.InsertWalletUseCase insertWalletUseCase;
    
    private PreviewView cameraPreview;

    private View llTopHeader, llCaptureActions;
    private TextView tvWalletBalanceHeader;
    
    private long selectedWalletId = -1;
    private List<WalletModel> cachedWallets;
    private View btnCapture;
    
    private ImageButton btnFlash, btnFlipCamera;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private int flashMode = ImageCapture.FLASH_MODE_OFF;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(AddTransactionViewModel.class);
        
        cameraPreview = view.findViewById(R.id.camera_preview);
        btnCapture = view.findViewById(R.id.btn_capture);
        
        llTopHeader = view.findViewById(R.id.ll_top_header);
        llCaptureActions = view.findViewById(R.id.ll_capture_actions);
        ImageButton btnSettings = view.findViewById(R.id.btn_settings);
        btnFlash = view.findViewById(R.id.btn_flash);
        btnFlipCamera = view.findViewById(R.id.btn_flip_camera);
        tvWalletBalanceHeader = view.findViewById(R.id.tv_wallet_balance_header);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            llCaptureActions.setPadding(
                    llCaptureActions.getPaddingLeft(),
                    llCaptureActions.getPaddingTop(),
                    llCaptureActions.getPaddingRight(),
                    insets.bottom + 32
            );
            return WindowInsetsCompat.CONSUMED;
        });

        // Check camera permissions (startCamera will be invoked in onResume)
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }

        ImageButton btnVoiceInput = view.findViewById(R.id.btn_voice_input);
        if (btnVoiceInput != null) {
            btnVoiceInput.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Tính năng nhập bằng giọng nói sẽ sớm ra mắt", Toast.LENGTH_SHORT).show());
        }

        ImageButton btnGallery = view.findViewById(R.id.btn_gallery);
        if (btnGallery != null) {
            btnGallery.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Tính năng chọn ảnh từ thư viện sẽ sớm ra mắt", Toast.LENGTH_SHORT).show());
        }

        btnFlipCamera.setOnClickListener(v -> {
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK) 
                    ? CameraSelector.LENS_FACING_FRONT 
                    : CameraSelector.LENS_FACING_BACK;
            startCamera();
        });

        btnFlash.setOnClickListener(v -> {
            flashMode = (flashMode == ImageCapture.FLASH_MODE_OFF) 
                    ? ImageCapture.FLASH_MODE_ON 
                    : ImageCapture.FLASH_MODE_OFF;
            btnFlash.setAlpha(flashMode == ImageCapture.FLASH_MODE_ON ? 1.0f : 0.5f);
            if (imageCapture != null) {
                imageCapture.setFlashMode(flashMode);
            }
        });
        btnFlash.setAlpha(0.5f);

        viewModel.getWallets().observe(getViewLifecycleOwner(), wallets -> {
            if (wallets == null || wallets.isEmpty()) {
                tvWalletBalanceHeader.setText("+ Add Wallet");
                cachedWallets = null;
                selectedWalletId = -1;
            } else {
                cachedWallets = wallets;
                WalletModel selectedWallet = wallets.get(0);
                if (selectedWalletId != -1) {
                    for (WalletModel w : wallets) {
                        if (w.getId() == selectedWalletId) {
                            selectedWallet = w;
                            break;
                        }
                    }
                }
                selectedWalletId = selectedWallet.getId();
                updateWalletHeader(selectedWallet);
            }
        });

        tvWalletBalanceHeader.setOnClickListener(v -> {
            if (cachedWallets == null || cachedWallets.isEmpty()) {
                showAddWalletDialog();
            } else {
                PopupMenu popupMenu = new PopupMenu(requireContext(), tvWalletBalanceHeader);
                for (int i = 0; i < cachedWallets.size(); i++) {
                    WalletModel wallet = cachedWallets.get(i);
                    String menuItemTitle = wallet.getName() + " - " + formatFullCurrency(wallet.getBalance());
                    popupMenu.getMenu().add(0, i, i, menuItemTitle);
                }
                popupMenu.setOnMenuItemClickListener(item -> {
                    WalletModel selected = cachedWallets.get(item.getItemId());
                    selectedWalletId = selected.getId();
                    updateWalletHeader(selected);
                    return true;
                });
                popupMenu.show();
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();
        
        btnCapture.setOnClickListener(v -> takePhoto());
        
        btnSettings.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .add(android.R.id.content, new SettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });
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

    private void updateWalletHeader(WalletModel wallet) {
        if (tvWalletBalanceHeader != null) {
            String formatAmount = formatFullCurrency(wallet.getBalance());
            tvWalletBalanceHeader.setText(wallet.getName() + ": " + formatAmount);
        }
    }

    private String formatFullCurrency(double amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format.format(amount) + " ₫";
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    private void startCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                
                imageCapture = new ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .build();
                
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();
                
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);
                
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        // Shutter flash feedback animation
        View flashOverlay = getView() != null ? getView().findViewById(R.id.view_flash_overlay) : null;
        if (flashOverlay != null) {
            flashOverlay.setVisibility(View.VISIBLE);
            flashOverlay.setAlpha(0.75f);
            flashOverlay.animate()
                    .alpha(0.0f)
                    .setDuration(120)
                    .withEndAction(() -> flashOverlay.setVisibility(View.GONE))
                    .start();
        }
        
        File outputDirectory = new File(requireContext().getFilesDir(), "expense_images");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        
        String format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File photoFile = new File(outputDirectory, format + ".jpg");
        
        ImageCapture.Metadata metadata = new ImageCapture.Metadata();
        metadata.setReversedHorizontal(lensFacing == CameraSelector.LENS_FACING_FRONT);
        
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build();
        
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                requireActivity().runOnUiThread(() -> {
                    String savedImageUri = Uri.fromFile(photoFile).toString();
                    
                    // Launch TransactionFormFragment over the camera
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .add(android.R.id.content, TransactionFormFragment.newInstance(savedImageUri, selectedWalletId))
                            .addToBackStack(null)
                            .commit();
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
    }
}
