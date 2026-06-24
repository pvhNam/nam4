package com.example.doanmb.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.doanmb.data.FirebaseContract.Col;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.Result;
import com.example.doanmb.model.Car;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Truy cập tin xe / bài tài xế (collection "cars").
 */
public class CarRepository extends BaseRepository {

    private static CarRepository instance;
    public static synchronized CarRepository getInstance() {
        if (instance == null) instance = new CarRepository();
        return instance;
    }
    private CarRepository() {}

    /** Lấy tất cả xe (đọc id vào model.id). */
    public void getAllCars(@NonNull Result.Callback<List<Car>> cb) {
        db.collection(Col.CARS).get()
                .addOnSuccessListener(snap -> cb.onResult(Result.ok(mapCars(snap))))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Lấy xe theo loại (sale/rental/driver...). */
    public void getCarsByType(@NonNull String type, @NonNull Result.Callback<List<Car>> cb) {
        db.collection(Col.CARS).whereEqualTo(F.TYPE, type).get()
                .addOnSuccessListener(snap -> cb.onResult(Result.ok(mapCars(snap))))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Lấy xe theo trạng thái (vd "pending" để Admin duyệt). */
    public void getCarsByStatus(@Nullable String status, @NonNull Result.Callback<List<Car>> cb) {
        Query q = db.collection(Col.CARS);
        if (status != null) q = q.whereEqualTo(F.STATUS, status);
        q.get()
                .addOnSuccessListener(snap -> cb.onResult(Result.ok(mapCars(snap))))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Lấy các xe do một người đăng. */
    public void getCarsBySeller(@NonNull String sellerId, @NonNull Result.Callback<List<Car>> cb) {
        db.collection(Col.CARS).whereEqualTo(F.SELLER_ID, sellerId).get()
                .addOnSuccessListener(snap -> cb.onResult(Result.ok(mapCars(snap))))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Lấy tất cả xe ở dạng document thô (để màn Admin lọc theo status client-side). */
    public void getAllCarDocs(@NonNull Result.Callback<List<DocumentSnapshot>> cb) {
        db.collection(Col.CARS).get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) out.add(d);
                    cb.onResult(Result.ok(out));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Lấy các bài đăng theo field "userId" (người đăng) ở dạng document thô. */
    public void getCarDocsByUser(@NonNull String userId, @NonNull Result.Callback<List<DocumentSnapshot>> cb) {
        db.collection(Col.CARS).whereEqualTo("userId", userId).get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) out.add(d);
                    cb.onResult(Result.ok(out));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    public void getCarDoc(@NonNull String carId, @NonNull Result.Callback<DocumentSnapshot> cb) {
        db.collection(Col.CARS).document(carId).get()
                .addOnSuccessListener(d -> cb.onResult(Result.ok(d)))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Thêm 1 bài đăng xe / tài xế vào collection cars. */
    public void addCar(@NonNull Map<String, Object> data, @NonNull Result.Callback<String> cb) {
        db.collection(Col.CARS).add(data)
                .addOnSuccessListener(ref -> cb.onResult(Result.ok(ref.getId())))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    public void updateCar(@NonNull String carId, @NonNull Map<String, Object> fields,
                          @NonNull Result.Callback<Void> cb) {
        db.collection(Col.CARS).document(carId).update(fields)
                .addOnSuccessListener(v -> cb.onResult(Result.ok(null)))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    public void setCarStatus(@NonNull String carId, @NonNull String status,
                             @NonNull Result.Callback<Void> cb) {
        Map<String, Object> m = new HashMap<>();
        m.put(F.STATUS, status);
        updateCar(carId, m, cb);
    }

    public void deleteCar(@NonNull String carId, @NonNull Result.Callback<Void> cb) {
        db.collection(Col.CARS).document(carId).delete()
                .addOnSuccessListener(v -> cb.onResult(Result.ok(null)))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /**
     * Map thủ công 1 document sang Car (mirror đúng cách dữ liệu đang lưu: price/info
     * là String). KHÔNG dùng toObject vì Car chưa có constructor rỗng và price có thể
     * là String.
     */
    public static Car fromDoc(@NonNull DocumentSnapshot d) {
        String name = d.getString(F.NAME);
        String price = d.getString(F.PRICE);
        String info = d.getString(F.INFO);
        Car c = new Car(name != null ? name : "",
                price != null ? price : "",
                info != null ? info : "",
                android.R.drawable.ic_menu_gallery);
        c.setId(d.getId());
        String type = d.getString(F.TYPE);
        String imageUrl = d.getString(F.IMAGE_URL);
        String sellerId = d.getString(F.SELLER_ID);
        c.setType(type != null ? type : "");
        c.setImageUrl(imageUrl != null ? imageUrl : "");
        c.setSellerId(sellerId != null ? sellerId : "");
        return c;
    }

    private List<Car> mapCars(Iterable<QueryDocumentSnapshot> snap) {
        List<Car> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : snap) out.add(fromDoc(d));
        return out;
    }
}
