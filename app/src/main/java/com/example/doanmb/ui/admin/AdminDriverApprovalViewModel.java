package com.example.doanmb.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.DriverStatus;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.model.User;
import com.example.doanmb.ui.base.BaseViewModel;

import java.util.List;

/** ViewModel cho màn Admin duyệt hồ sơ tài xế (danh sách hồ sơ đang chờ). */
public class AdminDriverApprovalViewModel extends BaseViewModel {

    private final UserRepository userRepo = UserRepository.getInstance();
    private final MutableLiveData<List<User>> pending = new MutableLiveData<>();

    public LiveData<List<User>> getPending() { return pending; }

    public void loadPending() {
        setLoading(true);
        userRepo.getUsersByDriverStatus(DriverStatus.PENDING, r -> {
            setLoading(false);
            if (r.isSuccess()) pending.setValue(r.getData());
            else postMessage("Lỗi tải dữ liệu: " + r.getError());
        });
    }
}
