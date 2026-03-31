package pl.discountapp.domain.coupon;

public class CountryNotAllowedException extends RuntimeException {
    public CountryNotAllowedException(String couponCountry, String userCountry) {
        super("Country not allowed. Coupon is for " + couponCountry + ", but user is from " + userCountry);
    }
}
