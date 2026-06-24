package com.example.doanmb.ui.customer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.doanmb.R;
import com.example.doanmb.databinding.ActivityMapPickerBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;
import java.util.Locale;

/**
 * Chọn ĐIỂM ĐÓN/ĐIỂM ĐẾN trên Google Map để đặt chuyến "theo quãng đường".
 * Màn thuần tương tác bản đồ, trả kết quả về qua setResult nên không cần ViewModel.
 */
public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String RESULT_PICKUP      = "result_pickup";
    public static final String RESULT_DEST        = "result_dest";
    public static final String RESULT_DISTANCE_KM = "result_distance_km";

    private static final int REQ_LOCATION = 101;
    private static final LatLng DEFAULT_CENTER = new LatLng(10.776530, 106.700981);

    private ActivityMapPickerBinding binding;
    private GoogleMap map;
    private FusedLocationProviderClient fused;

    private LatLng pickup, dest;
    private String pickupName = "", destName = "";
    private Marker pickupMarker, destMarker;
    private Polyline line;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fused = LocationServices.getFusedLocationProviderClient(this);

        binding.btnMapBack.setOnClickListener(v -> finish());
        binding.btnMapReset.setOnClickListener(v -> resetPoints());
        binding.btnMapConfirm.setOnClickListener(v -> confirm());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, 12f));
        map.setOnMapClickListener(this::onPick);
        enableMyLocationAndCenter();
    }

    private void onPick(LatLng point) {
        if (pickup == null) {
            pickup = point;
            pickupMarker = addMarker(point, "Điểm đón", BitmapDescriptorFactory.HUE_GREEN, pickupMarker);
            geocode(point, true);
            binding.tvMapHint.setText("Chạm bản đồ để chọn ĐIỂM ĐẾN");
        } else {
            dest = point;
            destMarker = addMarker(point, "Điểm đến", BitmapDescriptorFactory.HUE_RED, destMarker);
            geocode(point, false);
            binding.tvMapHint.setText("Chạm lại để đổi điểm đến, hoặc bấm Xác nhận");
        }
        updateDistance();
    }

    private Marker addMarker(LatLng point, String title, float hue, Marker old) {
        if (old != null) old.remove();
        return map.addMarker(new MarkerOptions().position(point).title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));
    }

    private void resetPoints() {
        pickup = dest = null;
        pickupName = destName = "";
        if (pickupMarker != null) { pickupMarker.remove(); pickupMarker = null; }
        if (destMarker != null) { destMarker.remove(); destMarker = null; }
        if (line != null) { line.remove(); line = null; }
        binding.tvMapHint.setText("Chạm bản đồ để chọn ĐIỂM ĐÓN");
        binding.tvMapPickup.setText("Điểm đón: chưa chọn");
        binding.tvMapDest.setText("Điểm đến: chưa chọn");
        binding.tvMapDistance.setText("Quãng đường: --");
        binding.btnMapConfirm.setEnabled(false);
    }

    private void updateDistance() {
        if (line != null) { line.remove(); line = null; }
        if (pickup == null || dest == null) { binding.btnMapConfirm.setEnabled(false); return; }
        line = map.addPolyline(new PolylineOptions().add(pickup, dest).width(8f).color(0xFF2E6BF0));
        double km = haversineKm(pickup, dest);
        binding.tvMapDistance.setText(String.format(Locale.US, "Quãng đường: %.1f km", km));
        binding.btnMapConfirm.setEnabled(true);
        try {
            com.google.android.gms.maps.model.LatLngBounds bounds =
                    new com.google.android.gms.maps.model.LatLngBounds.Builder()
                            .include(pickup).include(dest).build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160));
        } catch (Exception ignored) {}
    }

    private void confirm() {
        if (pickup == null || dest == null) {
            Toast.makeText(this, "Hãy chọn cả điểm đón và điểm đến", Toast.LENGTH_SHORT).show();
            return;
        }
        double km = Math.round(haversineKm(pickup, dest) * 10) / 10.0;
        android.content.Intent data = new android.content.Intent();
        data.putExtra(RESULT_PICKUP, !pickupName.isEmpty() ? pickupName : latLngText(pickup));
        data.putExtra(RESULT_DEST, !destName.isEmpty() ? destName : latLngText(dest));
        data.putExtra(RESULT_DISTANCE_KM, km);
        setResult(RESULT_OK, data);
        finish();
    }

    private void enableMyLocationAndCenter() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        try { map.setMyLocationEnabled(true); } catch (SecurityException ignored) {}
        fused.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null && map != null) {
                LatLng here = new LatLng(loc.getLatitude(), loc.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 14f));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationAndCenter();
        }
    }

    private void geocode(LatLng point, boolean isPickup) {
        new Thread(() -> {
            String name = latLngText(point);
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("vi"));
                List<Address> list = geocoder.getFromLocation(point.latitude, point.longitude, 1);
                if (list != null && !list.isEmpty()) {
                    Address a = list.get(0);
                    String line0 = a.getMaxAddressLineIndex() >= 0 ? a.getAddressLine(0) : null;
                    if (line0 != null && !line0.isEmpty()) name = line0;
                }
            } catch (Exception ignored) {}
            final String finalName = name;
            runOnUiThread(() -> {
                if (isPickup) {
                    pickupName = finalName;
                    binding.tvMapPickup.setText("Điểm đón: " + finalName);
                } else {
                    destName = finalName;
                    binding.tvMapDest.setText("Điểm đến: " + finalName);
                }
            });
        }).start();
    }

    private static String latLngText(LatLng p) {
        return String.format(Locale.US, "%.5f, %.5f", p.latitude, p.longitude);
    }

    private static double haversineKm(LatLng a, LatLng b) {
        double r = 6371.0;
        double dLat = Math.toRadians(b.latitude - a.latitude);
        double dLng = Math.toRadians(b.longitude - a.longitude);
        double s = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(s), Math.sqrt(1 - s));
    }
}
