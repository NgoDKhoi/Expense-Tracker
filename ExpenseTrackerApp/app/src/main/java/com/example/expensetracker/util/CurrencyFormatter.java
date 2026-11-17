package com.example.expensetracker.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Centralized helper for persisting and formatting display currency symbols (₫, $, €).
 */
public final class CurrencyFormatter {

    public static final String PREFS_NAME = "expense_tracker_prefs";
    public static final String KEY_CURRENCY_SYMBOL = "pref_currency_symbol";
    public static final String SYMBOL_VND = "₫";
    public static final String SYMBOL_USD = "$";
    public static final String SYMBOL_EUR = "€";

    private CurrencyFormatter() {
    }

    public static String getSymbol(Context context) {
        if (context == null) {
            return SYMBOL_VND;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String symbol = prefs.getString(KEY_CURRENCY_SYMBOL, SYMBOL_VND);
        return (symbol == null || symbol.trim().isEmpty()) ? SYMBOL_VND : symbol.trim();
    }

    public static void setSymbol(Context context, String symbol) {
        if (context == null || symbol == null || symbol.trim().isEmpty()) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol.trim()).apply();
    }

    public static String formatFull(Context context, double amount) {
        return formatFullWithSymbol(amount, getSymbol(context));
    }

    public static String formatCompact(Context context, double amount) {
        return formatCompactWithSymbol(amount, getSymbol(context));
    }

    public static String formatFullWithSymbol(double amount, String symbol) {
        String safeSymbol = (symbol == null || symbol.trim().isEmpty()) ? SYMBOL_VND : symbol.trim();
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(amount) + " " + safeSymbol;
    }

    public static String formatCompactWithSymbol(double amount, String symbol) {
        String safeSymbol = (symbol == null || symbol.trim().isEmpty()) ? SYMBOL_VND : symbol.trim();
        double abs = Math.abs(amount);
        String prefix = amount < 0 ? "-" : "";
        if (abs >= 1_000_000_000) {
            return prefix + String.format(Locale.US, "%.1fB %s", abs / 1_000_000_000.0, safeSymbol);
        } else if (abs >= 1_000_000) {
            return prefix + String.format(Locale.US, "%.1fM %s", abs / 1_000_000.0, safeSymbol);
        } else if (abs >= 1_000) {
            return prefix + String.format(Locale.US, "%.1fK %s", abs / 1_000.0, safeSymbol);
        } else {
            return formatFullWithSymbol(amount, safeSymbol);
        }
    }
}
