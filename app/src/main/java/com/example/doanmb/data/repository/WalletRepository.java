package com.example.doanmb.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.doanmb.data.FirebaseContract.Col;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.Result;
import com.example.doanmb.util.WalletHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Cổng truy cập ví/giao dịch ở tầng dữ liệu. Phần dịch chuyển tiền (cọc, hoàn,
 * tất toán, nạp) ủy thác cho {@link WalletHelper} đã được kiểm thử để giữ nguyên
 * logic; repository bổ sung phần ĐỌC (số dư, lịch sử giao dịch) cho tầng UI.
 */
public class WalletRepository extends BaseRepository {

    private static WalletRepository instance;
    public static synchronized WalletRepository getInstance() {
        if (instance == null) instance = new WalletRepository();
        return instance;
    }
    private WalletRepository() {}

    /** Đọc số dư ví của app (hoa hồng cộng dồn). */
    public void getAppWalletBalance(@NonNull Result.Callback<Long> cb) {
        db.collection(Col.APP_WALLET).document(Col.APP_WALLET_DOC).get()
                .addOnSuccessListener(d -> {
                    Double b = d.getDouble(F.BALANCE);
                    cb.onResult(Result.ok(b != null ? Math.round(b) : 0L));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Đọc số dư ví của user (VNĐ). */
    public void getBalance(@NonNull String uid, @NonNull Result.Callback<Long> cb) {
        db.collection(Col.USERS).document(uid).get()
                .addOnSuccessListener(d -> {
                    Double b = d.getDouble(F.BALANCE);
                    cb.onResult(Result.ok(b != null ? Math.round(b) : 0L));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Lịch sử giao dịch liên quan tới 1 user (gửi hoặc nhận). */
    public void getTransactions(@NonNull String uid, @NonNull Result.Callback<List<DocumentSnapshot>> cb) {
        Query q = db.collection(Col.TRANSACTIONS).orderBy(F.CREATED_AT, Query.Direction.DESCENDING);
        q.get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        String from = d.getString("fromUserId");
                        String to   = d.getString("toUserId");
                        if (uid.equals(from) || uid.equals(to)) out.add(d);
                    }
                    cb.onResult(Result.ok(out));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    // ── Ủy thác dịch chuyển tiền cho WalletHelper (giữ nguyên logic đã kiểm thử) ──

    public void topUp(@NonNull String userId, long amount, @Nullable WalletHelper.Callback cb) {
        WalletHelper.topUp(userId, amount, cb);
    }

    public void holdDeposit(@NonNull String customerId, long deposit,
                            @Nullable String orderId, @Nullable WalletHelper.Callback cb) {
        WalletHelper.holdDeposit(customerId, deposit, orderId, cb);
    }

    public void settle(@NonNull String driverId, long deposit,
                       @Nullable String orderId, @Nullable WalletHelper.Callback cb) {
        WalletHelper.settle(driverId, deposit, orderId, cb);
    }

    public void refund(@NonNull String customerId, long deposit,
                       @Nullable String orderId, @Nullable WalletHelper.Callback cb) {
        WalletHelper.refund(customerId, deposit, orderId, cb);
    }
}
