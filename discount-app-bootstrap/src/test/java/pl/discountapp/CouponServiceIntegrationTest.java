package pl.discountapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.discountapp.application.dto.CreateCouponCommand;
import pl.discountapp.application.dto.UseCouponCommand;
import pl.discountapp.application.port.input.CreateCouponUseCase;
import pl.discountapp.application.port.input.UseCouponUseCase;
import pl.discountapp.application.port.output.GeoLocationPort;
import pl.discountapp.domain.coupon.CouponAlreadyExistsException;
import pl.discountapp.domain.coupon.CouponAlreadyUsedException;
import pl.discountapp.domain.coupon.CouponExhaustedException;
import pl.discountapp.domain.coupon.CouponNotFoundException;
import pl.discountapp.domain.coupon.Country;
import pl.discountapp.domain.coupon.CountryNotAllowedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Testcontainers
@Timeout(120)
class CouponServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    CreateCouponUseCase createCouponUseCase;

    @Autowired
    UseCouponUseCase useCouponUseCase;

    @MockitoBean
    GeoLocationPort geoLocationPort;

    @Test
    void testCreateCouponAndPersistIt() {
        // given
        var cmd = new CreateCouponCommand("SVC01", 5, "PL");

        // when
        var result = createCouponUseCase.create(cmd);

        // then
        assertThat(result.code()).isEqualTo("SVC01");
        assertThat(result.maxUses()).isEqualTo(5);
        assertThat(result.currentUses()).isZero();
        assertThat(result.country()).isEqualTo("PL");
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void testThrowWhenCreatingDuplicateCoupon() {
        // given
        createCouponUseCase.create(new CreateCouponCommand("SVC02", 3, "DE"));

        // when / then
        assertThatThrownBy(() -> createCouponUseCase.create(new CreateCouponCommand("SVC02", 3, "DE")))
                .isInstanceOf(CouponAlreadyExistsException.class);
    }

    @Test
    void testUseCouponAndIncrementUses() {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("PL"));
        createCouponUseCase.create(new CreateCouponCommand("SVC03", 10, "PL"));

        // when
        var result = useCouponUseCase.use(new UseCouponCommand("SVC03", "user-a", "1.2.3.4"));

        // then
        assertThat(result.currentUses()).isEqualTo(1);
        assertThat(result.code()).isEqualTo("SVC03");
    }

    @Test
    void testThrowWhenUsingNonexistentCoupon() {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("PL"));

        // when / then
        assertThatThrownBy(() -> useCouponUseCase.use(new UseCouponCommand("NOPE", "user1", "1.2.3.4")))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    void testThrowWhenCountryDoesNotMatch() {
        // given
        createCouponUseCase.create(new CreateCouponCommand("SVC04", 5, "PL"));
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("US"));

        // when / then
        assertThatThrownBy(() -> useCouponUseCase.use(new UseCouponCommand("SVC04", "user1", "1.2.3.4")))
                .isInstanceOf(CountryNotAllowedException.class);
    }

    @Test
    void testThrowWhenSameUserUsesCouponTwice() {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("PL"));
        createCouponUseCase.create(new CreateCouponCommand("SVC05", 5, "PL"));
        useCouponUseCase.use(new UseCouponCommand("SVC05", "user-dup", "1.2.3.4"));

        // when / then
        assertThatThrownBy(() -> useCouponUseCase.use(new UseCouponCommand("SVC05", "user-dup", "1.2.3.4")))
                .isInstanceOf(CouponAlreadyUsedException.class);
    }

    @Test
    void testThrowWhenCouponMaxUsesReached() {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("PL"));
        createCouponUseCase.create(new CreateCouponCommand("SVC06", 2, "PL"));
        useCouponUseCase.use(new UseCouponCommand("SVC06", "u1", "1.2.3.4"));
        useCouponUseCase.use(new UseCouponCommand("SVC06", "u2", "1.2.3.4"));

        // when / then
        assertThatThrownBy(() -> useCouponUseCase.use(new UseCouponCommand("SVC06", "u3", "1.2.3.4")))
                .isInstanceOf(CouponExhaustedException.class);
    }

    @Test
    void testTrackMultipleUsagesCorrectly() {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("PL"));
        createCouponUseCase.create(new CreateCouponCommand("SVC07", 10, "PL"));

        // when
        useCouponUseCase.use(new UseCouponCommand("SVC07", "u1", "1.2.3.4"));
        useCouponUseCase.use(new UseCouponCommand("SVC07", "u2", "1.2.3.4"));
        var result = useCouponUseCase.use(new UseCouponCommand("SVC07", "u3", "1.2.3.4"));

        // then
        assertThat(result.currentUses()).isEqualTo(3);
    }
}
