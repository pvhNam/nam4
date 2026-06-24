package com.example.doanmb.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** ViewModel khung Admin: chỉ giữ tên admin để header hiển thị. */
public class AdminDashboardViewModel extends BaseViewModel {

    private final UserRepository userRepo = UserRepository.getInstance();
    private final MutableLiveData<String> adminName = new MutableLiveData<>();

    public LiveData<String> getAdminName() { return adminName; }

    public void loadAdminName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        userRepo.getUserDoc(user.getUid(), r -> {
            if (r.isSuccess() && r.getData() != null) {
                String n = r.getData().getString(F.NAME);
                if (n != null) adminName.setValue(n);
            }
        });
    }
}
