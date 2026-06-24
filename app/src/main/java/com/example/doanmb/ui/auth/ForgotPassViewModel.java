package com.example.doanmb.ui.auth;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.repository.AuthRepository;
import com.example.doanmb.ui.base.BaseViewModel;

/** ViewModel màn quên mật khẩu. */
public class ForgotPassViewModel extends BaseViewModel {

    private final AuthRepository authRepo = AuthRepository.getInstance();
    private final MutableLiveData<Boolean> success = new MutableLiveData<>();

    public LiveData<Boolean> getSuccess() { return success; }

    public void reset(String email) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            postMessage("Email không hợp lệ!");
            return;
        }
        authRepo.sendPasswordReset(email, r -> {
            if (r.isSuccess()) { postMessage("Vui lòng kiểm tra email để lấy lại mật khẩu!"); success.setValue(true); }
            else postMessage("Lỗi: " + r.getError());
        });
    }
}
