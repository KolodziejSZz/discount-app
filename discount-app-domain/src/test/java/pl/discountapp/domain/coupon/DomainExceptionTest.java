package pl.discountapp.domain.coupon;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    @Test
    void testCouponNotFoundContainCode() {
        // when
        var ex = new CouponNotFoundException("SPRING");

        // then
        assertThat(ex.getMessage()).contains("SPRING");
    }

    @Test
    void testCouponExhaustedContainCode() {
        // when
        var ex = new CouponExhaustedException("SPRING");

        // then
        assertThat(ex.getMessage()).contains("SPRING");
    }

    @Test
    void testCountryNotAllowedContainBothCountries() {
        // when
        var ex = new CountryNotAllowedException("PL", "DE");

        // then
        assertThat(ex.getMessage()).contains("PL").contains("DE");
    }

    @Test
    void testCouponAlreadyUsedContainUserAndCode() {
        // when
        var ex = new CouponAlreadyUsedException("user1", "SPRING");

        // then
        assertThat(ex.getMessage()).contains("user1").contains("SPRING");
    }

    @Test
    void testCouponAlreadyExistsContainCode() {
        // when
        var ex = new CouponAlreadyExistsException("SPRING");

        // then
        assertThat(ex.getMessage()).contains("SPRING");
    }

    @Test
    void testGeoLocationExceptionContainMessage() {
        // when
        var ex = new GeoLocationException("lookup failed");

        // then
        assertThat(ex.getMessage()).isEqualTo("lookup failed");
    }

    @Test
    void testGeoLocationExceptionContainCause() {
        // given
        var cause = new RuntimeException("timeout");

        // when
        var ex = new GeoLocationException("lookup failed", cause);

        // then
        assertThat(ex.getMessage()).isEqualTo("lookup failed");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void testAllExceptionsExtendRuntimeException() {
        assertThat(new CouponNotFoundException("x")).isInstanceOf(RuntimeException.class);
        assertThat(new CouponExhaustedException("x")).isInstanceOf(RuntimeException.class);
        assertThat(new CountryNotAllowedException("x", "y")).isInstanceOf(RuntimeException.class);
        assertThat(new CouponAlreadyUsedException("x", "y")).isInstanceOf(RuntimeException.class);
        assertThat(new CouponAlreadyExistsException("x")).isInstanceOf(RuntimeException.class);
        assertThat(new GeoLocationException("x")).isInstanceOf(RuntimeException.class);
    }
}
