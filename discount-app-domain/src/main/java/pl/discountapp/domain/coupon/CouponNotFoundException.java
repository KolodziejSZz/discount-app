package pl.discountapp.domain.coupon;

public class CouponNotFoundException extends RuntimeException {
    public CouponNotFoundException(String code) {
        super("Coupon not found: " + code);
    }
}
