package com.example.doanmb.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.doanmb.data.FirebaseContract.Col;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.Result;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Truy cập khiếu nại / báo cáo (collection "reports").
 */
public class ReportRepository extends BaseRepository {

    private static ReportRepository instance;
    public static synchronized ReportRepository getInstance() {
        if (instance == null) instance = new ReportRepository();
        return instance;
    }
    private ReportRepository() {}

    /** Lấy báo cáo theo trạng thái; status == null nghĩa là lấy tất cả. */
    public void getReportsByStatus(@Nullable String status,
                                   @NonNull Result.Callback<List<DocumentSnapshot>> cb) {
        Query q = db.collection(Col.REPORTS);
        if (status != null) q = q.whereEqualTo(F.STATUS, status);
        q.get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) out.add(d);
                    cb.onResult(Result.ok(out));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    public void setStatus(@NonNull String reportId, @NonNull String status,
                          @NonNull Result.Callback<Void> cb) {
        Map<String, Object> m = new HashMap<>();
        m.put(F.STATUS, status);
        db.collection(Col.REPORTS).document(reportId).update(m)
                .addOnSuccessListener(v -> cb.onResult(Result.ok(null)))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }
}
