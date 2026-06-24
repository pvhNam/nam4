package com.example.doanmb.ui.base;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Lớp cơ sở cho ViewModel: cung cấp 2 trạng thái UI dùng chung là "đang tải" và
 * "thông điệp" (lỗi/thông báo), để Fragment/Activity quan sát và hiển thị.
 */
public abstract class BaseViewModel extends ViewModel {

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getMessage()  { return message; }

    protected void setLoading(boolean isLoading) { loading.setValue(isLoading); }
    protected void postMessage(@Nullable String msg) { message.setValue(msg); }
}
