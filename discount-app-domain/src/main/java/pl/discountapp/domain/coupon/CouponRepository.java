package pl.discountapp.domain.coupon;

import java.util.Optional;

public interface CouponRepository {

    Optional<Coupon> findByCode(CouponCode code);

    Coupon save(Coupon coupon);

    boolean existsByCode(CouponCode code);
}
