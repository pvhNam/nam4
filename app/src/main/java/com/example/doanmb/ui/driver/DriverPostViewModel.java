package com.example.doanmb.ui.driver;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.CarStatus;
import com.example.doanmb.data.FirebaseContract.CarType;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.CarRepository;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.example.doanmb.util.CloudinaryHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** ViewModel màn tài xế đăng bài cho thuê (lưu vào collection cars). */
public class DriverPostViewModel extends BaseViewModel {

    /** Dữ liệu form nhập từ màn. */
    public static class PostInput {
        public String title, price, priceKm, carType, location, desc;
        public boolean withCar;
        public List<Uri> images = new ArrayList<>();
    }

    private final UserRepository userRepo = UserRepository.getInstance();
    private final CarRepository carRepo = CarRepository.getInstance();

    private final MutableLiveData<String> prefillCarType = new MutableLiveData<>();
    private final MutableLiveData<Boolean> posting = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> success = new MutableLiveData<>();

    private String uid = "";

    public LiveData<String> getPrefillCarType() { return prefillCarType; }
    public LiveData<Boolean> getPosting()        { return posting; }
    public LiveData<Boolean> getSuccess()        { return success; }

    public void loadPrefill() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : "";
        if (uid.isEmpty()) return;
        userRepo.getUserDoc(uid, r -> {
            if (!r.isSuccess() || r.getData() == null) return;
            String carType = r.getData().getString("driverCarType");
            if (carType != null && !carType.isEmpty()) prefillCarType.setValue(carType);
        });
    }

    public void submit(@NonNull Context appContext, @NonNull PostInput in) {
        if (in.title.isEmpty() || in.price.isEmpty()) { postMessage("Vui lòng nhập tiêu đề và giá!"); return; }
        if (uid.isEmpty()) { postMessage("Vui lòng đăng nhập trước!"); return; }

        posting.setValue(true);
        if (!in.images.isEmpty()) {
            CloudinaryHelper.uploadImages(appContext, new ArrayList<>(in.images),
                    new CloudinaryHelper.OnMultiUploadCallback() {
                        @Override public void onSuccess(List<String> imageUrls) { savePost(in, imageUrls); }
                        @Override public void onFailure(String error) {
                            postMessage("Lỗi upload ảnh: " + error);
                            posting.setValue(false);
                        }
                    });
        } else {
            savePost(in, new ArrayList<>());
        }
    }

    private void savePost(PostInput in, List<String> imageUrls) {
        long pricePerDay = parseMoney(in.price);
        long pricePerKm  = parseMoney(in.priceKm);
        String priceDisplay = formatMoney(pricePerDay) + " đ/ngày"
                + (pricePerKm > 0 ? "  ·  " + formatMoney(pricePerKm) + " đ/km" : "");
        String info = (in.withCar ? "Xe có tài xế" : "Lái thuê")
                + (in.carType.isEmpty() ? "" : " • " + in.carType)
                + (in.location.isEmpty() ? "" : " • " + in.location)
                + (pricePerKm > 0 ? "\nNhận đặt theo chuyến: " + formatMoney(pricePerKm) + " đ/km" : "")
                + (in.desc.isEmpty() ? "" : "\n" + in.desc);

        userRepo.getUserDoc(uid, r -> {
            if (!r.isSuccess() || r.getData() == null) {
                postMessage("Lỗi lấy thông tin tài xế!");
                posting.setValue(false);
                return;
            }
            String sellerName = r.getData().getString(F.NAME);
            String sellerPhone = r.getData().getString(F.PHONE);
            String coverUrl = imageUrls.isEmpty() ? "" : imageUrls.get(0);

            Map<String, Object> post = new HashMap<>();
            post.put(F.NAME, in.title);
            post.put(F.PRICE, priceDisplay);
            post.put(F.PRICE_PER_DAY, pricePerDay);
            post.put(F.PRICE_PER_KM, pricePerKm);
            post.put(F.INFO, info);
            post.put(F.TYPE, in.withCar ? CarType.DRIVER : CarType.DRIVER_ONLY);
            post.put(F.BRAND, "");
            post.put("location", in.location);
            post.put(F.STATUS, CarStatus.PENDING);
            post.put(F.IMAGE_URL, coverUrl);
            post.put("imageUrls", imageUrls);
            post.put("userId", uid);
            post.put(F.SELLER_ID, uid);
            post.put("sellerName", sellerName != null ? sellerName : "");
            post.put("sellerPhone", sellerPhone != null ? sellerPhone : "");
            post.put(F.CREATED_AT, Timestamp.now());

            carRepo.addCar(post, res -> {
                posting.setValue(false);
                if (res.isSuccess()) { postMessage("✅ Đăng bài thành công!"); success.setValue(true); }
                else postMessage("Lỗi: " + res.getError());
            });
        });
    }

    private static long parseMoney(String s) {
        if (s == null) return 0;
        String d = s.replaceAll("[^0-9]", "");
        if (d.isEmpty()) return 0;
        try { return Long.parseLong(d); } catch (NumberFormatException e) { return 0; }
    }

    private static String formatMoney(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount);
    }
}
