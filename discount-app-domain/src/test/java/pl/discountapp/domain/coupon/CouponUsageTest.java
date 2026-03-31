package pl.discountapp.domain.coupon;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponUsageTest {

    @Test
    void testCreateUsageWithCurrentTimestamp() {
        // given
        var before = Instant.now();
        var userId = UserId.of("user1");

        // when
        var usage = CouponUsage.of(userId);

        // then
        var after = Instant.now();
        assertThat(usage.userId()).isEqualTo(userId);
        assertThat(usage.usedAt()).isBetween(before, after);
    }

    @Test
    void testThrowWhenNullUserId() {
        assertThatThrownBy(() -> new CouponUsage(null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testThrowWhenNullUsedAt() {
        assertThatThrownBy(() -> new CouponUsage(UserId.of("user1"), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testThrowWhenOfNullUserId() {
        assertThatThrownBy(() -> CouponUsage.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testBeEqualForSameUserAndTimestamp() {
        // given
        var userId = UserId.of("user1");
        var ts = Instant.parse("2024-01-01T00:00:00Z");

        // when
        var u1 = new CouponUsage(userId, ts);
        var u2 = new CouponUsage(userId, ts);

        // then
        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }

    @Test
    void testNotBeEqualForDifferentUsers() {
        // given
        var ts = Instant.parse("2024-01-01T00:00:00Z");

        // when
        var u1 = new CouponUsage(UserId.of("user1"), ts);
        var u2 = new CouponUsage(UserId.of("user2"), ts);

        // then
        assertThat(u1).isNotEqualTo(u2);
    }
}
