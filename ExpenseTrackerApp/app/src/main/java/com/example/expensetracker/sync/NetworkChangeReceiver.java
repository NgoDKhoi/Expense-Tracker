package com.example.expensetracker.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;

import androidx.annotation.NonNull;

import com.example.expensetracker.core.domain.repository.ISyncScheduler;

/**
 * Asks for a sync whenever the phone gets a connection. Android also calls onAvailable right after
 * registering when already online, so this doubles as the "sync on app start".
 */
public final class NetworkChangeReceiver {
    private NetworkChangeReceiver() {
    }

    /** Call once from Application.onCreate; the callback lives as long as the process. */
    public static void register(Context context, ISyncScheduler scheduler) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        if (cm == null) return;
        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                scheduler.requestSync();
            }
        });
    }
}
