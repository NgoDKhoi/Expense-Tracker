package com.example.expensetracker.core.network.di;

import com.example.expensetracker.core.domain.repository.IAuthRepository;
import com.example.expensetracker.core.domain.repository.ISyncRemote;
import com.example.expensetracker.core.network.BuildConfig;
import com.example.expensetracker.core.network.auth.AuthRepositoryImpl;
import com.example.expensetracker.core.network.auth.SupabaseAuthApi;
import com.example.expensetracker.core.network.sync.SupabaseSyncApi;
import com.example.expensetracker.core.network.sync.SupabaseSyncRemote;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public abstract class NetworkModule {

    @Binds
    @Singleton
    abstract IAuthRepository bindAuthRepository(AuthRepositoryImpl impl);

    @Binds
    @Singleton
    abstract ISyncRemote bindSyncRemote(SupabaseSyncRemote impl);

    // Retrofit stays inside these methods so modules depending on :core:network don't need it on their classpath.
    @Provides
    @Singleton
    static SupabaseAuthApi provideSupabaseAuthApi() {
        return retrofit(new Gson()).create(SupabaseAuthApi.class);
    }

    // Bulk upserts need every row to carry the same keys, so nulls are sent instead of dropped.
    @Provides
    @Singleton
    static SupabaseSyncApi provideSupabaseSyncApi() {
        return retrofit(new GsonBuilder().serializeNulls().create()).create(SupabaseSyncApi.class);
    }

    private static Retrofit retrofit(Gson gson) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .build()))
                .build();
        String url = BuildConfig.SUPABASE_URL;
        // Retrofit rejects an empty base URL; AuthRepositoryImpl reports the missing config instead.
        String baseUrl = url.isEmpty() ? "http://localhost/" : url.replaceAll("/+$", "") + "/";
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
}
