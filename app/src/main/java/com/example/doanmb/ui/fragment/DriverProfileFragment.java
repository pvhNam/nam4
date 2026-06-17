package com.example.doanmb.ui.fragment;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.doanmb.MainActivity;
import com.example.doanmb.R;
import com.example.doanmb.model.Trip;
import com.example.doanmb.ui.activity.LoginActivity;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Tab tài khoản của tài xế: hiển thị hồ sơ, thống kê doanh thu + đăng xuất. */
public class DriverProfileFragment extends Fragment {

    private static final int MODE_DAY = 0, MODE_MONTH = 1, MODE_YEAR = 2;

    private TextView tvName, tvCarType, tvCccd, tvLicense, tvPhone;
    private TextView tvPeriodLabel, tvRevSelected, tvTripsSelected;
    private ImageView ivAvatar;

    private static final DecimalFormat MONEY = new DecimalFormat("#,###");

    /** Chế độ xem hiện tại (ngày/tháng/năm) và mốc thời gian được chọn. */
    private int mode = MODE_MONTH;
    private final Calendar selected = Calendar.getInstance();

    /** Cache các chuyến đã hoàn thành để chuyển kỳ không phải tải lại. */
    private final List<double[]> completed = new ArrayList<>(); // [completedAtMillis, price]

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_profile, container, false);

        tvName = view.findViewById(R.id.tv_dp_name);
        tvCarType = view.findViewById(R.id.tv_dp_cartype);
        tvCccd = view.findViewById(R.id.tv_dp_cccd);
        tvLicense = view.findViewById(R.id.tv_dp_license);
        tvPhone = view.findViewById(R.id.tv_dp_phone);
        ivAvatar = view.findViewById(R.id.iv_dp_avatar);

        tvPeriodLabel = view.findViewById(R.id.tv_period_label);
        tvRevSelected = view.findViewById(R.id.tv_rev_selected);
        tvTripsSelected = view.findViewById(R.id.tv_trips_selected);

        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_rev_mode);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_mode_day) mode = MODE_DAY;
            else if (checkedId == R.id.btn_mode_year) mode = MODE_YEAR;
            else mode = MODE_MONTH;
            refreshSelectedStat();
        });

        view.findViewById(R.id.btn_period_prev).setOnClickListener(v -> shiftPeriod(-1));
        view.findViewById(R.id.btn_period_next).setOnClickListener(v -> shiftPeriod(1));
        tvPeriodLabel.setOnClickListener(v -> openPeriodPicker());

        view.findViewById(R.id.btn_dp_logout).setOnClickListener(v -> logout());
        view.findViewById(R.id.btn_dp_switch_user).setOnClickListener(v -> switchToUserMode());

        loadInfo();
        refreshSelectedStat();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCompletedTrips();
    }

    private void loadInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || !doc.exists()) return;
                    tvName.setText(text(doc.getString("name"), "Tài xế"));
                    tvCarType.setText(text(doc.getString("driverCarType"), "--"));
                    tvCccd.setText(text(doc.getString("cccd"), "--"));
                    tvLicense.setText(text(doc.getString("licenseNumber"), "--"));
                    tvPhone.setText(text(doc.getString("phone"), "--"));
                    String avatar = doc.getString("avatarUrl");
                    if (avatar != null && !avatar.isEmpty()) {
                        Glide.with(this).load(avatar).into(ivAvatar);
                    }
                });
    }

    private String text(String value, String def) {
        return value != null && !value.isEmpty() ? value : def;
    }

    /** Tải 1 lần toàn bộ chuyến đã hoàn thành của tài xế, lưu cache rồi tính lại kỳ đang chọn. */
    private void loadCompletedTrips() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore.getInstance().collection("trips")
                .whereEqualTo("driverId", user.getUid())
                .whereEqualTo("status", Trip.STATUS_COMPLETED)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    completed.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        Timestamp done = d.getTimestamp("completedAt");
                        if (done == null) continue;
                        Double price = d.getDouble("price");
                        completed.add(new double[]{done.toDate().getTime(), price != null ? price : 0});
                    }
                    refreshSelectedStat();
                });
    }

    /** Dịch kỳ đang chọn tiến/lùi theo đơn vị của chế độ hiện tại. */
    private void shiftPeriod(int delta) {
        if (mode == MODE_DAY) selected.add(Calendar.DAY_OF_MONTH, delta);
        else if (mode == MODE_YEAR) selected.add(Calendar.YEAR, delta);
        else selected.add(Calendar.MONTH, delta);
        refreshSelectedStat();
    }

    /** Cập nhật nhãn kỳ + cộng doanh thu các chuyến rơi vào [đầu kỳ, cuối kỳ). */
    private void refreshSelectedStat() {
        if (tvPeriodLabel == null) return;
        tvPeriodLabel.setText(periodLabel());

        long start = periodStart().getTimeInMillis();
        long end = periodEnd().getTimeInMillis();
        double revenue = 0;
        int trips = 0;
        for (double[] t : completed) {
            if (t[0] >= start && t[0] < end) {
                revenue += t[1];
                trips++;
            }
        }
        tvRevSelected.setText(MONEY.format(revenue) + " đ");
        tvTripsSelected.setText(trips + " chuyến hoàn thành");
    }

    private String periodLabel() {
        if (mode == MODE_DAY) {
            return new java.text.SimpleDateFormat("'Ngày' dd/MM/yyyy", Locale.getDefault())
                    .format(selected.getTime());
        } else if (mode == MODE_YEAR) {
            return "Năm " + selected.get(Calendar.YEAR);
        }
        return "Tháng " + (selected.get(Calendar.MONTH) + 1) + "/" + selected.get(Calendar.YEAR);
    }

    /** Mốc đầu kỳ (0h) tuỳ theo chế độ. */
    private Calendar periodStart() {
        Calendar c = (Calendar) selected.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (mode == MODE_MONTH || mode == MODE_YEAR) c.set(Calendar.DAY_OF_MONTH, 1);
        if (mode == MODE_YEAR) c.set(Calendar.MONTH, Calendar.JANUARY);
        return c;
    }

    /** Mốc đầu kỳ kế tiếp (giới hạn trên, không bao gồm). */
    private Calendar periodEnd() {
        Calendar c = periodStart();
        if (mode == MODE_DAY) c.add(Calendar.DAY_OF_MONTH, 1);
        else if (mode == MODE_YEAR) c.add(Calendar.YEAR, 1);
        else c.add(Calendar.MONTH, 1);
        return c;
    }

    /** Mở bộ chọn phù hợp với chế độ: lịch ngày / chọn tháng-năm / chọn năm. */
    private void openPeriodPicker() {
        if (mode == MODE_DAY) {
            new DatePickerDialog(requireContext(), (v, y, m, d) -> {
                selected.set(y, m, d);
                refreshSelectedStat();
            }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH),
                    selected.get(Calendar.DAY_OF_MONTH)).show();
        } else if (mode == MODE_YEAR) {
            showYearPicker();
        } else {
            showMonthYearPicker();
        }
    }

    private void showYearPicker() {
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        NumberPicker yearPicker = new NumberPicker(requireContext());
        yearPicker.setMinValue(thisYear - 10);
        yearPicker.setMaxValue(thisYear + 1);
        yearPicker.setValue(selected.get(Calendar.YEAR));
        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn năm")
                .setView(wrap(yearPicker))
                .setPositiveButton("Chọn", (d, w) -> {
                    selected.set(Calendar.YEAR, yearPicker.getValue());
                    refreshSelectedStat();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void showMonthYearPicker() {
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        NumberPicker monthPicker = new NumberPicker(requireContext());
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(selected.get(Calendar.MONTH) + 1);

        NumberPicker yearPicker = new NumberPicker(requireContext());
        yearPicker.setMinValue(thisYear - 10);
        yearPicker.setMaxValue(thisYear + 1);
        yearPicker.setValue(selected.get(Calendar.YEAR));

        android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        row.setPadding(pad, pad, pad, pad);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(monthPicker, lp);
        row.addView(yearPicker, lp);

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn tháng / năm")
                .setView(row)
                .setPositiveButton("Chọn", (d, w) -> {
                    selected.set(Calendar.YEAR, yearPicker.getValue());
                    selected.set(Calendar.MONTH, monthPicker.getValue() - 1);
                    refreshSelectedStat();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    /** Bọc 1 view vào layout có padding để hiển thị đẹp trong dialog. */
    private View wrap(View child) {
        android.widget.FrameLayout frame = new android.widget.FrameLayout(requireContext());
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        frame.setPadding(pad, pad, pad, pad);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER);
        frame.addView(child, lp);
        return frame;
    }

    /** Quay về giao diện khách hàng, vẫn giữ đăng nhập. */
    private void switchToUserMode() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
}
