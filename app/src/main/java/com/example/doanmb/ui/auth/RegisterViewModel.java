package com.example.doanmb.ui.auth;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.AuthRepository;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.ui.base.BaseViewModel;

import java.util.HashMap;
import java.util.Map;

/** ViewModel màn đăng ký tài khoản khách. */
public class RegisterViewModel extends BaseViewModel {

    private final AuthRepository authRepo = AuthRepository.getInstance();
    private final UserRepository userRepo = UserRepository.getInstance();
    private final MutableLiveData<Boolean> success = new MutableLiveData<>();

    public LiveData<Boolean> getSuccess() { return success; }

    public void register(String name, String phone, String email, String password, String confirm) {
        if (name.isEmpty() || phone.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            postMessage("Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            postMessage("Email không hợp lệ!");
            return;
        }
        if (!password.equals(confirm)) {
            postMessage("Mật khẩu không hợp lệ!");
            return;
        }

        setLoading(true);
        authRepo.register(email, password, r -> {
            if (!r.isSuccess()) {
                setLoading(false);
                postMessage("Lỗi đăng ký: " + r.getError());
                return;
            }
            String uid = r.getData();
            Map<String, Object> user = new HashMap<>();
            user.put(F.UID, uid);
            user.put(F.NAME, name);
            user.put(F.EMAIL, email);
            user.put(F.PHONE, phone);
            user.put(F.ROLE, "CUSTOMER");
            userRepo.createUser(uid, user, res -> {
                setLoading(false);
                if (res.isSuccess()) { postMessage("Đăng ký thành công!"); success.setValue(true); }
                else postMessage("Lỗi lưu user: " + res.getError());
            });
        });
    }
}
