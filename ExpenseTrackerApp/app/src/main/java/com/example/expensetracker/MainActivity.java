package com.example.expensetracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.example.expensetracker.ui.CameraHostFragment;
import com.example.expensetracker.ui.DashboardFragment;
import com.example.expensetracker.ui.PhotosFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (!isGranted) {
                Toast.makeText(this, "Camera permission is required to add receipts", Toast.LENGTH_SHORT).show();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        viewPager = findViewById(R.id.main_view_pager);
        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        
        // Disable swipe if needed, or leave enabled. Currently enabled for horizontal swipe.
        
        // Start on Camera page (Index 1) - using post to ensure layout is ready
        viewPager.post(() -> viewPager.setCurrentItem(1, false));
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {

        public MainPagerAdapter(@NonNull AppCompatActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new DashboardFragment(); // Renamed from HomeFragment
                case 1:
                    return new CameraHostFragment();
                case 2:
                    return new PhotosFragment();
                default:
                    return new CameraHostFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
