package pl.discountapp.domain.coupon;

import java.util.Objects;

public record CouponCode(String value) {

    public CouponCode {
        Objects.requireNonNull(value);
        if (value.length() < 3 || value.length() > 50) {
            throw new IllegalArgumentException("Coupon code must be between 3 and 50 characters");
        }
        value = value.toUpperCase();
    }

    public static CouponCode of(String value) {
        return new CouponCode(value);
    }
}
