package com.example.doanmb.ui.admin;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.DriverStatus;
import com.example.doanmb.data.repository.NotificationRepository;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.model.User;
import com.example.doanmb.ui.base.BaseViewModel;

/** ViewModel cho màn chi tiết & duyệt 1 hồ sơ tài xế. */
public class AdminDriverDetailViewModel extends BaseViewModel {

    private final UserRepository userRepo = UserRepository.getInstance();
    private final NotificationRepository notifRepo = NotificationRepository.getInstance();

    private final MutableLiveData<User> user = new MutableLiveData<>();
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>();

    public LiveData<User> getUser()       { return user; }
    /** true khi đã duyệt/từ chối xong → màn nên đóng lại. */
    public LiveData<Boolean> getFinished() { return finished; }

    public void load(@NonNull String uid) {
        if (uid.isEmpty()) return;
        userRepo.getUser(uid, r -> {
            if (r.isSuccess()) user.setValue(r.getData());
            else postMessage("Lỗi tải hồ sơ: " + r.getError());
        });
    }

    public void approve(@NonNull String uid) {
        decide(uid, DriverStatus.APPROVED, true,
                "Đăng ký tài xế được duyệt ✅",
                "Hồ sơ của bạn đã được Admin duyệt. Đăng xuất và đăng nhập lại để vào giao diện tài xế.",
                "Đã duyệt tài xế!");
    }

    public void reject(@NonNull String uid) {
        decide(uid, DriverStatus.REJECTED, false,
                "Đăng ký tài xế bị từ chối ❌",
                "Hồ sơ của bạn chưa đáp ứng yêu cầu. Vui lòng cập nhật và gửi lại.",
                "Đã từ chối hồ sơ.");
    }

    private void decide(String uid, String status, boolean isDriver,
                        String notifTitle, String notifBody, String toast) {
        if (uid.isEmpty()) return;
        userRepo.setDriverDecision(uid, status, isDriver, r -> {
            if (r.isSuccess()) {
                notifRepo.notify(uid, notifTitle, notifBody);
                postMessage(toast);
                finished.setValue(true);
            } else {
                postMessage("Lỗi: " + r.getError());
                finished.setValue(false);
            }
        });
    }
}
