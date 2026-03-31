package pl.discountapp.domain.coupon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void testCreateValidUserId() {
        // when
        var userId = UserId.of("user123");

        // then
        assertThat(userId.value()).isEqualTo("user123");
    }

    @Test
    void testThrowWhenNull() {
        assertThatThrownBy(() -> UserId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    void testThrowWhenBlank(String input) {
        assertThatThrownBy(() -> UserId.of(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void testBeEqualForSameValue() {
        // given
        var id1 = UserId.of("user1");
        var id2 = UserId.of("user1");

        // then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void testNotBeEqualForDifferentValues() {
        // given
        var id1 = UserId.of("user1");
        var id2 = UserId.of("user2");

        // then
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void testPreserveOriginalValue() {
        // given
        var val = "User-With-Special_Chars.123";

        // when
        var userId = UserId.of(val);

        // then
        assertThat(userId.value()).isEqualTo(val);
    }

    @Test
    void testAcceptSingleCharacter() {
        // when
        var userId = UserId.of("a");

        // then
        assertThat(userId.value()).isEqualTo("a");
    }
}
