package com.example.doanmb.ui.driver;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** ViewModel trang cá nhân tài xế: tên + avatar. */
public class DriverProfileViewModel extends BaseViewModel {

    private final UserRepository userRepo = UserRepository.getInstance();
    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<String> avatarUrl = new MutableLiveData<>();

    public LiveData<String> getName()      { return name; }
    public LiveData<String> getAvatarUrl() { return avatarUrl; }

    public void load() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        userRepo.getUserDoc(user.getUid(), r -> {
            if (!r.isSuccess() || r.getData() == null || !r.getData().exists()) return;
            String n = r.getData().getString(F.NAME);
            name.setValue(n != null && !n.isEmpty() ? n : "Tài xế");
            String a = r.getData().getString(F.AVATAR_URL);
            if (a != null && !a.isEmpty()) avatarUrl.setValue(a);
        });
    }
}
