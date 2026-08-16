package com.example.expensetracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.usecase.GetAllTransactionsUseCase;

import java.util.ArrayList;

public class PhotosFragment extends Fragment {

    @javax.inject.Inject
    GetAllTransactionsUseCase getAllTransactionsUseCase;
    private PhotoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);

        RecyclerView rvPhotos = view.findViewById(R.id.rv_photos);
        rvPhotos.setLayoutManager(new GridLayoutManager(getContext(), 3));
        
        adapter = new PhotoAdapter(requireContext(), new ArrayList<>());
        rvPhotos.setAdapter(adapter);

        getAllTransactionsUseCase.execute().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                adapter.setExpenses(expenses);
            }
        });
    }
}

