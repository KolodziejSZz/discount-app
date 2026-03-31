package pl.discountapp.domain.coupon;

public class CouponExhaustedException extends RuntimeException {
    public CouponExhaustedException(String code) {
        super("Coupon exhausted: " + code);
    }
}
