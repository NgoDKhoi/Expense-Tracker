package com.example.expensetracker.viewmodel;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.usecase.AddCategoryUseCase;
import com.example.expensetracker.core.domain.usecase.AddTransactionUseCase;
import com.example.expensetracker.core.domain.usecase.GetAllWalletsUseCase;
import com.example.expensetracker.core.domain.usecase.GetCategoriesUseCase;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

/**
 * ViewModel for the Add Expense screen.
 * Receives dependencies via constructor injection (no Android Context).
 */
public class AddTransactionViewModel extends ViewModel {

    private static final String TAG = "AddTransactionVM";

    private static final String[] INCOME_KEYWORDS = {
            "nhận", "thu", "lương", "thưởng", "lãi", "hoàn",
            "biếu", "tặng", "cổ tức", "salary", "bonus", "income", "refund"
    };

    private static final String[] EXPENSE_KEYWORDS = {
            "mua", "chi", "tiêu", "trả", "đóng", "ăn",
            "uống", "đổ", "gửi", "nạp", "thuê", "sửa", "khám"
    };

    private static final Pattern THOUSAND_GROUP_PATTERN = Pattern.compile("(\\d)[,.](\\d{3})(?!\\d)");
    private static final Pattern MILLION_NUM_HALF_PATTERN = Pattern.compile("(\\d+)\\s*(?:triệu|tr|củ)\\s*rưỡi(?![\\p{L}\\p{N}])");
    private static final Pattern MILLION_HALF_PATTERN = Pattern.compile("(?<![\\p{L}\\p{N}])(?:một\\s+)?(?:triệu|tr|củ)\\s*rưỡi(?![\\p{L}\\p{N}])");
    private static final Pattern HUNDRED_NUM_HALF_PATTERN = Pattern.compile("(\\d+)\\s*trăm\\s*rưỡi(?![\\p{L}\\p{N}])");
    private static final Pattern HUNDRED_HALF_PATTERN = Pattern.compile("(?<![\\p{L}\\p{N}])(?:một\\s+)?trăm\\s*rưỡi(?![\\p{L}\\p{N}])");
    private static final Pattern COMPOUND_MILLION_PATTERN = Pattern.compile("(\\d+)\\s*(?:triệu|tr|củ)\\s*(\\d+)");
    private static final Pattern MILLION_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:triệu|tr|củ|m)(?![\\p{L}\\p{N}])");
    private static final Pattern HUNDRED_THOUSAND_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*trăm(?:\\s*(?:nghìn|ngàn|k))?(?![\\p{L}\\p{N}])");
    private static final Pattern TEN_THOUSAND_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*chục(?:\\s*(?:nghìn|ngàn|k))?(?![\\p{L}\\p{N}])");
    private static final Pattern THOUSAND_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:nghìn|ngàn|nhành|cành|k)(?![\\p{L}\\p{N}])");
    private static final Pattern PLAIN_NUMBER_PATTERN = Pattern.compile("(?<![\\p{L}\\p{N}])(\\d+)(?![\\p{L}\\p{N}])");

    private static final Pattern STRIP_DATE_PATTERN = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:hôm\\s+nay|hôm\\s+qua|hôm\\s+kia|yesterday|today)(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern STRIP_AMOUNT_PATTERN = Pattern.compile(
            "(?iu)"
                    + "\\d+\\s*(?:triệu|tr|củ)\\s*(?:rưỡi|\\d+)(?![\\p{L}\\p{N}])"
                    + "|(?<![\\p{L}\\p{N}])(?:một\\s+)?(?:triệu|tr|củ)\\s*rưỡi(?![\\p{L}\\p{N}])"
                    + "|\\d+\\s*trăm\\s*rưỡi(?![\\p{L}\\p{N}])"
                    + "|(?<![\\p{L}\\p{N}])(?:một\\s+)?trăm\\s*rưỡi(?![\\p{L}\\p{N}])"
                    + "|\\d+(?:[.,]\\d+)?\\s*(?:triệu|tr|củ|m)(?![\\p{L}\\p{N}])"
                    + "|\\d+(?:[.,]\\d+)?\\s*(?:trăm|chục)(?:\\s*(?:nghìn|ngàn|k))?(?![\\p{L}\\p{N}])"
                    + "|\\d+(?:[.,]\\d+)?\\s*(?:nghìn|ngàn|nhành|cành|k)(?![\\p{L}\\p{N}])"
                    + "|(?<![\\p{L}\\p{N}])\\d{1,3}(?:[.,]\\d{3})+(?![\\p{L}\\p{N}])"
                    + "|(?<![\\p{L}\\p{N}])\\d+(?![\\p{L}\\p{N}])"
    );
    private static final Pattern STRIP_TRAILING_FILLER_PATTERN = Pattern.compile(
            "(?iu)(?:^|\\s+)(?:đồng|đ|vnđ|vnd|hết|mất|khoảng|tầm|với\\s+giá|giá)(?![\\p{L}\\p{N}])\\s*$"
    );

    public static final class ParsedVoiceExpense {
        public final double amount;
        public final String type;
        public final String category;
        public final String note;
        public final int dayOffset;

        public ParsedVoiceExpense(double amount, String type, String category, String note) {
            this(amount, type, category, note, 0);
        }

        public ParsedVoiceExpense(double amount, String type, String category, String note, int dayOffset) {
            this.amount = amount;
            this.type = type;
            this.category = category;
            this.note = note;
            this.dayOffset = dayOffset;
        }
    }

    private final AddTransactionUseCase addTransactionUseCase;
    private final GetAllWalletsUseCase getAllWalletsUseCase;
    private final GetCategoriesUseCase getCategoriesUseCase;
    private final AddCategoryUseCase addCategoryUseCase;
    private final LiveData<List<WalletModel>> wallets;

    @Inject
    public AddTransactionViewModel(
            AddTransactionUseCase addTransactionUseCase,
            GetAllWalletsUseCase getAllWalletsUseCase,
            GetCategoriesUseCase getCategoriesUseCase,
            AddCategoryUseCase addCategoryUseCase) {

        Log.d(TAG, "AddTransactionViewModel created with injected use cases.");

        this.addTransactionUseCase = addTransactionUseCase;
        this.getAllWalletsUseCase = getAllWalletsUseCase;
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.addCategoryUseCase = addCategoryUseCase;
        this.wallets = getAllWalletsUseCase.execute();
    }

    public LiveData<List<WalletModel>> getWallets() {
        return wallets;
    }

    public LiveData<List<CategoryModel>> getCategories(String type) {
        return getCategoriesUseCase.execute(type);
    }

    public void addCategory(String name, String type, String colorHex, String iconName) {
        addCategoryUseCase.execute(new CategoryModel(name, type, colorHex, iconName));
    }

    public void saveExpense(double amount, String title, String category,
                            String imageUri, String note, long walletId, String type) {
        saveExpense(amount, title, category, imageUri, note, walletId, type, new Date());
    }

    public void saveExpense(double amount, String title, String category,
                            String imageUri, String note, long walletId, String type, Date timestamp) {
        Log.d(TAG, "saveExpense: title=" + title + ", amount=" + amount
                + ", category=" + category + ", type=" + type + ", note=" + note);

        TransactionModel record = new TransactionModel.Builder()
                .amount(amount)
                .title(title)
                .category(category)
                .imageUri(imageUri)
                .timestamp(timestamp != null ? timestamp : new Date())
                .syncStatus("PENDING")
                .note(note)
                .walletId(walletId)
                .type(type)
                .build();

        addTransactionUseCase.execute(record);
    }

    /**
     * Parses a natural-language expense/income utterance (Vietnamese or English) into structured fields.
     */
    public static ParsedVoiceExpense parseVoiceInput(@Nullable String rawText,
                                                     @Nullable List<CategoryModel> customCategories) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return new ParsedVoiceExpense(0.0, "EXPENSE", "Other", "", 0);
        }
        String trimmed = rawText.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        String type = detectTransactionType(lower);
        double amount = extractAmount(lower);
        int dayOffset = extractDayOffset(lower);
        String category = matchCategory(lower, type, customCategories);
        String note = cleanSpokenNote(trimmed);

        return new ParsedVoiceExpense(amount, type, category, note, dayOffset);
    }

    private static String detectTransactionType(String lower) {
        int incomeIdx = firstKeywordIndex(lower, INCOME_KEYWORDS);
        if (incomeIdx == Integer.MAX_VALUE) {
            return "EXPENSE";
        }
        int expenseIdx = firstKeywordIndex(lower, EXPENSE_KEYWORDS);
        return incomeIdx < expenseIdx ? "INCOME" : "EXPENSE";
    }

    private static int firstKeywordIndex(String lower, String[] keywords) {
        int earliest = Integer.MAX_VALUE;
        for (String keyword : keywords) {
            Pattern p = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(keyword) + "(?![\\p{L}\\p{N}])");
            Matcher m = p.matcher(lower);
            if (m.find() && m.start() < earliest) {
                earliest = m.start();
            }
        }
        return earliest;
    }

    private static int extractDayOffset(String lower) {
        if (lower.contains("hôm kia")) {
            return -2;
        }
        if (lower.contains("hôm qua") || lower.contains("yesterday")) {
            return -1;
        }
        return 0;
    }

    private static String cleanSpokenNote(String rawText) {
        String cleaned = STRIP_DATE_PATTERN.matcher(rawText).replaceAll(" ");
        cleaned = STRIP_AMOUNT_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
        cleaned = STRIP_TRAILING_FILLER_PATTERN.matcher(cleaned).replaceAll("").trim();

        if (cleaned.isEmpty()) {
            return rawText.trim();
        }
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    private static double extractAmount(String lower) {
        String normalized = lower;
        while (true) {
            Matcher m = THOUSAND_GROUP_PATTERN.matcher(normalized);
            if (!m.find()) {
                break;
            }
            normalized = m.replaceAll("$1$2");
        }

        Matcher millionNumHalf = MILLION_NUM_HALF_PATTERN.matcher(normalized);
        if (millionNumHalf.find()) {
            long millions = Long.parseLong(millionNumHalf.group(1));
            return millions * 1_000_000.0 + 500_000.0;
        }

        Matcher millionHalf = MILLION_HALF_PATTERN.matcher(normalized);
        if (millionHalf.find()) {
            return 1_500_000.0;
        }

        Matcher hundredNumHalf = HUNDRED_NUM_HALF_PATTERN.matcher(normalized);
        if (hundredNumHalf.find()) {
            long hundreds = Long.parseLong(hundredNumHalf.group(1));
            return hundreds * 100_000.0 + 50_000.0;
        }

        Matcher hundredHalf = HUNDRED_HALF_PATTERN.matcher(normalized);
        if (hundredHalf.find()) {
            return 150_000.0;
        }

        Matcher compoundMillion = COMPOUND_MILLION_PATTERN.matcher(normalized);
        if (compoundMillion.find()) {
            long millions = Long.parseLong(compoundMillion.group(1));
            String tail = compoundMillion.group(2);
            long tailRaw = Long.parseLong(tail);
            double tailValue;
            if (tail.length() == 1) {
                tailValue = tailRaw * 100_000.0;
            } else if (tail.length() == 2) {
                tailValue = tailRaw * 10_000.0;
            } else {
                tailValue = tailRaw * 1_000.0;
            }
            return millions * 1_000_000.0 + tailValue;
        }

        Matcher million = MILLION_PATTERN.matcher(normalized);
        if (million.find()) {
            double val = parseFlexibleDouble(million.group(1));
            return val * 1_000_000.0;
        }

        Matcher hundredThousand = HUNDRED_THOUSAND_PATTERN.matcher(normalized);
        if (hundredThousand.find()) {
            double val = parseFlexibleDouble(hundredThousand.group(1));
            return val * 100_000.0;
        }

        Matcher tenThousand = TEN_THOUSAND_PATTERN.matcher(normalized);
        if (tenThousand.find()) {
            double val = parseFlexibleDouble(tenThousand.group(1));
            return val * 10_000.0;
        }

        Matcher thousand = THOUSAND_PATTERN.matcher(normalized);
        if (thousand.find()) {
            double val = parseFlexibleDouble(thousand.group(1));
            return val * 1_000.0;
        }

        Matcher plain = PLAIN_NUMBER_PATTERN.matcher(normalized);
        if (plain.find()) {
            double val = Double.parseDouble(plain.group(1));
            if (val > 0 && val < 1000) {
                return val * 1_000.0;
            }
            return val;
        }

        return 0.0;
    }

    private static double parseFlexibleDouble(String raw) {
        return Double.parseDouble(raw.replace(',', '.'));
    }

    private static String matchCategory(String lower, String type, @Nullable List<CategoryModel> customCategories) {
        if (customCategories != null) {
            for (CategoryModel c : customCategories) {
                if (c == null || c.getName() == null || c.getName().trim().isEmpty()) {
                    continue;
                }
                if (c.getType() != null && !type.equalsIgnoreCase(c.getType())) {
                    continue;
                }
                if (lower.contains(c.getName().toLowerCase(Locale.ROOT))) {
                    return c.getName();
                }
            }
        }

        if ("INCOME".equals(type)) {
            if (containsAny(lower, "thưởng", "bonus", "lì xì", "quà", "tặng", "biếu")) {
                return "Bonus";
            }
            if (containsAny(lower, "đầu tư", "lãi", "cổ phiếu", "cổ tức", "tiết kiệm", "bán được", "investment", "crypto")) {
                return "Investment";
            }
            if (containsAny(lower, "lương", "salary", "thu nhập")) {
                return "Salary";
            }
            return "Other";
        }

        if (containsAny(lower, "ăn", "uống", "phở", "bún", "cơm", "cà phê", "cafe", "coffee",
                "trà sữa", "nhậu", "bánh", "sáng", "trưa", "tối", "chợ", "siêu thị", "food", "lunch", "dinner")) {
            return "Food";
        }
        if (containsAny(lower, "xăng", "xe", "grab", "be", "taxi", "bus", "buýt",
                "gửi xe", "vé", "tàu", "bay", "transport", "gas", "parking")) {
            return "Transport";
        }
        if (containsAny(lower, "điện", "nước", "mạng", "internet", "wifi", "điện thoại",
                "4g", "5g", "tiền nhà", "thuê nhà", "rác", "bill", "utilities")) {
            return "Utilities";
        }
        if (containsAny(lower, "mua", "áo", "quần", "giày", "dép", "mỹ phẩm",
                "shopee", "lazada", "tiktok", "tiki", "shopping", "sắm")) {
            return "Shopping";
        }
        if (containsAny(lower, "phim", "game", "chơi", "du lịch", "netflix", "spotify",
                "karaoke", "bóng đá", "concert", "entertainment")) {
            return "Entertainment";
        }
        return "Other";
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
