package pl.discountapp.domain.coupon;

public class CouponAlreadyUsedException extends RuntimeException {
    public CouponAlreadyUsedException(String userId, String couponCode) {
        super("User " + userId + " already used coupon " + couponCode);
    }
}
