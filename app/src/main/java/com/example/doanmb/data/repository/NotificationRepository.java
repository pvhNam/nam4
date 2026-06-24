package com.example.doanmb.data.repository;

import androidx.annotation.NonNull;

import com.example.doanmb.data.FirebaseContract.Col;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

/**
 * Ghi thông báo trong app (collection "notifications").
 */
public class NotificationRepository extends BaseRepository {

    private static NotificationRepository instance;
    public static synchronized NotificationRepository getInstance() {
        if (instance == null) instance = new NotificationRepository();
        return instance;
    }
    private NotificationRepository() {}

    /** Tạo một thông báo gửi tới user. */
    public void notify(@NonNull String userId, @NonNull String title, @NonNull String body) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("userId", userId);
        notif.put("title", title);
        notif.put("body", body);
        notif.put("createdAt", Timestamp.now());
        notif.put("read", false);
        db.collection(Col.NOTIFICATIONS).add(notif);
    }
}
