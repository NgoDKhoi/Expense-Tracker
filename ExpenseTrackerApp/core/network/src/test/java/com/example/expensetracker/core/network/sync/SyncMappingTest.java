package com.example.expensetracker.core.network.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.network.sync.dto.TransactionRow;

import org.junit.Test;

import java.util.Date;

public class SyncMappingTest {

    @Test
    public void parse_handlesPostgresFormats() {
        long expected = 1_762_074_062_123L; // 2025-11-02T09:01:02.123Z
        assertEquals(expected, SyncTime.parse("2025-11-02T09:01:02.123456+00:00"));
        assertEquals(expected, SyncTime.parse("2025-11-02T09:01:02.123Z"));
        assertEquals(expected, SyncTime.parse("2025-11-02T16:01:02.123+07:00"));
        assertEquals(expected - 123, SyncTime.parse("2025-11-02T09:01:02+00"));
        assertEquals(expected + 377, SyncTime.parse("2025-11-02T09:01:02.5+00:00"));
    }

    @Test
    public void format_roundTrips() {
        long t = 1_762_074_062_123L;
        assertEquals("2025-11-02T09:01:02.123Z", SyncTime.format(t));
        assertEquals(t, SyncTime.parse(SyncTime.format(t)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_rejectsGarbage() {
        SyncTime.parse("yesterday");
    }

    @Test
    public void transaction_roundTripsAndDefaultsType() {
        TransactionModel m = new TransactionModel();
        m.setId(42);
        m.setUuid("t-uuid");
        m.setWalletUuid("w-uuid");
        m.setAmount(15000);
        m.setCategory("Food");
        m.setTimestamp(new Date(1000));
        m.setUpdatedAt(2000);
        m.setDeleted(true);

        TransactionRow row = SupabaseSyncRemote.toRow(m);
        assertEquals("t-uuid", row.id);
        assertEquals("w-uuid", row.walletId);
        assertEquals("EXPENSE", row.type);
        assertEquals(Long.valueOf(1000), row.occurredAt);
        assertNull(row.syncedAt);

        TransactionModel back = SupabaseSyncRemote.toModel(row);
        assertEquals("t-uuid", back.getUuid());
        assertEquals("w-uuid", back.getWalletUuid());
        assertEquals(0, back.getId()); // the local id is never sent
        assertEquals(15000, back.getAmount(), 0.001);
        assertEquals(1000, back.getTimestamp().getTime());
        assertEquals(2000, back.getUpdatedAt());
        assertTrue(back.isDeleted());
    }
}
