package com.example.doanmb.ui.driver;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** ViewModel tab "Bản đồ" tài xế: hồ sơ + trạng thái online. */
public class DriverMapViewModel extends BaseViewModel {

    private final UserRepository userRepo = UserRepository.getInstance();
    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<String> avatarUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> online = new MutableLiveData<>(true);

    private String uid = "";

    public LiveData<String> getName()      { return name; }
    public LiveData<String> getAvatarUrl() { return avatarUrl; }
    public LiveData<Boolean> getOnline()   { return online; }
    public boolean isOnline() { return Boolean.TRUE.equals(online.getValue()); }

    public void load() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : "";
        if (uid.isEmpty()) return;
        userRepo.getUserDoc(uid, r -> {
            if (!r.isSuccess() || r.getData() == null) return;
            String n = r.getData().getString(F.NAME);
            name.setValue(n != null ? n : "Tài xế");
            String a = r.getData().getString(F.AVATAR_URL);
            if (a != null && !a.isEmpty()) avatarUrl.setValue(a);
            Boolean on = r.getData().getBoolean("driverOnline");
            online.setValue(on == null || on);
        });
    }

    public void setOnline(boolean value) {
        online.setValue(value);
        if (!uid.isEmpty()) userRepo.setDriverOnline(uid, value);
    }
}
