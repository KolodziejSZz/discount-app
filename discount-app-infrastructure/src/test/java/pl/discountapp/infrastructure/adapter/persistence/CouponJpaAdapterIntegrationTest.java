package pl.discountapp.infrastructure.adapter.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.discountapp.domain.coupon.Coupon;
import pl.discountapp.domain.coupon.CouponCode;
import pl.discountapp.domain.coupon.CouponRepository;
import pl.discountapp.domain.coupon.Country;
import pl.discountapp.domain.coupon.UserId;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CouponJpaAdapter.class, CouponEntityMapperImpl.class})
@Timeout(120)
class CouponJpaAdapterIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    CouponRepository couponRepository;

    @Autowired
    CouponJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    void testSaveAndFindCouponByCode() {
        // given
        var coupon = Coupon.create(CouponCode.of("SAVE10"), 5, Country.of("PL"));

        // when
        var saved = couponRepository.save(coupon);
        var found = couponRepository.findByCode(CouponCode.of("SAVE10"));

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getCode().value()).isEqualTo("SAVE10");
        assertThat(found.get().getMaxUses()).isEqualTo(5);
        assertThat(found.get().getCountry().value()).isEqualTo("PL");
        assertThat(found.get().getCurrentUses()).isZero();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void testReturnEmptyWhenCouponNotFound() {
        // given
        var code = CouponCode.of("NOPE");

        // when
        var result = couponRepository.findByCode(code);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void testReturnTrueWhenCodeExists() {
        // given
        var coupon = Coupon.create(CouponCode.of("EXISTS"), 1, Country.of("DE"));
        couponRepository.save(coupon);

        // when
        var exists = couponRepository.existsByCode(CouponCode.of("EXISTS"));

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void testReturnFalseWhenCodeDoesNotExist() {
        // when
        var exists = couponRepository.existsByCode(CouponCode.of("GHOST"));

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void testFindByCodeCaseInsensitive() {
        // given
        var coupon = Coupon.create(CouponCode.of("UPPER"), 3, Country.of("FR"));
        couponRepository.save(coupon);

        // when
        var found = couponRepository.findByCode(CouponCode.of("upper"));

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getCode().value()).isEqualTo("UPPER");
    }

    @Test
    void testPersistCouponUsages() {
        // given
        var coupon = Coupon.create(CouponCode.of("USAGE"), 5, Country.of("PL"));
        coupon.use(UserId.of("user-1"), Country.of("PL"));
        coupon.use(UserId.of("user-2"), Country.of("PL"));

        // when
        couponRepository.save(coupon);
        var found = couponRepository.findByCode(CouponCode.of("USAGE"));

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getCurrentUses()).isEqualTo(2);
        assertThat(found.get().getUsages()).hasSize(2);
    }

    @Test
    void testUpdateCouponAfterUse() {
        // given
        var coupon = Coupon.create(CouponCode.of("UPDATE"), 10, Country.of("PL"));
        couponRepository.save(coupon);

        // when
        var loaded = couponRepository.findByCode(CouponCode.of("UPDATE")).orElseThrow();
        loaded.use(UserId.of("user-x"), Country.of("PL"));
        couponRepository.save(loaded);

        // then
        var reloaded = couponRepository.findByCode(CouponCode.of("UPDATE")).orElseThrow();
        assertThat(reloaded.getCurrentUses()).isEqualTo(1);
        assertThat(reloaded.getUsages()).hasSize(1);
        assertThat(reloaded.getVersion()).isGreaterThan(0);
    }

    @Test
    void testPreserveVersionForOptimisticLocking() {
        // given
        var coupon = Coupon.create(CouponCode.of("VERSION"), 5, Country.of("US"));

        // when
        var saved = couponRepository.save(coupon);

        // then
        assertThat(saved.getVersion()).isNotNegative();
        var found = couponRepository.findByCode(CouponCode.of("VERSION")).orElseThrow();
        assertThat(found.getVersion()).isEqualTo(saved.getVersion());
    }
}
