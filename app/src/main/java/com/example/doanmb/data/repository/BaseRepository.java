package com.example.doanmb.data.repository;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Lớp cơ sở cho mọi Repository: cung cấp Firestore và thông tin user đăng nhập.
 * Repository là nơi DUY NHẤT chạm tới Firestore — tầng UI/ViewModel không gọi
 * FirebaseFirestore.getInstance() trực tiếp nữa.
 */
public abstract class BaseRepository {

    protected final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** UID của user đang đăng nhập, hoặc null nếu chưa đăng nhập. */
    @Nullable
    protected String currentUid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }
}
