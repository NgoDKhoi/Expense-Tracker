package com.example.expensetracker.core.network.sync;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.LeaderboardProfileModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import com.example.expensetracker.core.domain.repository.ILeaderboardRepository;
import com.example.expensetracker.core.network.sync.dto.LeaderboardRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

@Singleton
public class LeaderboardRepositoryImpl implements ILeaderboardRepository {

    private final SupabaseSyncApi api;
    private final IAuthRepository authRepository;

    @Inject
    public LeaderboardRepositoryImpl(SupabaseSyncApi api, IAuthRepository authRepository) {
        this.api = api;
        this.authRepository = authRepository;
    }

    @Override
    public Result<Void> upsertMyProfile(LeaderboardProfileModel profile) {
        if (profile == null) {
            return Result.error("NULL_PROFILE", null);
        }
        Result<String> tokenResult = authRepository.getValidAccessToken();
        if (tokenResult == null || !tokenResult.isSuccess() || tokenResult.getData() == null) {
            return Result.error("NOT_SIGNED_IN", null);
        }
        try {
            LeaderboardRow row = new LeaderboardRow(
                    profile.getDisplayName(),
                    profile.getUsername(),
                    profile.getStreakDays(),
                    profile.getTotalSafeDays(),
                    profile.getStage()
            );
            Response<Void> response = api.upsertLeaderboardProfile(
                    "Bearer " + tokenResult.getData(),
                    Collections.singletonList(row)
            ).execute();
            if (!response.isSuccessful()) {
                return Result.error("HTTP_" + response.code(), null);
            }
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("NETWORK_ERROR", e);
        }
    }

    @Override
    public Result<List<LeaderboardProfileModel>> fetchTopProfiles(int limit) {
        Result<String> tokenResult = authRepository.getValidAccessToken();
        if (tokenResult == null || !tokenResult.isSuccess() || tokenResult.getData() == null) {
            return Result.error("NOT_SIGNED_IN", null);
        }
        try {
            Response<List<LeaderboardRow>> response = api.getLeaderboardProfiles(
                    "Bearer " + tokenResult.getData(),
                    limit
            ).execute();
            if (!response.isSuccessful() || response.body() == null) {
                return Result.error("HTTP_" + response.code(), null);
            }
            List<LeaderboardProfileModel> models = new ArrayList<>();
            for (LeaderboardRow row : response.body()) {
                if (row == null) continue;
                models.add(new LeaderboardProfileModel(
                        row.userId,
                        row.displayName,
                        row.username,
                        row.streakDays,
                        row.totalSafeDays,
                        row.stage
                ));
            }
            return Result.success(models);
        } catch (Exception e) {
            return Result.error("NETWORK_ERROR", e);
        }
    }
}
