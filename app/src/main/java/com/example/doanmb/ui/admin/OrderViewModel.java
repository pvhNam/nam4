package com.example.doanmb.ui.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.CarStatus;
import com.example.doanmb.data.FirebaseContract.Deposit;
import com.example.doanmb.data.FirebaseContract.F;
import com.example.doanmb.data.FirebaseContract.OrderStatus;
import com.example.doanmb.data.Result;
import com.example.doanmb.data.repository.OrderRepository;
import com.example.doanmb.data.repository.WalletRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.example.doanmb.util.WalletHelper;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/**
 * ViewModel cho màn quản lý đơn của Admin. Giữ trạng thái danh sách đơn theo tab
 * và xử lý nghiệp vụ xác nhận / hoàn thành (chia tiền) / huỷ (hoàn cọc) thông qua
 * Repository — tách hoàn toàn khỏi tầng giao diện.
 */
public class OrderViewModel extends BaseViewModel {

    public static final int TAB_PENDING   = 0;
    public static final int TAB_CONFIRMED = 1;
    public static final int TAB_ALL       = 2;

    private final OrderRepository orderRepo = OrderRepository.getInstance();
    private final WalletRepository walletRepo = WalletRepository.getInstance();

    private final MutableLiveData<List<DocumentSnapshot>> orders = new MutableLiveData<>();
    private int currentTab = TAB_PENDING;

    public LiveData<List<DocumentSnapshot>> getOrders() { return orders; }
    public int getCurrentTab() { return currentTab; }

    public void selectTab(int tab) {
        currentTab = tab;
        loadOrders();
    }

    public void loadOrders() {
        String status = currentTab == TAB_PENDING ? OrderStatus.PENDING
                : currentTab == TAB_CONFIRMED ? OrderStatus.CONFIRMED
                : null;
        setLoading(true);
        orderRepo.getOrdersByStatus(status, r -> {
            setLoading(false);
            if (r.isSuccess()) orders.setValue(r.getData());
            else postMessage("Lỗi tải đơn: " + r.getError());
        });
    }

    public void confirmOrder(@NonNull String orderId) {
        orderRepo.updateStatus(orderId, OrderStatus.CONFIRMED, null, r -> {
            if (r.isSuccess()) { postMessage("✅ Đã xác nhận đơn hàng"); loadOrders(); }
            else postMessage("Lỗi: " + r.getError());
        });
    }

    /** Hoàn thành đơn: nếu có cọc giữ qua ví thì chia tiền trước rồi đánh dấu completed. */
    public void completeOrder(@NonNull String orderId) {
        orderRepo.getOrder(orderId, r -> {
            if (!r.isSuccess() || r.getData() == null || !r.getData().exists()) {
                postMessage("Không đọc được đơn"); return;
            }
            DocumentSnapshot doc = r.getData();
            String depositStatus = doc.getString(F.DEPOSIT_STATUS);
            String sellerId      = doc.getString(F.SELLER_ID);
            Long   deposit       = doc.getLong(F.DEPOSIT_AMOUNT);

            boolean hasHeldDeposit = Deposit.HELD.equals(depositStatus)
                    && sellerId != null && !sellerId.isEmpty()
                    && deposit != null && deposit > 0;

            if (!hasHeldDeposit) { markCompleted(orderId, null); return; }

            walletRepo.settle(sellerId, deposit, orderId, new WalletHelper.Callback() {
                @Override public void onSuccess() { markCompleted(orderId, Deposit.SETTLED); }
                @Override public void onError(String message) { postMessage("Lỗi chia tiền: " + message); }
            });
        });
    }

    private void markCompleted(@NonNull String orderId, @Nullable String newDepositStatus) {
        orderRepo.updateStatus(orderId, OrderStatus.COMPLETED, newDepositStatus, r -> {
            if (r.isSuccess()) { postMessage("✅ Đơn đã hoàn thành & chia tiền"); loadOrders(); }
            else postMessage("Lỗi: " + r.getError());
        });
    }

    /** Huỷ đơn: trả xe về "active", hoàn 100% cọc nếu đang giữ. */
    public void cancelOrder(@NonNull String orderId) {
        orderRepo.getOrder(orderId, r -> {
            if (!r.isSuccess() || r.getData() == null) { postMessage("Không đọc được đơn"); return; }
            DocumentSnapshot doc = r.getData();
            String carId         = doc.getString(F.CAR_ID);
            String buyerId       = doc.getString(F.BUYER_ID);
            String depositStatus = doc.getString(F.DEPOSIT_STATUS);
            Long   deposit       = doc.getLong(F.DEPOSIT_AMOUNT);

            if (carId != null && !carId.isEmpty()) orderRepo.updateCarStatus(carId, CarStatus.ACTIVE);

            boolean held = Deposit.HELD.equals(depositStatus) && buyerId != null
                    && deposit != null && deposit > 0;
            if (held) walletRepo.refund(buyerId, deposit, orderId, null);

            orderRepo.updateStatus(orderId, OrderStatus.CANCELLED, held ? Deposit.REFUNDED : null, res -> {
                if (res.isSuccess()) {
                    postMessage("Đã hủy đơn hàng" + (held ? " & hoàn cọc cho khách" : ""));
                    loadOrders();
                } else postMessage("Lỗi: " + res.getError());
            });
        });
    }
}
