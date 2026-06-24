package com.example.doanmb.ui.admin;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.data.repository.WalletRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.example.doanmb.util.WalletHelper;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/** ViewModel cho màn Admin quản lý người dùng. */
public class AdminUsersViewModel extends BaseViewModel {

    private final UserRepository userRepo = UserRepository.getInstance();
    private final WalletRepository walletRepo = WalletRepository.getInstance();
    private final MutableLiveData<List<DocumentSnapshot>> users = new MutableLiveData<>();

    public LiveData<List<DocumentSnapshot>> getUsers() { return users; }

    public void load() {
        setLoading(true);
        userRepo.getAllUserDocs(r -> {
            setLoading(false);
            if (r.isSuccess()) users.setValue(r.getData());
            else postMessage("Lỗi tải người dùng: " + r.getError());
        });
    }

    public void changeRole(@NonNull String userId, @NonNull String newRole) {
        userRepo.setRole(userId, newRole, r -> {
            postMessage(r.isSuccess() ? "Đã đổi quyền thành " + newRole : "Lỗi: " + r.getError());
            if (r.isSuccess()) load();
        });
    }

    public void topUp(@NonNull String userId, String userName, long amount) {
        walletRepo.topUp(userId, amount, new WalletHelper.Callback() {
            @Override public void onSuccess() {
                postMessage("✅ Đã nạp " + amount + " đ cho " + userName);
                load();
            }
            @Override public void onError(String message) { postMessage("Lỗi nạp tiền: " + message); }
        });
    }
}
