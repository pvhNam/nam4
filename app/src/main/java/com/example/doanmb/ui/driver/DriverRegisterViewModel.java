package com.example.doanmb.ui.driver;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.DriverStatus;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.example.doanmb.util.CloudinaryHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/** ViewModel màn đăng ký làm tài xế (CCCD + bằng lái + loại xe). */
public class DriverRegisterViewModel extends BaseViewModel {

    /** Dữ liệu hồ sơ đã có (để điền lại form). */
    public static class Existing {
        public String cccd, license, cccdImageUrl, licenseImageUrl, status;
    }

    private final UserRepository userRepo = UserRepository.getInstance();

    private final MutableLiveData<Existing> existing = new MutableLiveData<>();
    private final MutableLiveData<String> cccdImageUrl = new MutableLiveData<>();
    private final MutableLiveData<String> licenseImageUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>();

    private String uid = "";

    public LiveData<Existing> getExisting()        { return existing; }
    public LiveData<String> getCccdImageUrl()      { return cccdImageUrl; }
    public LiveData<String> getLicenseImageUrl()   { return licenseImageUrl; }
    public LiveData<Boolean> getSubmitting()       { return submitting; }
    public LiveData<Boolean> getFinished()         { return finished; }

    public void load() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : "";
        if (uid.isEmpty()) return;
        userRepo.getUserDoc(uid, r -> {
            if (!r.isSuccess() || r.getData() == null || !r.getData().exists()) return;
            DocumentSnapshot doc = r.getData();
            Existing e = new Existing();
            e.cccd = doc.getString("cccd");
            e.license = doc.getString("licenseNumber");
            e.cccdImageUrl = doc.getString("cccdImageUrl");
            e.licenseImageUrl = doc.getString("licenseImageUrl");
            e.status = doc.getString(F.DRIVER_STATUS);
            cccdImageUrl.setValue(e.cccdImageUrl);
            licenseImageUrl.setValue(e.licenseImageUrl);
            existing.setValue(e);
        });
    }

    /** which: 1 = CCCD, 2 = bằng lái. */
    public void uploadImage(@NonNull Context ctx, @NonNull Uri uri, int which) {
        postMessage("Đang tải ảnh lên...");
        CloudinaryHelper.uploadImage(ctx, uri, new CloudinaryHelper.OnUploadCallback() {
            @Override public void onSuccess(String url) {
                if (which == 1) cccdImageUrl.setValue(url); else licenseImageUrl.setValue(url);
            }
            @Override public void onFailure(String error) { postMessage("Lỗi tải ảnh: " + error); }
        });
    }

    public void submit(String cccd, String license, String carType) {
        if (cccd.isEmpty() || license.isEmpty()) {
            postMessage("Vui lòng nhập đầy đủ CCCD và số bằng lái");
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("cccd", cccd);
        data.put("licenseNumber", license);
        data.put("driverCarType", carType);
        data.put(F.DRIVER_STATUS, DriverStatus.PENDING);
        data.put("appliedAt", Timestamp.now());
        if (cccdImageUrl.getValue() != null) data.put("cccdImageUrl", cccdImageUrl.getValue());
        if (licenseImageUrl.getValue() != null) data.put("licenseImageUrl", licenseImageUrl.getValue());

        submitting.setValue(true);
        userRepo.updateUser(uid, data, r -> {
            submitting.setValue(false);
            if (r.isSuccess()) { postMessage("✅ Đã gửi đăng ký. Chờ Admin duyệt!"); finished.setValue(true); }
            else postMessage("Lỗi: " + r.getError());
        });
    }
}
