package pl.discountapp.domain.coupon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    @Test
    void testCreateCouponWithValidData() {
        // given
        var code = CouponCode.of("SPRING2024");
        var country = Country.of("PL");

        // when
        var coupon = Coupon.create(code, 10, country);

        // then
        assertThat(coupon.getId()).isNotNull();
        assertThat(coupon.getCode()).isEqualTo(code);
        assertThat(coupon.getMaxUses()).isEqualTo(10);
        assertThat(coupon.getCurrentUses()).isZero();
        assertThat(coupon.getCountry()).isEqualTo(country);
        assertThat(coupon.getCreatedAt()).isNotNull();
        assertThat(coupon.getUsages()).isEmpty();
    }

    @Test
    void testThrowWhenMaxUsesZero() {
        // given
        var code = CouponCode.of("BAD");
        var country = Country.of("PL");

        // when / then
        assertThatThrownBy(() -> Coupon.create(code, 0, country))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxUses must be >= 1");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
    void testThrowWhenMaxUsesNegative(int maxUses) {
        // given
        var code = CouponCode.of("NEG");
        var country = Country.of("PL");

        // when / then
        assertThatThrownBy(() -> Coupon.create(code, maxUses, country))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateCouponWithMaxUsesOne() {
        // when
        var coupon = Coupon.create(CouponCode.of("SINGLE"), 1, Country.of("PL"));

        // then
        assertThat(coupon.getMaxUses()).isEqualTo(1);
    }

    @Test
    void testIncrementUsageOnSuccessfulUse() {
        // given
        var coupon = Coupon.create(CouponCode.of("TEST"), 5, Country.of("PL"));

        // when
        coupon.use(UserId.of("user1"), Country.of("PL"));

        // then
        assertThat(coupon.getCurrentUses()).isEqualTo(1);
        assertThat(coupon.getUsages()).hasSize(1);
    }

    @Test
    void testAllowMultipleDifferentUsers() {
        // given
        var coupon = Coupon.create(CouponCode.of("MULTI"), 5, Country.of("PL"));

        // when
        coupon.use(UserId.of("user1"), Country.of("PL"));
        coupon.use(UserId.of("user2"), Country.of("PL"));
        coupon.use(UserId.of("user3"), Country.of("PL"));

        // then
        assertThat(coupon.getCurrentUses()).isEqualTo(3);
        assertThat(coupon.getUsages()).hasSize(3);
    }

    @Test
    void testThrowWhenCountryNotAllowed() {
        // given
        var coupon = Coupon.create(CouponCode.of("TEST"), 5, Country.of("PL"));

        // when / then
        assertThatThrownBy(() -> coupon.use(UserId.of("user1"), Country.of("DE")))
                .isInstanceOf(CountryNotAllowedException.class)
                .hasMessageContaining("PL")
                .hasMessageContaining("DE");
    }

    @Test
    void testThrowWhenCouponExhausted() {
        // given
        var coupon = Coupon.create(CouponCode.of("TEST"), 1, Country.of("PL"));
        coupon.use(UserId.of("user1"), Country.of("PL"));

        // when / then
        assertThatThrownBy(() -> coupon.use(UserId.of("user2"), Country.of("PL")))
                .isInstanceOf(CouponExhaustedException.class)
                .hasMessageContaining("TEST");
    }

    @Test
    void testThrowWhenAlreadyUsedBySameUser() {
        // given
        var coupon = Coupon.create(CouponCode.of("TEST"), 5, Country.of("PL"));
        coupon.use(UserId.of("user1"), Country.of("PL"));

        // when / then
        assertThatThrownBy(() -> coupon.use(UserId.of("user1"), Country.of("PL")))
                .isInstanceOf(CouponAlreadyUsedException.class)
                .hasMessageContaining("user1")
                .hasMessageContaining("TEST");
    }

    @Test
    void testCheckCountryBeforeExhaustion() {
        // given
        var coupon = Coupon.create(CouponCode.of("TEST"), 1, Country.of("PL"));
        coupon.use(UserId.of("user1"), Country.of("PL"));

        // when / then
        assertThatThrownBy(() -> coupon.use(UserId.of("user2"), Country.of("DE")))
                .isInstanceOf(CountryNotAllowedException.class);
    }

    @Test
    void testCheckCountryBeforeDuplicateUse() {
        // given
        var coupon = Coupon.create(CouponCode.of("TEST"), 5, Country.of("PL"));
        coupon.use(UserId.of("user1"), Country.of("PL"));

        // when / then
        assertThatThrownBy(() -> coupon.use(UserId.of("user1"), Country.of("DE")))
                .isInstanceOf(CountryNotAllowedException.class);
    }

    @Test
    void testExhaustCouponAtExactMaxUses() {
        // given
        var coupon = Coupon.create(CouponCode.of("EXACT"), 3, Country.of("PL"));
        coupon.use(UserId.of("u1"), Country.of("PL"));
        coupon.use(UserId.of("u2"), Country.of("PL"));
        coupon.use(UserId.of("u3"), Country.of("PL"));

        // when / then
        assertThat(coupon.getCurrentUses()).isEqualTo(3);
        assertThatThrownBy(() -> coupon.use(UserId.of("u4"), Country.of("PL")))
                .isInstanceOf(CouponExhaustedException.class);
    }

    @Test
    void testNotIncrementUsesOnFailedUse() {
        // given
        var coupon = Coupon.create(CouponCode.of("FAIL"), 5, Country.of("PL"));

        // when
        try {
            coupon.use(UserId.of("user1"), Country.of("DE"));
        } catch (CountryNotAllowedException ignored) {
        }

        // then
        assertThat(coupon.getCurrentUses()).isZero();
        assertThat(coupon.getUsages()).isEmpty();
    }

    @Test
    void testHaveVersionZeroByDefault() {
        // when
        var coupon = Coupon.create(CouponCode.of("VER"), 1, Country.of("PL"));

        // then
        assertThat(coupon.getVersion()).isZero();
    }
}
