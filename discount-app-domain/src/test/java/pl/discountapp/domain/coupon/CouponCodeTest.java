package pl.discountapp.domain.coupon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponCodeTest {

    @Test
    void testNormalizeToUppercase() {
        // given / when
        var code = CouponCode.of("spring");

        // then
        assertThat(code.value()).isEqualTo("SPRING");
    }

    @Test
    void testCreateWithExactly3Characters() {
        // given / when
        var code = CouponCode.of("ABC");

        // then
        assertThat(code.value()).isEqualTo("ABC");
    }

    @Test
    void testCreateWithExactly50Characters() {
        // given
        var longCode = "A".repeat(50);

        // when
        var code = CouponCode.of(longCode);

        // then
        assertThat(code.value()).hasSize(50);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB", "A", ""})
    void testThrowWhenCodeTooShort(String input) {
        assertThatThrownBy(() -> CouponCode.of(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testThrowWhenCodeTooLong() {
        // given
        var tooLong = "A".repeat(51);

        // when / then
        assertThatThrownBy(() -> CouponCode.of(tooLong))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testThrowWhenNull() {
        assertThatThrownBy(() -> CouponCode.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testNormalizeMixedCase() {
        // when
        var code = CouponCode.of("SpRiNg2024");

        // then
        assertThat(code.value()).isEqualTo("SPRING2024");
    }

    @Test
    void testBeEqualForSameCodeDifferentCase() {
        // given
        var c1 = CouponCode.of("spring");
        var c2 = CouponCode.of("SPRING");

        // then
        assertThat(c1).isEqualTo(c2);
    }

    @Test
    void testAcceptSpecialCharacters() {
        // when
        var code = CouponCode.of("SAVE-10%");

        // then
        assertThat(code.value()).isEqualTo("SAVE-10%");
    }

    @Test
    void testAcceptNumericCode() {
        // when
        var code = CouponCode.of("123");

        // then
        assertThat(code.value()).isEqualTo("123");
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 10, 25, 50})
    void testAcceptValidLengths(int len) {
        // given
        var input = "A".repeat(len);

        // when
        var code = CouponCode.of(input);

        // then
        assertThat(code.value()).hasSize(len);
    }

    @Test
    void testThrowWithMeaningfulMessageWhenTooShort() {
        assertThatThrownBy(() -> CouponCode.of("AB"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 3 and 50");
    }
}
