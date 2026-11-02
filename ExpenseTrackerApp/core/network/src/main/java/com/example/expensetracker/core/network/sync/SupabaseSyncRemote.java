package com.example.expensetracker.core.network.sync;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.SyncPage;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.repository.ISyncRemote;
import com.example.expensetracker.core.network.auth.AuthErrorMapper;
import com.example.expensetracker.core.network.sync.dto.CategoryRow;
import com.example.expensetracker.core.network.sync.dto.TransactionRow;
import com.example.expensetracker.core.network.sync.dto.WalletRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/** ISyncRemote over Supabase PostgREST (tables from supabase/sync_schema.sql). */
@Singleton
public class SupabaseSyncRemote implements ISyncRemote {

    private final SupabaseSyncApi api;

    @Inject
    public SupabaseSyncRemote(SupabaseSyncApi api) {
        this.api = api;
    }

    @Override
    public Result<Void> pushWallets(String accessToken, List<WalletModel> wallets) {
        if (wallets.isEmpty()) return Result.success(null);
        return execute(api.upsertWallets(bearer(accessToken), map(wallets, SupabaseSyncRemote::toRow)));
    }

    @Override
    public Result<Void> pushCategories(String accessToken, List<CategoryModel> categories) {
        if (categories.isEmpty()) return Result.success(null);
        return execute(api.upsertCategories(bearer(accessToken), map(categories, SupabaseSyncRemote::toRow)));
    }

    @Override
    public Result<Void> pushTransactions(String accessToken, List<TransactionModel> transactions) {
        if (transactions.isEmpty()) return Result.success(null);
        return execute(api.upsertTransactions(bearer(accessToken), map(transactions, SupabaseSyncRemote::toRow)));
    }

    @Override
    public Result<SyncPage<WalletModel>> pullWallets(String accessToken, long sinceMillis, int limit) {
        return pull(api.pullWallets(bearer(accessToken), after(sinceMillis), limit), r -> r.syncedAt, SupabaseSyncRemote::toModel);
    }

    @Override
    public Result<SyncPage<CategoryModel>> pullCategories(String accessToken, long sinceMillis, int limit) {
        return pull(api.pullCategories(bearer(accessToken), after(sinceMillis), limit), r -> r.syncedAt, SupabaseSyncRemote::toModel);
    }

    @Override
    public Result<SyncPage<TransactionModel>> pullTransactions(String accessToken, long sinceMillis, int limit) {
        return pull(api.pullTransactions(bearer(accessToken), after(sinceMillis), limit), r -> r.syncedAt, SupabaseSyncRemote::toModel);
    }

    private static <R, M> Result<SyncPage<M>> pull(Call<List<R>> call, Function<R, String> syncedAt, Function<R, M> toModel) {
        try {
            Response<List<R>> response = call.execute();
            if (!response.isSuccessful()) return Result.error(errorMessage(response), null);
            List<R> rows = response.body() != null ? response.body() : new ArrayList<>();
            long max = rows.isEmpty() ? 0 : SyncTime.parse(syncedAt.apply(rows.get(rows.size() - 1)));
            return Result.success(new SyncPage<>(map(rows, toModel), max));
        } catch (IOException e) {
            return Result.error(AuthErrorMapper.NETWORK, e);
        } catch (RuntimeException e) {
            // Malformed JSON or timestamp from the server.
            return Result.error("Dữ liệu đồng bộ không hợp lệ", e);
        }
    }

    private static Result<Void> execute(Call<Void> call) {
        try {
            Response<Void> response = call.execute();
            if (!response.isSuccessful()) return Result.error(errorMessage(response), null);
            return Result.success(null);
        } catch (IOException e) {
            return Result.error(AuthErrorMapper.NETWORK, e);
        }
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private static String after(long sinceMillis) {
        return "gt." + SyncTime.format(sinceMillis);
    }

    private static <A, B> List<B> map(List<A> items, Function<A, B> f) {
        List<B> out = new ArrayList<>(items.size());
        for (A item : items) out.add(f.apply(item));
        return out;
    }

    private static String errorMessage(Response<?> response) {
        String body = null;
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) body = errorBody.string();
        } catch (IOException ignored) {
            // Use the HTTP code only.
        }
        return AuthErrorMapper.fromResponse(response.code(), body);
    }

    static WalletRow toRow(WalletModel m) {
        WalletRow r = new WalletRow();
        r.id = m.getUuid();
        r.name = m.getName();
        r.initialBalance = m.getInitialBalance();
        r.colorHex = m.getColorHex();
        r.iconName = m.getIconName();
        r.updatedAt = m.getUpdatedAt();
        r.deleted = m.isDeleted();
        return r;
    }

    static WalletModel toModel(WalletRow r) {
        WalletModel m = new WalletModel();
        m.setUuid(r.id);
        m.setName(r.name);
        m.setInitialBalance(r.initialBalance);
        m.setColorHex(r.colorHex);
        m.setIconName(r.iconName);
        m.setUpdatedAt(r.updatedAt);
        m.setDeleted(r.deleted);
        return m;
    }

    static CategoryRow toRow(CategoryModel m) {
        CategoryRow r = new CategoryRow();
        r.id = m.getUuid();
        r.name = m.getName();
        r.type = m.getType();
        r.colorHex = m.getColorHex();
        r.iconName = m.getIconName();
        r.updatedAt = m.getUpdatedAt();
        r.deleted = m.isDeleted();
        return r;
    }

    static CategoryModel toModel(CategoryRow r) {
        CategoryModel m = new CategoryModel();
        m.setUuid(r.id);
        m.setName(r.name);
        m.setType(r.type);
        m.setColorHex(r.colorHex);
        m.setIconName(r.iconName);
        m.setUpdatedAt(r.updatedAt);
        m.setDeleted(r.deleted);
        return m;
    }

    static TransactionRow toRow(TransactionModel m) {
        TransactionRow r = new TransactionRow();
        r.id = m.getUuid();
        r.walletId = m.getWalletUuid();
        r.amount = m.getAmount();
        // The server requires a type; the app treats a missing one as an expense.
        r.type = m.getType() != null ? m.getType() : "EXPENSE";
        r.category = m.getCategory();
        r.title = m.getTitle();
        r.note = m.getNote();
        r.occurredAt = m.getTimestamp() != null ? m.getTimestamp().getTime() : null;
        r.updatedAt = m.getUpdatedAt();
        r.deleted = m.isDeleted();
        return r;
    }

    static TransactionModel toModel(TransactionRow r) {
        TransactionModel m = new TransactionModel();
        m.setUuid(r.id);
        m.setWalletUuid(r.walletId);
        m.setAmount(r.amount);
        m.setType(r.type);
        m.setCategory(r.category);
        m.setTitle(r.title);
        m.setNote(r.note);
        m.setTimestamp(r.occurredAt != null ? new Date(r.occurredAt) : null);
        m.setUpdatedAt(r.updatedAt);
        m.setDeleted(r.deleted);
        return m;
    }
}
