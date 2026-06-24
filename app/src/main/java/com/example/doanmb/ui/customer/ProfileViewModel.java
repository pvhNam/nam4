package com.example.doanmb.ui.customer;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.AuthRepository;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.example.doanmb.util.CloudinaryHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/** ViewModel trang cá nhân khách: hồ sơ, ví, avatar, lưu thông tin, đăng xuất. */
public class ProfileViewModel extends BaseViewModel {

    /** Hồ sơ hiển thị. */
    public static class UserProfile {
        public String name, phone, dob, gender, avatarUrl, driverStatus = "";
        public boolean phoneVerified;
        public long balance;
    }

    private final UserRepository userRepo = UserRepository.getInstance();
    private final AuthRepository authRepo = AuthRepository.getInstance();

    private final MutableLiveData<Boolean> loggedIn = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<String> avatarUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> infoSaved = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loggedOut = new MutableLiveData<>();

    private String uid = "";

    public LiveData<Boolean> getLoggedIn()    { return loggedIn; }
    public LiveData<UserProfile> getProfile() { return profile; }
    public LiveData<String> getAvatarUrl()    { return avatarUrl; }
    public LiveData<Boolean> getInfoSaved()   { return infoSaved; }
    public LiveData<Boolean> getLoggedOut()   { return loggedOut; }

    public void refresh() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { loggedIn.setValue(false); return; }
        loggedIn.setValue(true);
        uid = user.getUid();
        load();
    }

    private void load() {
        userRepo.getUserDoc(uid, r -> {
            if (!r.isSuccess() || r.getData() == null || !r.getData().exists()) return;
            DocumentSnapshot doc = r.getData();
            UserProfile p = new UserProfile();
            p.name = doc.getString(F.NAME);
            p.phone = doc.getString(F.PHONE);
            p.dob = doc.getString("dob");
            p.gender = doc.getString("gender");
            p.avatarUrl = doc.getString(F.AVATAR_URL);
            Boolean v = doc.getBoolean("phoneVerified");
            p.phoneVerified = v != null && v;
            String st = doc.getString(F.DRIVER_STATUS);
            p.driverStatus = st != null ? st : "";
            Double b = doc.getDouble(F.BALANCE);
            p.balance = b != null ? Math.round(b) : 0L;
            profile.setValue(p);
            if (p.avatarUrl != null) avatarUrl.setValue(p.avatarUrl);
        });
    }

    public void uploadAvatar(Context ctx, Uri uri) {
        if (uid.isEmpty()) return;
        postMessage("Đang tải ảnh lên...");
        CloudinaryHelper.uploadImage(ctx, uri, new CloudinaryHelper.OnUploadCallback() {
            @Override public void onSuccess(String imageUrl) {
                Map<String, Object> m = new HashMap<>();
                m.put(F.AVATAR_URL, imageUrl);
                userRepo.updateUser(uid, m, r -> {
                    if (r.isSuccess()) { avatarUrl.setValue(imageUrl); postMessage("✅ Cập nhật ảnh đại diện thành công!"); }
                    else postMessage("Lỗi: " + r.getError());
                });
            }
            @Override public void onFailure(String error) { postMessage("Lỗi: " + error); }
        });
    }

    public void saveInfo(String name, String dob, String gender, String phone) {
        if (uid.isEmpty()) return;
        if (name.isEmpty()) { postMessage("Họ và tên không được để trống"); return; }
        Map<String, Object> data = new HashMap<>();
        data.put(F.NAME, name);
        data.put("dob", dob);
        data.put("gender", gender);
        data.put(F.PHONE, phone);
        postMessage("Đang lưu thay đổi...");
        userRepo.updateUser(uid, data, r -> {
            if (r.isSuccess()) { postMessage("✅ Lưu thông tin thành công!"); infoSaved.setValue(true); load(); }
            else postMessage("Lỗi: " + r.getError());
        });
    }

    public void logout() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> m = new HashMap<>();
            m.put("fcmToken", "");
            userRepo.updateUser(user.getUid(), m, r -> { authRepo.signOut(); loggedOut.setValue(true); });
        } else {
            authRepo.signOut();
            loggedOut.setValue(true);
        }
    }
}
