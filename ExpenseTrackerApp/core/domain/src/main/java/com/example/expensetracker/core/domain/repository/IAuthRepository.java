package com.example.expensetracker.core.domain.repository;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;

/**
 * Account operations. Network calls block, so callers must run them off the main thread.
 */
public interface IAuthRepository {
    Result<UserModel> signUp(String email, String password, String displayName);
    Result<UserModel> signIn(String email, String password);
    Result<Void> sendPasswordReset(String email);
    void signOut();
    /** Returns the locally saved user, or null when signed out. */
    UserModel getCurrentUser();
}
