package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.repository.ICategoryRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

public class DeleteCategoryUseCase {

    private static final Set<String> DEFAULT_CATEGORY_NAMES = new HashSet<>(Arrays.asList(
            "food",
            "drink",
            "transport",
            "utilities",
            "shopping",
            "entertainment",
            "other",
            "salary",
            "bonus",
            "investment"
    ));

    private final ICategoryRepository repository;

    @Inject
    public DeleteCategoryUseCase(ICategoryRepository repository) {
        this.repository = repository;
    }

    public void execute(long categoryId) {
        repository.deleteCategory(categoryId);
    }

    /**
     * Returns true only for user-created categories so seeded default categories stay protected.
     */
    public static boolean isCustomCategory(CategoryModel category) {
        if (category == null) {
            return false;
        }
        if ("custom".equalsIgnoreCase(category.getIconName())) {
            return true;
        }
        String name = category.getName();
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return !DEFAULT_CATEGORY_NAMES.contains(name.trim().toLowerCase(Locale.ROOT));
    }
}
