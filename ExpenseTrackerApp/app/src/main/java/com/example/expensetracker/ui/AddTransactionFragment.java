package com.example.expensetracker.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.core.domain.model.WalletModel;
// removed DependencyContainer
import java.util.List;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.expensetracker.R;
import com.example.expensetracker.viewmodel.AddTransactionViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTransactionFragment extends Fragment {

    @javax.inject.Inject
    ViewModelProvider.Factory viewModelFactory;
    private AddTransactionViewModel viewModel;
    private PreviewView cameraPreview;
    private ImageView ivCapturedImage;
    private Button btnSave;
    private EditText etAmount;
    private String selectedCategory = "Other";
    private String savedImageUri = null;
    private List<WalletModel> walletList = new ArrayList<>();
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private Button btnCatFood, btnCatTransport, btnCatUtilities, btnCatShopping, btnCatOther;
    private MaterialButtonToggleGroup toggleTransactionType;
    private String currentTransactionType = "EXPENSE";

    private View llTopHeader, llCaptureActions, llInputForm;
    private android.widget.ImageButton btnRetake;
    private android.widget.TextView tvWalletBalanceHeader;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnCapture;
    
    private long defaultWalletId = -1;

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
        ivCapturedImage = view.findViewById(R.id.iv_captured_image);
        btnCapture = view.findViewById(R.id.btn_capture);
        etAmount = view.findViewById(R.id.et_amount);
        btnSave = view.findViewById(R.id.btn_save);
        
        llTopHeader = view.findViewById(R.id.ll_top_header);
        llCaptureActions = view.findViewById(R.id.ll_capture_actions);
        llInputForm = view.findViewById(R.id.ll_input_form);
        btnRetake = view.findViewById(R.id.btn_retake);
        tvWalletBalanceHeader = view.findViewById(R.id.tv_wallet_balance_header);
        
        btnCatFood = view.findViewById(R.id.btn_cat_food);
        btnCatTransport = view.findViewById(R.id.btn_cat_transport);
        btnCatUtilities = view.findViewById(R.id.btn_cat_utilities);
        btnCatShopping = view.findViewById(R.id.btn_cat_shopping);
        btnCatOther = view.findViewById(R.id.btn_cat_other);
        
        toggleTransactionType = view.findViewById(R.id.toggle_transaction_type);
        
        toggleTransactionType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_type_income) {
                    currentTransactionType = "INCOME";
                    btnCatFood.setText("Salary");
                    btnCatTransport.setText("Gift");
                    btnCatUtilities.setText("Investment");
                    btnCatShopping.setVisibility(View.GONE);
                } else {
                    currentTransactionType = "EXPENSE";
                    btnCatFood.setText(R.string.cat_food);
                    btnCatTransport.setText(R.string.cat_transport);
                    btnCatUtilities.setText(R.string.cat_utilities);
                    btnCatShopping.setVisibility(View.VISIBLE);
                    btnCatShopping.setText(R.string.cat_shopping);
                }
                btnCatOther.callOnClick();
            }
        });
        
        viewModel.getWallets().observe(getViewLifecycleOwner(), wallets -> {
            this.walletList = wallets;
            double totalBalance = 0;
            if (wallets != null && !wallets.isEmpty()) {
                defaultWalletId = wallets.get(0).getId();
                for (WalletModel w : wallets) {
                    totalBalance += w.getBalance();
                }
            }
            tvWalletBalanceHeader.setText(String.format(Locale.getDefault(), "$%.2f", totalBalance));
        });
        
        setupCategoryButtons();
        
        cameraExecutor = Executors.newSingleThreadExecutor();
        startCamera();
        
        btnCapture.setOnClickListener(v -> takePhoto());
        btnRetake.setOnClickListener(v -> resetFormToCamera());
        
        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                etAmount.setError("Amount is required");
                etAmount.requestFocus();
                return;
            }
            if (defaultWalletId == -1) {
                Toast.makeText(requireContext(), "Please create a wallet first", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String titleStr = ""; // Removed title field
            String note = ""; // Removed note field
            
            viewModel.saveExpense(amount, titleStr, selectedCategory, savedImageUri, note, defaultWalletId, currentTransactionType);
            Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show();
            
            resetFormToCamera();
        });
    }

    private void resetFormToCamera() {
        ivCapturedImage.setVisibility(View.GONE);
        llInputForm.setVisibility(View.GONE);
        llTopHeader.setVisibility(View.VISIBLE);
        llCaptureActions.setVisibility(View.VISIBLE);
        etAmount.setText("");
        savedImageUri = null;
    }

    private void setupCategoryButtons() {
        View.OnClickListener listener = v -> {
            btnCatFood.setAlpha(0.5f);
            btnCatTransport.setAlpha(0.5f);
            btnCatUtilities.setAlpha(0.5f);
            btnCatShopping.setAlpha(0.5f);
            btnCatOther.setAlpha(0.5f);
            
            v.setAlpha(1.0f);
            
            if (v.getId() == R.id.btn_cat_food) selectedCategory = "INCOME".equals(currentTransactionType) ? "Salary" : "Food";
            else if (v.getId() == R.id.btn_cat_transport) selectedCategory = "INCOME".equals(currentTransactionType) ? "Gift" : "Transport";
            else if (v.getId() == R.id.btn_cat_utilities) selectedCategory = "INCOME".equals(currentTransactionType) ? "Investment" : "Utilities";
            else if (v.getId() == R.id.btn_cat_shopping) selectedCategory = "Shopping";
            else if (v.getId() == R.id.btn_cat_other) selectedCategory = "Other";
        };
        
        btnCatFood.setOnClickListener(listener);
        btnCatTransport.setOnClickListener(listener);
        btnCatUtilities.setOnClickListener(listener);
        btnCatShopping.setOnClickListener(listener);
        btnCatOther.setOnClickListener(listener);
        
        btnCatOther.callOnClick();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                
                imageCapture = new ImageCapture.Builder().build();
                
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);
                
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        
        File outputDirectory = new File(requireContext().getFilesDir(), "expense_images");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        
        String format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File photoFile = new File(outputDirectory, format + ".jpg");
        
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                requireActivity().runOnUiThread(() -> {
                    savedImageUri = Uri.fromFile(photoFile).toString();
                    ivCapturedImage.setImageURI(Uri.parse(savedImageUri));
                    ivCapturedImage.setVisibility(View.VISIBLE);
                    llTopHeader.setVisibility(View.GONE);
                    llCaptureActions.setVisibility(View.GONE);
                    llInputForm.setVisibility(View.VISIBLE);
                    etAmount.requestFocus();
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
