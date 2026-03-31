package pl.discountapp.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.discountapp.application.dto.CreateCouponCommand;
import pl.discountapp.application.dto.UseCouponCommand;
import pl.discountapp.application.port.output.GeoLocationPort;
import pl.discountapp.domain.coupon.Coupon;
import pl.discountapp.domain.coupon.CouponAlreadyExistsException;
import pl.discountapp.domain.coupon.CouponCode;
import pl.discountapp.domain.coupon.CouponExhaustedException;
import pl.discountapp.domain.coupon.CouponNotFoundException;
import pl.discountapp.domain.coupon.CouponRepository;
import pl.discountapp.domain.coupon.Country;
import pl.discountapp.domain.coupon.CountryNotAllowedException;
import pl.discountapp.domain.coupon.GeoLocationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    CouponRepository couponRepository;

    @Mock
    GeoLocationPort geoLocationPort;

    @InjectMocks
    CouponService couponService;

    @Test
    void testCreateCouponWhenCodeNotExists() {
        // given
        var cmd = new CreateCouponCommand("SPRING", 10, "PL");
        given(couponRepository.existsByCode(any(CouponCode.class))).willReturn(false);
        given(couponRepository.save(any(Coupon.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        var result = couponService.create(cmd);

        // then
        assertThat(result.code()).isEqualTo("SPRING");
        assertThat(result.maxUses()).isEqualTo(10);
        assertThat(result.country()).isEqualTo("PL");
        then(couponRepository).should().save(any(Coupon.class));
    }

    @Test
    void testThrowWhenCouponCodeAlreadyExists() {
        // given
        var cmd = new CreateCouponCommand("SPRING", 10, "PL");
        given(couponRepository.existsByCode(any(CouponCode.class))).willReturn(true);

        // when / then
        assertThatThrownBy(() -> couponService.create(cmd))
                .isInstanceOf(CouponAlreadyExistsException.class);
    }

    @Test
    void testUseCouponWhenValid() {
        // given
        var coupon = Coupon.create(CouponCode.of("TEST"), 5, Country.of("PL"));
        var cmd = new UseCouponCommand("TEST", "user1", "8.8.8.8");
        given(couponRepository.findByCode(any(CouponCode.class))).willReturn(Optional.of(coupon));
        given(geoLocationPort.resolveCountry("8.8.8.8")).willReturn(Country.of("PL"));
        given(couponRepository.save(any(Coupon.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        var result = couponService.use(cmd);

        // then
        assertThat(result.currentUses()).isEqualTo(1);
        then(geoLocationPort).should().resolveCountry("8.8.8.8");
    }

    @Test
    void testThrowWhenCouponNotFound() {
        // given
        var cmd = new UseCouponCommand("NONEXISTENT", "user1", "8.8.8.8");
        given(couponRepository.findByCode(any(CouponCode.class))).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> couponService.use(cmd))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    void testResolveCountryFromIpWhenUsingCoupon() {
        // given
        var coupon = Coupon.create(CouponCode.of("GEO"), 5, Country.of("DE"));
        var cmd = new UseCouponCommand("GEO", "user1", "1.2.3.4");
        given(couponRepository.findByCode(any(CouponCode.class))).willReturn(Optional.of(coupon));
        given(geoLocationPort.resolveCountry("1.2.3.4")).willReturn(Country.of("DE"));
        given(couponRepository.save(any(Coupon.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        couponService.use(cmd);

        // then
        then(geoLocationPort).should().resolveCountry("1.2.3.4");
    }

    @Test
    void testNotSaveWhenCouponCodeAlreadyExists() {
        // given
        var cmd = new CreateCouponCommand("DUPE", 5, "PL");
        given(couponRepository.existsByCode(any(CouponCode.class))).willReturn(true);

        // when / then
        assertThatThrownBy(() -> couponService.create(cmd))
                .isInstanceOf(CouponAlreadyExistsException.class);
        then(couponRepository).should(never()).save(any(Coupon.class));
    }

    @Test
    void testPropagateCountryNotAllowedFromDomain() {
        // given
        var coupon = Coupon.create(CouponCode.of("GEO"), 5, Country.of("PL"));
        var cmd = new UseCouponCommand("GEO", "user1", "1.2.3.4");
        given(couponRepository.findByCode(any(CouponCode.class))).willReturn(Optional.of(coupon));
        given(geoLocationPort.resolveCountry("1.2.3.4")).willReturn(Country.of("DE"));

        // when / then
        assertThatThrownBy(() -> couponService.use(cmd))
                .isInstanceOf(CountryNotAllowedException.class);
    }

    @Test
    void testPropagateCouponExhaustedFromDomain() {
        // given
        var coupon = Coupon.create(CouponCode.of("FULL"), 1, Country.of("PL"));
        coupon.use(new pl.discountapp.domain.coupon.UserId("first"), Country.of("PL"));
        var cmd = new UseCouponCommand("FULL", "second", "8.8.8.8");
        given(couponRepository.findByCode(any(CouponCode.class))).willReturn(Optional.of(coupon));
        given(geoLocationPort.resolveCountry("8.8.8.8")).willReturn(Country.of("PL"));

        // when / then
        assertThatThrownBy(() -> couponService.use(cmd))
                .isInstanceOf(CouponExhaustedException.class);
    }

    @Test
    void testPropagateGeoLocationException() {
        // given
        var coupon = Coupon.create(CouponCode.of("GEO"), 5, Country.of("PL"));
        var cmd = new UseCouponCommand("GEO", "user1", "bad-ip");
        given(couponRepository.findByCode(any(CouponCode.class))).willReturn(Optional.of(coupon));
        given(geoLocationPort.resolveCountry("bad-ip")).willThrow(new GeoLocationException("lookup failed"));

        // when / then
        assertThatThrownBy(() -> couponService.use(cmd))
                .isInstanceOf(GeoLocationException.class)
                .hasMessageContaining("lookup failed");
    }

    @Test
    void testReturnCorrectResultFieldsOnCreate() {
        // given
        var cmd = new CreateCouponCommand("RESULT", 7, "DE");
        given(couponRepository.existsByCode(any(CouponCode.class))).willReturn(false);
        given(couponRepository.save(any(Coupon.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        var result = couponService.create(cmd);

        // then
        assertThat(result.code()).isEqualTo("RESULT");
        assertThat(result.maxUses()).isEqualTo(7);
        assertThat(result.currentUses()).isZero();
        assertThat(result.country()).isEqualTo("DE");
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void testReturnIncrementedUsesOnUse() {
        // given
        var coupon = Coupon.create(CouponCode.of("USE"), 5, Country.of("PL"));
        var cmd = new UseCouponCommand("USE", "user1", "8.8.8.8");
        given(couponRepository.findByCode(any(CouponCode.class))).willReturn(Optional.of(coupon));
        given(geoLocationPort.resolveCountry("8.8.8.8")).willReturn(Country.of("PL"));
        given(couponRepository.save(any(Coupon.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        var result = couponService.use(cmd);

        // then
        assertThat(result.code()).isEqualTo("USE");
        assertThat(result.currentUses()).isEqualTo(1);
        assertThat(result.maxUses()).isEqualTo(5);
    }

    @Test
    void testNotSaveWhenCouponNotFound() {
        // given
        var cmd = new UseCouponCommand("MISSING", "user1", "8.8.8.8");
        given(couponRepository.findByCode(any(CouponCode.class))).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> couponService.use(cmd))
                .isInstanceOf(CouponNotFoundException.class);
        then(couponRepository).should(never()).save(any(Coupon.class));
    }

    @Test
    void testNormalizeCodeToUppercaseOnCreate() {
        // given
        var cmd = new CreateCouponCommand("lower", 5, "PL");
        given(couponRepository.existsByCode(any(CouponCode.class))).willReturn(false);
        given(couponRepository.save(any(Coupon.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        var result = couponService.create(cmd);

        // then
        assertThat(result.code()).isEqualTo("LOWER");
    }
}
