package com.example.expensetracker.core.database.converter;

import androidx.room.TypeConverter;

import com.example.expensetracker.core.database.entity.SyncStatus;
import com.example.expensetracker.core.database.entity.TransactionType;

import java.util.Date;

public class Converters {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static String fromSyncStatus(SyncStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static SyncStatus toSyncStatus(String status) {
        return status == null ? null : SyncStatus.valueOf(status);
    }

    @TypeConverter
    public static String fromTransactionType(TransactionType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static TransactionType toTransactionType(String type) {
        return type == null ? null : TransactionType.valueOf(type);
    }
}
