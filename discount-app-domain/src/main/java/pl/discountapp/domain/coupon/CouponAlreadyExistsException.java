package pl.discountapp.domain.coupon;

public class CouponAlreadyExistsException extends RuntimeException {
    public CouponAlreadyExistsException(String code) {
        super("Coupon already exists: " + code);
    }
}
