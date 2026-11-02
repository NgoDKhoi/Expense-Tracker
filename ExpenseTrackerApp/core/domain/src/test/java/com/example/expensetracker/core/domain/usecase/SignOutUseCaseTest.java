package com.example.expensetracker.core.domain.usecase;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.example.expensetracker.core.domain.repository.IAuthRepository;
import com.example.expensetracker.core.domain.repository.ISyncScheduler;

import org.junit.Test;
import org.mockito.InOrder;

public class SignOutUseCaseTest {

    @Test
    public void signsOutFirst_thenStopsSync_thenWipesDevice() {
        IAuthRepository auth = mock(IAuthRepository.class);
        ISyncScheduler scheduler = mock(ISyncScheduler.class);
        SyncDataUseCase sync = mock(SyncDataUseCase.class);

        new SignOutUseCase(auth, scheduler, sync).execute();

        InOrder order = inOrder(auth, scheduler, sync);
        order.verify(auth).signOut();
        order.verify(scheduler).cancelAll();
        order.verify(sync).clearLocalData();
    }
}
