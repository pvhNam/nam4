package com.example.doanmb.ui.driver;

import androidx.annotation.NonNull;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** ViewModel trang chủ tài xế (driver1): online, chuyến chờ, thống kê hôm nay. */
public class DriverHomeViewModel extends BaseViewModel {

    /** Thống kê hôm nay. */
    public static class TodayStats {
        public final double revenue;
        public final int trips;
        public TodayStats(double r, int t) { revenue = r; trips = t; }
    }

    private final UserRepository userRepo = UserRepository.getInstance();
    private final TripRepository tripRepo = TripRepository.getInstance();

    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<String> avatarUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> online = new MutableLiveData<>(true);
    private final MutableLiveData<List<Trip>> waiting = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<TodayStats> todayStats = new MutableLiveData<>();

    private String uid = "";
    private String driverName;
    private String driverCarType;

    public LiveData<String> getName()      { return name; }
    public LiveData<String> getAvatarUrl() { return avatarUrl; }
    public LiveData<Boolean> getOnline()   { return online; }
    public LiveData<List<Trip>> getWaiting() { return waiting; }
    public LiveData<TodayStats> getTodayStats() { return todayStats; }

    public boolean isOnline() { return Boolean.TRUE.equals(online.getValue()); }

    public void load() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : "";
        if (uid.isEmpty()) return;
        userRepo.getUserDoc(uid, r -> {
            if (!r.isSuccess() || r.getData() == null) return;
            DocumentSnapshot doc = r.getData();
            driverName = doc.getString(F.NAME);
            driverCarType = doc.getString("driverCarType");
            Boolean on = doc.getBoolean("driverOnline");
            online.setValue(on == null || on);
            name.setValue(driverName != null && !driverName.isEmpty() ? driverName : "Tài xế");
            String a = doc.getString(F.AVATAR_URL);
            if (a != null && !a.isEmpty()) avatarUrl.setValue(a);
            loadWaiting();
            loadTodayStats();
        });
    }

    public void setOnline(boolean value) {
        online.setValue(value);
        if (!uid.isEmpty()) userRepo.setDriverOnline(uid, value);
        loadWaiting();
    }

    public void loadWaiting() {
        if (!isOnline()) { waiting.setValue(new ArrayList<>()); return; }
        tripRepo.getWaitingTrips(r -> {
            if (!r.isSuccess() || r.getData() == null) return;
            List<Trip> filtered = new ArrayList<>();
            for (Trip t : r.getData()) {
                if (driverCarType == null || driverCarType.isEmpty()
                        || driverCarType.equals(t.getCarType())) filtered.add(t);
            }
            waiting.setValue(filtered);
        });
    }

    public void accept(@NonNull Trip trip) {
        if (trip.getId() == null) return;
        tripRepo.acceptTrip(trip.getId(), uid, driverName != null ? driverName : "", r -> {
            if (r.isSuccess()) postMessage("✅ Đã nhận chuyến! Xem ở tab Chuyến.");
            else postMessage("Không nhận được: " + r.getError());
            loadWaiting();
            loadTodayStats();
        });
    }

    private void loadTodayStats() {
        tripRepo.getTripsByDriver(uid, Trip.STATUS_COMPLETED, r -> {
            if (!r.isSuccess() || r.getData() == null) return;
            Date start = startOfToday();
            int count = 0;
            double revenue = 0;
            for (DocumentSnapshot d : r.getData()) {
                Timestamp done = d.getTimestamp("completedAt");
                if (done != null && done.toDate().after(start)) {
                    count++;
                    Double p = d.getDouble("price");
                    if (p != null) revenue += p;
                }
            }
            todayStats.setValue(new TodayStats(revenue, count));
        });
    }

    private Date startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
