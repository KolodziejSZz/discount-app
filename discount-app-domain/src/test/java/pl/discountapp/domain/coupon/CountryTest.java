package pl.discountapp.domain.coupon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountryTest {

    @Test
    void testCreateValidCountry() {
        // given / when
        var country = Country.of("pl");

        // then
        assertThat(country.value()).isEqualTo("PL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"P", "POL", "123", "p1", ""})
    void testThrowWhenInvalidFormat(String input) {
        assertThatThrownBy(() -> Country.of(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testThrowWhenNull() {
        assertThatThrownBy(() -> Country.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PL", "DE", "US", "GB"})
    void testCreateValidIsoCodes(String iso) {
        // when
        var country = Country.of(iso);

        // then
        assertThat(country.value()).isEqualTo(iso);
    }

    @Test
    void testNormalizeLowercaseToUppercase() {
        // when
        var country = Country.of("de");

        // then
        assertThat(country.value()).isEqualTo("DE");
    }

    @Test
    void testBeEqualForSameCode() {
        // given
        var c1 = Country.of("PL");
        var c2 = Country.of("pl");

        // then
        assertThat(c1).isEqualTo(c2);
    }

    @Test
    void testNotBeEqualForDifferentCodes() {
        assertThat(Country.of("PL")).isNotEqualTo(Country.of("DE"));
    }

    @Test
    void testThrowWithMeaningfulMessageForInvalidCode() {
        assertThatThrownBy(() -> Country.of("XYZ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1 alpha-2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1A", "A1", "!!", "  "})
    void testThrowForNonAlphaCodes(String input) {
        assertThatThrownBy(() -> Country.of(input))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
