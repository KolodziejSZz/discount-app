package pl.discountapp.domain.coupon;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponIdTest {

    @Test
    void testGenerateUniqueIds() {
        // given
        var id1 = CouponId.generate();
        var id2 = CouponId.generate();

        // then
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1.value()).isNotEqualTo(id2.value());
    }

    @Test
    void testCreateFromExistingUuid() {
        // given
        var uuid = UUID.randomUUID();

        // when
        var id = CouponId.of(uuid);

        // then
        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void testThrowWhenNullUuid() {
        assertThatThrownBy(() -> new CouponId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testThrowWhenOfNullUuid() {
        assertThatThrownBy(() -> CouponId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testBeEqualForSameUuid() {
        // given
        var uuid = UUID.randomUUID();

        // when
        var id1 = CouponId.of(uuid);
        var id2 = CouponId.of(uuid);

        // then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void testHaveNonNullValueWhenGenerated() {
        // when
        var id = CouponId.generate();

        // then
        assertThat(id.value()).isNotNull();
    }
}
