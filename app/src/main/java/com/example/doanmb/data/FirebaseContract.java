package com.example.doanmb.data;

/**
 * Gom toàn bộ "magic string" của Firestore vào một nơi: tên collection, tên field
 * và các giá trị enum (role, status, type). Mục tiêu: gõ sai 1 ký tự sẽ lỗi biên
 * dịch thay vì lỗi âm thầm lúc chạy, và đổi tên chỉ sửa 1 chỗ.
 *
 * Lưu ý: các hằng số ở đây PHẢN ÁNH ĐÚNG dữ liệu đang lưu trên Firestore hiện tại
 * (kể cả chỗ chưa nhất quán như role), KHÔNG đổi giá trị để tránh phá dữ liệu cũ.
 */
public final class FirebaseContract {

    private FirebaseContract() {}

    /** Tên các collection. */
    public static final class Col {
        public static final String USERS          = "users";
        public static final String CARS           = "cars";
        public static final String ORDERS         = "orders";
        public static final String TRIPS          = "trips";
        public static final String TRANSACTIONS   = "transactions";
        public static final String APP_WALLET     = "app_wallet";
        public static final String CHAT_ROOMS     = "chat_rooms";
        public static final String MESSAGES       = "messages";       // sub-collection trong chat_rooms
        public static final String REPORTS        = "reports";
        public static final String MESSAGE_REPORTS= "message_reports";
        public static final String NOTIFICATIONS  = "notifications";
        public static final String BLOCKS         = "blocks";
        public static final String FAVORITES      = "favorites";

        public static final String APP_WALLET_DOC = "main"; // doc ví của app
        private Col() {}
    }

    /** Tên field dùng chung nhiều nơi. */
    public static final class F {
        public static final String UID            = "uid";
        public static final String NAME           = "name";
        public static final String EMAIL          = "email";
        public static final String PHONE          = "phone";
        public static final String ROLE           = "role";
        public static final String AVATAR_URL     = "avatarUrl";
        public static final String BALANCE        = "balance";
        public static final String STATUS         = "status";
        public static final String TYPE           = "type";
        public static final String BRAND          = "brand";
        public static final String PRICE          = "price";
        public static final String INFO           = "info";
        public static final String IMAGE_URL      = "imageUrl";
        public static final String SELLER_ID      = "sellerId";
        public static final String BUYER_ID       = "buyerId";
        public static final String CAR_ID         = "carId";
        public static final String DRIVER_STATUS  = "driverStatus";
        public static final String DEPOSIT_STATUS = "depositStatus";
        public static final String DEPOSIT_AMOUNT = "depositAmount";
        public static final String PRICE_PER_DAY  = "pricePerDay";
        public static final String PRICE_PER_KM   = "pricePerKm";
        public static final String CREATED_AT     = "createdAt";
        private F() {}
    }

    /** Giá trị role (giữ nguyên hiện trạng — có chỗ chưa nhất quán hoa/thường). */
    public static final class Role {
        public static final String ADMIN  = "ADMIN";
        public static final String DRIVER = "driver";
        public static final String USER   = "user";
        private Role() {}
    }

    /** Loại xe / bài đăng (field type của collection cars). */
    public static final class CarType {
        public static final String SALE        = "sale";
        public static final String RENTAL      = "rental";
        public static final String DRIVER      = "driver";
        public static final String DRIVER_ONLY = "driver_only";
        private CarType() {}
    }

    /** Trạng thái đơn (orders.status). */
    public static final class OrderStatus {
        public static final String PENDING   = "pending";
        public static final String CONFIRMED = "confirmed";
        public static final String COMPLETED = "completed";
        public static final String CANCELLED = "cancelled";
        private OrderStatus() {}
    }

    /** Trạng thái xe (cars.status). */
    public static final class CarStatus {
        public static final String ACTIVE   = "active";
        public static final String PENDING  = "pending";
        public static final String APPROVED = "approved";
        public static final String REJECTED = "rejected";
        public static final String SOLD     = "sold";
        private CarStatus() {}
    }

    /** Trạng thái cọc (orders.depositStatus). */
    public static final class Deposit {
        public static final String HELD     = "held";
        public static final String SETTLED  = "settled";
        public static final String REFUNDED = "refunded";
        private Deposit() {}
    }

    /** Trạng thái duyệt tài xế (users.driverStatus). */
    public static final class DriverStatus {
        public static final String PENDING  = "pending";
        public static final String APPROVED = "approved";
        public static final String REJECTED = "rejected";
        private DriverStatus() {}
    }
}
