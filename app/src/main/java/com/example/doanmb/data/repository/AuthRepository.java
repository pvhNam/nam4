package com.example.doanmb.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.doanmb.data.FirebaseContract.Col;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.FirebaseContract.Role;
import com.example.doanmb.data.Result;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Cổng xác thực: bọc FirebaseAuth (đăng nhập/đăng ký/quên mật khẩu) và xác định
 * tuyến điều hướng theo vai trò. Là nơi DUY NHẤT tầng UI chạm tới Auth.
 */
public class AuthRepository extends BaseRepository {

    /** Tuyến điều hướng sau đăng nhập. */
    public enum Route { ADMIN, DRIVER, USER }

    private static AuthRepository instance;
    public static synchronized AuthRepository getInstance() {
        if (instance == null) instance = new AuthRepository();
        return instance;
    }
    private AuthRepository() {}

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Nullable
    public FirebaseUser getCurrentUser() { return auth.getCurrentUser(); }

    public void signOut() { auth.signOut(); }

    /** Đăng nhập bằng email + mật khẩu; trả về uid. */
    public void signIn(@NonNull String email, @NonNull String password,
                       @NonNull Result.Callback<String> cb) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(res -> {
                    FirebaseUser u = res.getUser();
                    if (u == null) cb.onResult(Result.fail("Đăng nhập thất bại"));
                    else cb.onResult(Result.ok(u.getUid()));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Tìm email theo số điện thoại (cho phép đăng nhập bằng SĐT). */
    public void resolveEmailByPhone(@NonNull String phone, @NonNull Result.Callback<String> cb) {
        db.collection(Col.USERS).whereEqualTo(F.PHONE, phone).limit(1).get()
                .addOnSuccessListener(snap -> {
                    String email = null;
                    if (!snap.isEmpty()) email = snap.getDocuments().get(0).getString(F.EMAIL);
                    if (email == null || email.trim().isEmpty()) email = phone + "@doanmb.com";
                    cb.onResult(Result.ok(email));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Tạo tài khoản auth mới; trả về uid. */
    public void register(@NonNull String email, @NonNull String password,
                         @NonNull Result.Callback<String> cb) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(res -> {
                    FirebaseUser u = res.getUser();
                    if (u == null) cb.onResult(Result.fail("Đăng ký thất bại"));
                    else cb.onResult(Result.ok(u.getUid()));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    public void sendPasswordReset(@NonNull String email, @NonNull Result.Callback<Void> cb) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> cb.onResult(Result.ok(null)))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Xác định tuyến theo role + cờ isDriver trong users. */
    public void routeFor(@NonNull String uid, @NonNull Result.Callback<Route> cb) {
        db.collection(Col.USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString(F.ROLE);
                    boolean isDriver = Boolean.TRUE.equals(doc.getBoolean("isDriver"));
                    Route route = Role.ADMIN.equals(role) ? Route.ADMIN
                            : isDriver ? Route.DRIVER : Route.USER;
                    cb.onResult(Result.ok(route));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }
}
