package com.example.expensetracker.core.database.mapper;

import com.example.expensetracker.core.database.entity.CategoryTotal;
import com.example.expensetracker.core.database.entity.TransactionEntity;
import com.example.expensetracker.core.database.entity.SyncStatus;
import com.example.expensetracker.core.database.entity.TransactionType;
import com.example.expensetracker.core.database.entity.WalletEntity;
import com.example.expensetracker.core.domain.model.CategoryTotalModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;

import java.util.ArrayList;
import java.util.List;

public class EntityMapper {

    public static TransactionModel toModel(TransactionEntity entity) {
        if (entity == null) return null;
        TransactionModel model = new TransactionModel();
        model.setId(entity.getId());
        model.setAmount(entity.getAmount());
        model.setTitle(entity.getTitle());
        model.setCategory(entity.getCategory());
        model.setImageUri(entity.getImageUri());
        model.setTimestamp(entity.getTimestamp());
        model.setSyncStatus(entity.getSyncStatus() != null ? entity.getSyncStatus().name() : null);
        model.setNote(entity.getNote());
        model.setWalletId(entity.getWalletId());
        model.setType(entity.getType() != null ? entity.getType().name() : null);
        return model;
    }

    public static TransactionEntity toEntity(TransactionModel model) {
        if (model == null) return null;
        TransactionEntity entity = new TransactionEntity();
        entity.setId(model.getId());
        entity.setAmount(model.getAmount());
        entity.setTitle(model.getTitle());
        entity.setCategory(model.getCategory());
        entity.setImageUri(model.getImageUri());
        entity.setTimestamp(model.getTimestamp());
        if (model.getSyncStatus() != null) {
            try {
                entity.setSyncStatus(SyncStatus.valueOf(model.getSyncStatus()));
            } catch (IllegalArgumentException e) {
                entity.setSyncStatus(SyncStatus.PENDING);
            }
        }
        entity.setNote(model.getNote());
        entity.setWalletId(model.getWalletId());
        if (model.getType() != null) {
            try {
                entity.setType(TransactionType.valueOf(model.getType()));
            } catch (IllegalArgumentException e) {
                entity.setType(TransactionType.EXPENSE);
            }
        }
        return entity;
    }

    public static WalletModel toModel(WalletEntity entity) {
        if (entity == null) return null;
        WalletModel model = new WalletModel();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setBalance(entity.getBalance());
        return model;
    }

    public static WalletEntity toEntity(WalletModel model) {
        if (model == null) return null;
        WalletEntity entity = new WalletEntity();
        entity.setId(model.getId());
        entity.setName(model.getName());
        entity.setBalance(model.getBalance());
        return entity;
    }

    public static CategoryTotalModel toModel(CategoryTotal entity) {
        if (entity == null) return null;
        CategoryTotalModel model = new CategoryTotalModel();
        model.setCategory(entity.getCategory());
        model.setTotal(entity.getTotal());
        return model;
    }

    public static List<TransactionModel> toModelList(List<TransactionEntity> entities) {
        if (entities == null) return null;
        List<TransactionModel> models = new ArrayList<>();
        for (TransactionEntity entity : entities) {
            models.add(toModel(entity));
        }
        return models;
    }

    public static List<WalletModel> toWalletModelList(List<WalletEntity> entities) {
        if (entities == null) return null;
        List<WalletModel> models = new ArrayList<>();
        for (WalletEntity entity : entities) {
            models.add(toModel(entity));
        }
        return models;
    }

    public static List<CategoryTotalModel> toCategoryTotalModelList(List<CategoryTotal> entities) {
        if (entities == null) return null;
        List<CategoryTotalModel> models = new ArrayList<>();
        for (CategoryTotal entity : entities) {
            models.add(toModel(entity));
        }
        return models;
    }
}
