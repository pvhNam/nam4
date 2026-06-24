package com.example.doanmb.ui.auth;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.repository.AuthRepository;
import com.example.doanmb.ui.base.BaseViewModel;

/** ViewModel màn đăng nhập (email hoặc số điện thoại). */
public class LoginViewModel extends BaseViewModel {

    private final AuthRepository authRepo = AuthRepository.getInstance();
    private final MutableLiveData<AuthRepository.Route> route = new MutableLiveData<>();

    public LiveData<AuthRepository.Route> getRoute() { return route; }

    public void login(String loginInput, String password) {
        if (loginInput.isEmpty() || password.isEmpty()) {
            postMessage("Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        if (loginInput.contains("@")) {
            if (!Patterns.EMAIL_ADDRESS.matcher(loginInput).matches()) {
                postMessage("Email không hợp lệ!");
                return;
            }
            signIn(loginInput, password);
        } else {
            authRepo.resolveEmailByPhone(loginInput, r -> {
                if (r.isSuccess()) signIn(r.getData(), password);
                else postMessage("Không tìm thấy tài khoản!");
            });
        }
    }

    private void signIn(String email, String password) {
        setLoading(true);
        authRepo.signIn(email, password, r -> {
            if (!r.isSuccess()) {
                setLoading(false);
                postMessage("Sai email/số điện thoại hoặc mật khẩu!");
                return;
            }
            authRepo.routeFor(r.getData(), rt -> {
                setLoading(false);
                postMessage("Đăng nhập thành công!");
                route.setValue(rt.isSuccess() && rt.getData() != null
                        ? rt.getData() : AuthRepository.Route.USER);
            });
        });
    }
}
