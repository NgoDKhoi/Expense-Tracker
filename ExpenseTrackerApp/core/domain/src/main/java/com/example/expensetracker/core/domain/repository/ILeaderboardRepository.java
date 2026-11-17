package com.example.expensetracker.core.domain.repository;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.LeaderboardProfileModel;

import java.util.List;

public interface ILeaderboardRepository {
    Result<Void> upsertMyProfile(LeaderboardProfileModel profile);
    Result<List<LeaderboardProfileModel>> fetchTopProfiles(int limit);
}
