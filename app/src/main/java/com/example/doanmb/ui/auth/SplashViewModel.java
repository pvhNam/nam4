package com.example.doanmb.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.repository.AuthRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.auth.FirebaseUser;

/** ViewModel màn khởi động: kiểm tra phiên đăng nhập & điều hướng theo vai trò. */
public class SplashViewModel extends BaseViewModel {

    private final AuthRepository authRepo = AuthRepository.getInstance();
    private final MutableLiveData<AuthRepository.Route> route = new MutableLiveData<>();

    public LiveData<AuthRepository.Route> getRoute() { return route; }

    public boolean hasSession() { return authRepo.getCurrentUser() != null; }

    public void resolveRoute() {
        FirebaseUser user = authRepo.getCurrentUser();
        if (user == null) return;
        authRepo.routeFor(user.getUid(), r ->
                route.setValue(r.isSuccess() && r.getData() != null
                        ? r.getData() : AuthRepository.Route.USER));
    }
}
