package com.example.doanmb.ui.driver;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.TripRepository;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.model.Trip;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/** ViewModel tab "Chuyến" tài xế: hồ sơ + 3 nhóm chuyến (chờ/đang chạy/lịch sử). */
public class DriverTripsViewModel extends BaseViewModel {

    private final UserRepository userRepo = UserRepository.getInstance();
    private final TripRepository tripRepo = TripRepository.getInstance();

    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<String> avatarUrl = new MutableLiveData<>();
    private final MutableLiveData<List<Trip>> waiting = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Trip>> running = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Trip>> history = new MutableLiveData<>(new ArrayList<>());

    private String uid = "";
    private String driverName;
    private String driverCarType;

    public LiveData<String> getName()      { return name; }
    public LiveData<String> getAvatarUrl() { return avatarUrl; }
    public LiveData<List<Trip>> getWaiting() { return waiting; }
    public LiveData<List<Trip>> getRunning() { return running; }
    public LiveData<List<Trip>> getHistory() { return history; }

    public void load() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : "";
        if (uid.isEmpty()) return;
        userRepo.getUserDoc(uid, r -> {
            if (!r.isSuccess() || r.getData() == null) return;
            driverName = r.getData().getString(F.NAME);
            driverCarType = r.getData().getString("driverCarType");
            name.setValue(driverName != null ? driverName : "Tài xế");
            String a = r.getData().getString(F.AVATAR_URL);
            if (a != null && !a.isEmpty()) avatarUrl.setValue(a);
            loadTrips();
        });
    }

    public void loadTrips() {
        if (uid.isEmpty()) return;
        tripRepo.getWaitingTrips(r -> {
            if (!r.isSuccess() || r.getData() == null) return;
            List<Trip> filtered = new ArrayList<>();
            for (Trip t : r.getData()) {
                if (driverCarType == null || driverCarType.isEmpty()
                        || driverCarType.equals(t.getCarType())) filtered.add(t);
            }
            waiting.setValue(filtered);
        });
        tripRepo.getMyTrips(uid, r -> {
            if (!r.isSuccess() || r.getData() == null) return;
            List<Trip> run = new ArrayList<>();
            List<Trip> his = new ArrayList<>();
            for (Trip t : r.getData()) {
                if (Trip.STATUS_RUNNING.equals(t.getStatus())) run.add(t);
                else if (Trip.STATUS_COMPLETED.equals(t.getStatus())
                        || Trip.STATUS_CANCELLED.equals(t.getStatus())) his.add(t);
            }
            running.setValue(run);
            history.setValue(his);
        });
    }

    public void accept(@NonNull Trip trip) {
        if (trip.getId() == null) return;
        tripRepo.acceptTrip(trip.getId(), uid, driverName != null ? driverName : "", r -> {
            if (r.isSuccess()) postMessage("✅ Đã nhận chuyến!");
            else postMessage("Không nhận được: " + r.getError());
            loadTrips();
        });
    }

    public void complete(@NonNull Trip trip) {
        if (trip.getId() == null) return;
        tripRepo.completeTrip(trip.getId(), r -> {
            postMessage(r.isSuccess() ? "✅ Đã hoàn thành chuyến!" : "Lỗi: " + r.getError());
            loadTrips();
        });
    }

    /** Bỏ qua 1 chuyến chờ (chỉ ẩn ở phía client). */
    public void skip(@NonNull Trip trip) {
        List<Trip> cur = waiting.getValue() != null ? new ArrayList<>(waiting.getValue()) : new ArrayList<>();
        cur.remove(trip);
        waiting.setValue(cur);
    }
}
