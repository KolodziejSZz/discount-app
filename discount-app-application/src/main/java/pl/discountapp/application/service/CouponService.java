package pl.discountapp.application.service;

import lombok.RequiredArgsConstructor;
import pl.discountapp.application.dto.CouponResult;
import pl.discountapp.application.dto.CreateCouponCommand;
import pl.discountapp.application.dto.UseCouponCommand;
import pl.discountapp.application.port.input.CreateCouponUseCase;
import pl.discountapp.application.port.input.UseCouponUseCase;
import pl.discountapp.application.port.output.GeoLocationPort;
import pl.discountapp.domain.coupon.Coupon;
import pl.discountapp.domain.coupon.CouponAlreadyExistsException;
import pl.discountapp.domain.coupon.CouponCode;
import pl.discountapp.domain.coupon.CouponNotFoundException;
import pl.discountapp.domain.coupon.CouponRepository;
import pl.discountapp.domain.coupon.Country;
import pl.discountapp.domain.coupon.UserId;

@RequiredArgsConstructor
public class CouponService implements CreateCouponUseCase, UseCouponUseCase {

    private final CouponRepository couponRepository;
    private final GeoLocationPort geoLocationPort;

    @Override
    public CouponResult create(CreateCouponCommand cmd) {
        var code = CouponCode.of(cmd.code());
        if (couponRepository.existsByCode(code)) {
            throw new CouponAlreadyExistsException(code.value());
        }
        var coupon = Coupon.create(code, cmd.maxUses(), Country.of(cmd.country()));
        var saved = couponRepository.save(coupon);
        return toResult(saved);
    }

    @Override
    public CouponResult use(UseCouponCommand cmd) {
        var code = CouponCode.of(cmd.couponCode());
        var coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new CouponNotFoundException(code.value()));
        var userCountry = geoLocationPort.resolveCountry(cmd.ipAddress());
        coupon.use(UserId.of(cmd.userId()), userCountry);
        var saved = couponRepository.save(coupon);
        return toResult(saved);
    }

    private CouponResult toResult(Coupon coupon) {
        return new CouponResult(
                coupon.getCode().value(),
                coupon.getCreatedAt(),
                coupon.getMaxUses(),
                coupon.getCurrentUses(),
                coupon.getCountry().value()
        );
    }
}
