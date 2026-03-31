package pl.discountapp.domain.coupon;

import java.time.Instant;
import java.util.Objects;

public record CouponUsage(UserId userId, Instant usedAt) {

    public CouponUsage {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(usedAt);
    }

    public static CouponUsage of(UserId userId) {
        return new CouponUsage(userId, Instant.now());
    }
}
