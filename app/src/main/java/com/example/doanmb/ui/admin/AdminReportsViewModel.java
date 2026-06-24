package com.example.doanmb.ui.admin;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.repository.ReportRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/** ViewModel cho màn Admin xử lý khiếu nại/báo cáo. */
public class AdminReportsViewModel extends BaseViewModel {

    public static final String STATUS_PENDING   = "pending";
    public static final String STATUS_RESOLVED  = "resolved";
    public static final String STATUS_DISMISSED = "dismissed";

    private final ReportRepository repo = ReportRepository.getInstance();
    private final MutableLiveData<List<DocumentSnapshot>> reports = new MutableLiveData<>();
    private boolean showingPending = true;

    public LiveData<List<DocumentSnapshot>> getReports() { return reports; }
    public boolean isShowingPending() { return showingPending; }

    public void selectTab(boolean pending) {
        showingPending = pending;
        load();
    }

    public void load() {
        setLoading(true);
        repo.getReportsByStatus(showingPending ? STATUS_PENDING : null, r -> {
            setLoading(false);
            if (r.isSuccess()) reports.setValue(r.getData());
            else postMessage("Lỗi tải khiếu nại: " + r.getError());
        });
    }

    public void resolve(@NonNull String reportId) { update(reportId, STATUS_RESOLVED, "✅ Đã đánh dấu xử lý"); }
    public void dismiss(@NonNull String reportId) { update(reportId, STATUS_DISMISSED, "Đã bỏ qua khiếu nại"); }

    private void update(String reportId, String status, String okMsg) {
        repo.setStatus(reportId, status, r -> {
            postMessage(r.isSuccess() ? okMsg : "Lỗi: " + r.getError());
            if (r.isSuccess()) load();
        });
    }
}
