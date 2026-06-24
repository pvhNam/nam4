package com.example.doanmb.data.repository;

import androidx.annotation.NonNull;

import com.example.doanmb.data.FirebaseContract.Col;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.Result;
import com.example.doanmb.model.Trip;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Truy cập chuyến tài xế (collection "trips").
 */
public class TripRepository extends BaseRepository {

    private static TripRepository instance;
    public static synchronized TripRepository getInstance() {
        if (instance == null) instance = new TripRepository();
        return instance;
    }
    private TripRepository() {}

    /** Chuyến của 1 tài xế theo trạng thái (vd completed). */
    public void getTripsByDriver(@NonNull String driverId, @NonNull String status,
                                 @NonNull Result.Callback<List<DocumentSnapshot>> cb) {
        db.collection(Col.TRIPS)
                .whereEqualTo("driverId", driverId)
                .whereEqualTo(F.STATUS, status)
                .get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) out.add(d);
                    cb.onResult(Result.ok(out));
                })
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Các chuyến đang chờ (status = waiting) — trả model Trip. */
    public void getWaitingTrips(@NonNull Result.Callback<List<Trip>> cb) {
        db.collection(Col.TRIPS).whereEqualTo(F.STATUS, Trip.STATUS_WAITING).get()
                .addOnSuccessListener(snap -> cb.onResult(Result.ok(mapTrips(snap))))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Tất cả chuyến của 1 tài xế (mọi trạng thái) — trả model Trip. */
    public void getMyTrips(@NonNull String driverId, @NonNull Result.Callback<List<Trip>> cb) {
        db.collection(Col.TRIPS).whereEqualTo("driverId", driverId).get()
                .addOnSuccessListener(snap -> cb.onResult(Result.ok(mapTrips(snap))))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Tài xế nhận 1 chuyến (transaction: chỉ nhận được nếu còn waiting). */
    public void acceptTrip(@NonNull String tripId, @NonNull String driverId,
                           @NonNull String driverName, @NonNull Result.Callback<Void> cb) {
        DocumentReference ref = db.collection(Col.TRIPS).document(tripId);
        db.runTransaction(tr -> {
            DocumentSnapshot snap = tr.get(ref);
            if (!Trip.STATUS_WAITING.equals(snap.getString(F.STATUS))) {
                throw new FirebaseFirestoreException("Đơn đã được nhận",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            tr.update(ref,
                    F.STATUS, Trip.STATUS_RUNNING,
                    "driverId", driverId,
                    "driverName", driverName,
                    "acceptedAt", Timestamp.now());
            return null;
        }).addOnSuccessListener(v -> cb.onResult(Result.ok(null)))
          .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    /** Tài xế hoàn thành 1 chuyến. */
    public void completeTrip(@NonNull String tripId, @NonNull Result.Callback<Void> cb) {
        db.collection(Col.TRIPS).document(tripId)
                .update(F.STATUS, Trip.STATUS_COMPLETED, "completedAt", Timestamp.now())
                .addOnSuccessListener(v -> cb.onResult(Result.ok(null)))
                .addOnFailureListener(e -> cb.onResult(Result.fail(e.getMessage())));
    }

    private List<Trip> mapTrips(Iterable<QueryDocumentSnapshot> snap) {
        List<Trip> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : snap) {
            Trip t = d.toObject(Trip.class);
            t.setId(d.getId());
            out.add(t);
        }
        return out;
    }
}
