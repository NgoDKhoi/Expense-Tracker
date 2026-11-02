package com.example.expensetracker.core.domain.repository;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;

/**
 * Account operations. Network calls block, so callers must run them off the main thread.
 */
public interface IAuthRepository {
    Result<UserModel> signUp(String email, String password, String displayName, String username);
    Result<UserModel> signIn(String email, String password);
    Result<Void> sendPasswordReset(String email);
    /** Email registered for the username; success with null data when the username is free. */
    Result<String> findEmailByUsername(String username);
    void signOut();
    /** Returns the locally saved user, or null when signed out. */
    UserModel getCurrentUser();
    /**
     * Access token for calls to the database, refreshed first when it is about to expire.
     * Success with null = signed out (or the server rejected the session, which signs the user out).
     * Error = network problem; the session is kept, try again later.
     */
    Result<String> getValidAccessToken();
}
