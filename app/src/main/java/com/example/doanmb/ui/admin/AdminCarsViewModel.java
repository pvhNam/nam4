package com.example.doanmb.ui.admin;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.CarStatus;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.CarRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel cho màn Admin duyệt xe. Lọc client-side để bắt được cả xe cũ chưa có
 * field "status" (null = coi như chờ duyệt).
 */
public class AdminCarsViewModel extends BaseViewModel {

    private final CarRepository carRepo = CarRepository.getInstance();
    private final MutableLiveData<List<DocumentSnapshot>> cars = new MutableLiveData<>();
    private boolean showingPending = true;

    public LiveData<List<DocumentSnapshot>> getCars() { return cars; }
    public boolean isShowingPending() { return showingPending; }

    public void selectTab(boolean pending) {
        showingPending = pending;
        load();
    }

    public void load() {
        setLoading(true);
        carRepo.getAllCarDocs(r -> {
            setLoading(false);
            if (!r.isSuccess() || r.getData() == null) {
                postMessage("Lỗi tải xe: " + r.getError());
                return;
            }
            List<DocumentSnapshot> out = new ArrayList<>();
            for (DocumentSnapshot d : r.getData()) {
                String status = d.getString(F.STATUS);
                boolean isPending = status == null || CarStatus.PENDING.equals(status);
                if (!showingPending || isPending) out.add(d);
            }
            cars.setValue(out);
        });
    }

    public void approve(@NonNull String carId) {
        carRepo.setCarStatus(carId, CarStatus.ACTIVE, r -> {
            postMessage(r.isSuccess() ? "✅ Đã duyệt xe" : "Lỗi: " + r.getError());
            if (r.isSuccess()) load();
        });
    }

    public void reject(@NonNull String carId) {
        carRepo.setCarStatus(carId, CarStatus.REJECTED, r -> {
            postMessage(r.isSuccess() ? "❌ Đã từ chối xe" : "Lỗi: " + r.getError());
            if (r.isSuccess()) load();
        });
    }

    public void delete(@NonNull String carId) {
        carRepo.deleteCar(carId, r -> {
            postMessage(r.isSuccess() ? "🗑️ Đã xóa xe khỏi hệ thống" : "Lỗi: " + r.getError());
            if (r.isSuccess()) load();
        });
    }
}
