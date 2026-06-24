package com.example.doanmb.ui.customer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.CarStatus;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.repository.CarRepository;
import com.example.doanmb.model.Car;
import com.example.doanmb.ui.base.BaseViewModel;
import com.example.doanmb.util.FavoriteHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/** ViewModel màn xe yêu thích: tải xe theo danh sách id, tự dọn xe bán/ẩn/xóa. */
public class FavoriteViewModel extends BaseViewModel {

    private final CarRepository carRepo = CarRepository.getInstance();
    private final MutableLiveData<List<Car>> favCars = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Car>> getFavCars() { return favCars; }

    public void load() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { favCars.setValue(new ArrayList<>()); return; }
        String uid = user.getUid();
        FavoriteHelper.loadIds(uid, ids -> {
            if (ids.isEmpty()) { favCars.setValue(new ArrayList<>()); return; }
            List<Car> result = new ArrayList<>();
            final int[] remaining = { ids.size() };
            for (String id : ids) {
                carRepo.getCarDoc(id, r -> {
                    DocumentSnapshot doc = r.isSuccess() ? r.getData() : null;
                    if (doc != null && doc.exists()) {
                        String status = doc.getString(F.STATUS);
                        if (CarStatus.SOLD.equals(status) || "hidden".equals(status)) {
                            FavoriteHelper.remove(uid, id);
                        } else {
                            result.add(buildCar(doc));
                        }
                    } else {
                        FavoriteHelper.remove(uid, id);
                    }
                    if (--remaining[0] == 0) favCars.setValue(result);
                });
            }
        });
    }

    private Car buildCar(DocumentSnapshot doc) {
        Car car = CarRepository.fromDoc(doc);
        if (car.getSellerId() == null || car.getSellerId().isEmpty()) {
            String userId = doc.getString("userId");
            if (userId != null) car.setSellerId(userId);
        }
        return car;
    }
}
