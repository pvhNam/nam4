package com.example.doanmb.data.repository;

import androidx.annotation.NonNull;

import com.example.doanmb.data.FirebaseContract.Col;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.Result;
import com.example.doanmb.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Truy cập dữ liệu người dùng (collection "users").
 * Singleton để dùng lại 1 instance trong toàn app.
 */
public class UserRepository extends BaseRepository {

    private static UserRepository instance;
    public static synchronized UserRepository getInstance() {
        if (instance == null) instance = new UserRepository();
        return instance;
    }
    private UserRepository() {}

    /** Lấy hồ sơ user theo uid (trả về model User). */
    public void getUser(@NonNull String uid, @NonNull Result.Callback<User> cb) {
        db.collection(Col.USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { cb.onResult(Result.fail("Không tìm thấy người dùng")); return; }
                    User u = doc.toObject(User.class);
                    cb.onResult(Result.ok(u));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Lấy snapshot thô (khi cần đọc field linh hoạt mà chưa map model). */
    public void getUserDoc(@NonNull String uid, @NonNull Result.Callback<DocumentSnapshot> cb) {
        db.collection(Col.USERS).document(uid).get()
                .addOnSuccessListener(doc -> cb.onResult(Result.ok(doc)))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Tạo mới (ghi đè) document hồ sơ user. */
    public void createUser(@NonNull String uid, @NonNull Map<String, Object> data,
                           @NonNull Result.Callback<Void> cb) {
        db.collection(Col.USERS).document(uid).set(data)
                .addOnSuccessListener(v -> cb.onResult(Result.ok(null)))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Cập nhật một số field hồ sơ. */
    public void updateUser(@NonNull String uid, @NonNull Map<String, Object> fields,
                           @NonNull Result.Callback<Void> cb) {
        db.collection(Col.USERS).document(uid).update(fields)
                .addOnSuccessListener(v -> cb.onResult(Result.ok(null)))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Bật/tắt trạng thái online của tài xế (fire-and-forget). */
    public void setDriverOnline(@NonNull String uid, boolean online) {
        Map<String, Object> m = new HashMap<>();
        m.put("driverOnline", online);
        db.collection(Col.USERS).document(uid).update(m);
    }

    /** Lấy tất cả người dùng ở dạng document thô (cho màn Admin quản lý). */
    public void getAllUserDocs(@NonNull Result.Callback<List<DocumentSnapshot>> cb) {
        db.collection(Col.USERS).get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) out.add(d);
                    cb.onResult(Result.ok(out));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Đổi vai trò (role) của một user. */
    public void setRole(@NonNull String uid, @NonNull String role, @NonNull Result.Callback<Void> cb) {
        Map<String, Object> m = new HashMap<>();
        m.put(F.ROLE, role);
        updateUser(uid, m, cb);
    }

    /** Admin duyệt/từ chối hồ sơ tài xế: set driverStatus + isDriver. */
    public void setDriverDecision(@NonNull String uid, @NonNull String driverStatus,
                                  boolean isDriver, @NonNull Result.Callback<Void> cb) {
        Map<String, Object> m = new HashMap<>();
        m.put(F.DRIVER_STATUS, driverStatus);
        m.put("isDriver", isDriver);
        updateUser(uid, m, cb);
    }

    /** Lấy danh sách user theo trạng thái duyệt tài xế (vd "pending"). */
    public void getUsersByDriverStatus(@NonNull String driverStatus,
                                       @NonNull Result.Callback<List<User>> cb) {
        db.collection(Col.USERS).whereEqualTo(F.DRIVER_STATUS, driverStatus).get()
                .addOnSuccessListener(snap -> {
                    List<User> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        User u = d.toObject(User.class);
                        u.setUid(d.getId());
                        out.add(u);
                    }
                    cb.onResult(Result.ok(out));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }
}
