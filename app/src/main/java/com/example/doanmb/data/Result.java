package com.example.doanmb.data;

import androidx.annotation.Nullable;

/**
 * Bọc kết quả của một thao tác bất đồng bộ (Firestore) thành 1 đối tượng:
 * hoặc thành công kèm dữ liệu, hoặc thất bại kèm thông điệp lỗi.
 *
 * Vì dự án viết bằng Java (không có coroutine), Repository trả kết quả về qua
 * {@link Callback} thay vì lồng nhiều addOnSuccess/addOnFailure ở tầng UI.
 *
 * @param <T> kiểu dữ liệu trả về khi thành công
 */
public final class Result<T> {

    private final boolean success;
    @Nullable private final T data;
    @Nullable private final String error;

    private Result(boolean success, @Nullable T data, @Nullable String error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> Result<T> ok(@Nullable T data) { return new Result<>(true, data, null); }

    public static <T> Result<T> fail(@Nullable String error) {
        return new Result<>(false, null, error != null ? error : "Lỗi không xác định");
    }

    public boolean isSuccess()          { return success; }
    @Nullable public T getData()        { return data; }
    @Nullable public String getError()  { return error; }

    /** Callback nhận về một {@link Result}. */
    public interface Callback<T> {
        void onResult(Result<T> result);
    }
}
