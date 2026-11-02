package com.example.expensetracker.core.network.di;

import com.example.expensetracker.core.domain.repository.IAuthRepository;
import com.example.expensetracker.core.network.BuildConfig;
import com.example.expensetracker.core.network.auth.AuthRepositoryImpl;
import com.example.expensetracker.core.network.auth.SupabaseAuthApi;

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

    // Retrofit stays inside this method so modules depending on :core:network don't need it on their classpath.
    @Provides
    @Singleton
    static SupabaseAuthApi provideSupabaseAuthApi() {
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
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SupabaseAuthApi.class);
    }
}
