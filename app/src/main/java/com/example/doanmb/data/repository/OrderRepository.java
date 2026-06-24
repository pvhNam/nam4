package com.example.doanmb.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.doanmb.data.FirebaseContract.Col;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.Result;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Truy cập đơn hàng (collection "orders"). Gom các thao tác mà trước đây nằm rải
 * rác trong AdminOrdersFragment / ManageFragment / CarDetailActivity.
 */
public class OrderRepository extends BaseRepository {

    private static OrderRepository instance;
    public static synchronized OrderRepository getInstance() {
        if (instance == null) instance = new OrderRepository();
        return instance;
    }
    private OrderRepository() {}

    /** Lấy đơn theo trạng thái; status == null nghĩa là lấy tất cả. */
    public void getOrdersByStatus(@Nullable String status,
                                  @NonNull Result.Callback<List<DocumentSnapshot>> cb) {
        Query q = db.collection(Col.ORDERS);
        if (status != null) q = q.whereEqualTo(F.STATUS, status);
        q.get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) out.add(d);
                    cb.onResult(Result.ok(out));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Lắng nghe realtime các đơn theo sellerId. Trả ListenerRegistration để gỡ. */
    public ListenerRegistration listenBySeller(@NonNull String sellerId,
                                               @NonNull Result.Callback<List<DocumentSnapshot>> cb) {
        return db.collection(Col.ORDERS).whereEqualTo(F.SELLER_ID, sellerId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) { cb.onResult(Result.fail(err.getMessage())); return; }
                    List<DocumentSnapshot> out = new ArrayList<>();
                    if (snap != null) out.addAll(snap.getDocuments());
                    cb.onResult(Result.ok(out));
                });
    }

    /** Lấy tất cả đơn (để lọc theo carId khi đơn cũ chưa có sellerId). */
    public void getAllOrders(@NonNull Result.Callback<List<DocumentSnapshot>> cb) {
        db.collection(Col.ORDERS).get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) out.add(d);
                    cb.onResult(Result.ok(out));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Lấy 1 đơn theo id. */
    public void getOrder(@NonNull String orderId, @NonNull Result.Callback<DocumentSnapshot> cb) {
        db.collection(Col.ORDERS).document(orderId).get()
                .addOnSuccessListener(d -> cb.onResult(Result.ok(d)))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Đổi trạng thái đơn (kèm tuỳ chọn cập nhật depositStatus). */
    public void updateStatus(@NonNull String orderId, @NonNull String status,
                             @Nullable String depositStatus, @NonNull Result.Callback<Void> cb) {
        Map<String, Object> update = new HashMap<>();
        update.put(F.STATUS, status);
        if (depositStatus != null) update.put(F.DEPOSIT_STATUS, depositStatus);
        db.collection(Col.ORDERS).document(orderId).update(update)
                .addOnSuccessListener(v -> cb.onResult(Result.ok(null)))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Cập nhật trạng thái xe (dùng khi huỷ đơn trả xe về "active"). */
    public void updateCarStatus(@NonNull String carId, @NonNull String carStatus) {
        Map<String, Object> m = new HashMap<>();
        m.put(F.STATUS, carStatus);
        db.collection(Col.CARS).document(carId).update(m);
    }
}
