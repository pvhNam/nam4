package com.example.doanmb.ui.driver;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.TripRepository;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.model.Trip;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel tab Thu nhập tài xế: hồ sơ (tên/avatar/số dư) và danh sách chuyến đã
 * hoàn thành dạng [completedAtMillis, price] để màn vẽ biểu đồ & tính kỳ.
 */
public class DriverEarningsViewModel extends BaseViewModel {

    private final UserRepository userRepo = UserRepository.getInstance();
    private final TripRepository tripRepo = TripRepository.getInstance();

    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<String> avatarUrl = new MutableLiveData<>();
    private final MutableLiveData<Long> balance = new MutableLiveData<>();
    private final MutableLiveData<List<double[]>> completed = new MutableLiveData<>();

    public LiveData<String> getName()      { return name; }
    public LiveData<String> getAvatarUrl() { return avatarUrl; }
    public LiveData<Long> getBalance()     { return balance; }
    public LiveData<List<double[]>> getCompleted() { return completed; }

    public void load() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "";
        if (uid.isEmpty()) return;

        userRepo.getUserDoc(uid, r -> {
            if (!r.isSuccess() || r.getData() == null) return;
            DocumentSnapshot doc = r.getData();
            String n = doc.getString(F.NAME);
            name.setValue(n != null ? n : "Tài xế");
            String a = doc.getString(F.AVATAR_URL);
            if (a != null && !a.isEmpty()) avatarUrl.setValue(a);
            Double b = doc.getDouble(F.BALANCE);
            balance.setValue(b != null ? Math.round(b) : 0L);
        });

        tripRepo.getTripsByDriver(uid, Trip.STATUS_COMPLETED, r -> {
            if (!r.isSuccess() || r.getData() == null) return;
            List<double[]> out = new ArrayList<>();
            for (DocumentSnapshot d : r.getData()) {
                Timestamp done = d.getTimestamp("completedAt");
                if (done == null) continue;
                Double p = d.getDouble("price");
                out.add(new double[]{done.toDate().getTime(), p != null ? p : 0});
            }
            completed.setValue(out);
        });
    }
}
