package com.example.doanmb.ui.customer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.CarStatus;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.FirebaseContract.OrderStatus;
import com.example.doanmb.data.repository.CarRepository;
import com.example.doanmb.data.repository.OrderRepository;
import com.example.doanmb.model.Car;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** ViewModel tab Quản lý: xe đã đăng + yêu cầu nhận được (realtime). */
public class ManageViewModel extends BaseViewModel {

    private final CarRepository carRepo = CarRepository.getInstance();
    private final OrderRepository orderRepo = OrderRepository.getInstance();

    private final MutableLiveData<List<Car>> myPosts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<DocumentSnapshot>> requests = new MutableLiveData<>(new ArrayList<>());

    private String uid = "";
    private ListenerRegistration requestsListener;

    public LiveData<List<Car>> getMyPosts()                 { return myPosts; }
    public LiveData<List<DocumentSnapshot>> getRequests()   { return requests; }

    public void start() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : "";
        if (uid.isEmpty()) return;
        loadMyPosts();
        loadRequests();
    }

    public void loadMyPosts() {
        if (uid.isEmpty()) return;
        carRepo.getCarDocsByUser(uid, r -> {
            if (!r.isSuccess() || r.getData() == null) return;
            List<Car> cars = new ArrayList<>();
            for (DocumentSnapshot doc : r.getData()) {
                Car c = buildCar(doc);
                if (c != null) cars.add(c);
            }
            myPosts.setValue(cars);
        });
    }

    public void loadRequests() {
        if (uid.isEmpty()) return;
        if (requestsListener != null) requestsListener.remove();
        requestsListener = orderRepo.listenBySeller(uid, r -> {
            if (!r.isSuccess() || r.getData() == null) { loadRequestsByCarId(); return; }
            if (r.getData().isEmpty()) { loadRequestsByCarId(); return; }
            requests.setValue(r.getData());
        });
    }

    /** Fallback: đơn cũ chưa có sellerId → lọc theo carId thuộc về mình. */
    private void loadRequestsByCarId() {
        carRepo.getCarDocsByUser(uid, cr -> {
            if (!cr.isSuccess() || cr.getData() == null) { requests.setValue(new ArrayList<>()); return; }
            List<String> myCarIds = new ArrayList<>();
            for (DocumentSnapshot d : cr.getData()) myCarIds.add(d.getId());
            if (myCarIds.isEmpty()) { requests.setValue(new ArrayList<>()); return; }
            orderRepo.getAllOrders(or -> {
                List<DocumentSnapshot> out = new ArrayList<>();
                if (or.isSuccess() && or.getData() != null) {
                    for (DocumentSnapshot d : or.getData()) {
                        String carId = d.getString(F.CAR_ID);
                        if (carId != null && myCarIds.contains(carId)) out.add(d);
                    }
                }
                requests.setValue(out);
            });
        });
    }

    public void confirmRequest(String orderId, String carId) {
        orderRepo.updateStatus(orderId, OrderStatus.CONFIRMED, null, r -> {});
        if (carId != null && !carId.isEmpty()) {
            carRepo.setCarStatus(carId, CarStatus.SOLD, r -> {
                postMessage("✅ Đã xác nhận! Xe sẽ được ẩn khỏi danh sách.");
                loadMyPosts();
                loadRequests();
            });
        } else {
            postMessage("✅ Đã xác nhận yêu cầu!");
            loadRequests();
        }
    }

    public void rejectRequest(String orderId, String carId) {
        orderRepo.updateStatus(orderId, "rejected", null, r -> {});
        if (carId != null && !carId.isEmpty()) {
            carRepo.setCarStatus(carId, CarStatus.ACTIVE, r -> {
                postMessage("Đã từ chối yêu cầu. Xe tiếp tục hiển thị.");
                loadMyPosts();
                loadRequests();
            });
        } else {
            postMessage("Đã từ chối yêu cầu.");
            loadRequests();
        }
    }

    private Car buildCar(DocumentSnapshot doc) {
        String name = doc.getString(F.NAME);
        if (name == null) return null;
        String price = doc.getString(F.PRICE);
        String info = doc.getString(F.INFO);
        String status = doc.getString(F.STATUS);
        Car car;
        if ("holding".equals(status)) {
            car = new Car("⏳ " + name, price != null ? price : "",
                    "Đang có người đặt cọc • " + (info != null ? info : ""),
                    android.R.drawable.ic_menu_gallery);
        } else {
            car = new Car(name, price != null ? price : "", info != null ? info : "",
                    android.R.drawable.ic_menu_gallery);
        }
        car.setId(doc.getId());
        String type = doc.getString(F.TYPE);
        String imageUrl = doc.getString(F.IMAGE_URL);
        String sellerId = doc.getString(F.SELLER_ID);
        if (sellerId == null) sellerId = doc.getString("userId");
        car.setType(type != null ? type : "");
        car.setImageUrl(imageUrl != null ? imageUrl : "");
        car.setSellerId(sellerId != null ? sellerId : "");
        return car;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (requestsListener != null) { requestsListener.remove(); requestsListener = null; }
    }
}
