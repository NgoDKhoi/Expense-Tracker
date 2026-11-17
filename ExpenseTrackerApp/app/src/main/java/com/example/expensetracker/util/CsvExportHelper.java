package com.example.expensetracker.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.TransactionModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Exports transactions to a UTF-8 BOM CSV file (Excel-compatible for Vietnamese text)
 * and opens the Android share sheet via FileProvider.
 */
public final class CsvExportHelper {

    private static final String TAG = "CsvExportHelper";
    private static final String UTF8_BOM = "\uFEFF";

    private CsvExportHelper() {
    }

    public static String buildCsvContent(List<TransactionModel> transactions, Map<Long, String> walletNames) {
        StringBuilder sb = new StringBuilder();
        sb.append(UTF8_BOM);
        sb.append("ID,Ngày giờ,Loại,Danh mục,Số tiền,Ví,Ghi chú\n");

        if (transactions == null || transactions.isEmpty()) {
            return sb.toString();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        for (TransactionModel tx : transactions) {
            if (tx == null) continue;
            String dateStr = tx.getTimestamp() != null ? dateFormat.format(tx.getTimestamp()) : "";
            String typeStr = tx.getType() != null ? tx.getType() : "";
            String catStr = tx.getCategory() != null ? tx.getCategory() : "";
            String amountStr = formatPlainAmount(tx.getAmount());

            String walletStr = "#" + tx.getWalletId();
            if (walletNames != null && walletNames.containsKey(tx.getWalletId())) {
                String resolved = walletNames.get(tx.getWalletId());
                if (resolved != null && !resolved.trim().isEmpty()) {
                    walletStr = resolved;
                }
            }

            String noteStr = tx.getNote() != null && !tx.getNote().trim().isEmpty()
                    ? tx.getNote()
                    : (tx.getTitle() != null ? tx.getTitle() : "");

            sb.append(tx.getId()).append(',')
                    .append(escapeCsv(dateStr)).append(',')
                    .append(escapeCsv(typeStr)).append(',')
                    .append(escapeCsv(catStr)).append(',')
                    .append(escapeCsv(amountStr)).append(',')
                    .append(escapeCsv(walletStr)).append(',')
                    .append(escapeCsv(noteStr)).append('\n');
        }
        return sb.toString();
    }

    public static void exportAndShareCsv(Context context,
                                         List<TransactionModel> transactions,
                                         Map<Long, String> walletNames) {
        if (context == null) return;
        try {
            File exportDir = new File(context.getCacheDir(), "exports");
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                Toast.makeText(context, R.string.msg_export_csv_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            File csvFile = new File(exportDir, "so_giao_dich.csv");
            String csvContent = buildCsvContent(transactions, walletNames);

            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(csvFile), StandardCharsets.UTF_8)) {
                writer.write(csvContent);
                writer.flush();
            }

            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    csvFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "so_giao_dich.csv");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareIntent, context.getString(R.string.chooser_share_csv));
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(chooser);
        } catch (Exception e) {
            Log.e(TAG, "Failed to export CSV", e);
            Toast.makeText(context, R.string.msg_export_csv_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private static String formatPlainAmount(double amount) {
        if (amount == Math.floor(amount) && !Double.isInfinite(amount)) {
            return String.valueOf((long) amount);
        }
        return String.valueOf(amount);
    }

    static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");
        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
