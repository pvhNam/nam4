package com.example.doanmb.ui.base;

import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Lớp cơ sở cho Fragment trong app: gom vài tiện ích lặp lại (toast an toàn khi
 * fragment còn gắn vào activity). Các Fragment migrate sang MVVM kế thừa lớp này.
 */
public abstract class BaseFragment extends Fragment {

    /** Hiện toast ngắn, bỏ qua nếu fragment đã rời khỏi màn hình. */
    protected void toast(@Nullable String message) {
        if (!isAdded() || getContext() == null || message == null) return;
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
