package pl.discountapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.discountapp.application.port.output.GeoLocationPort;
import pl.discountapp.domain.coupon.Country;
import pl.discountapp.domain.coupon.GeoLocationException;
import pl.discountapp.infrastructure.adapter.web.dto.CreateCouponRequest;
import pl.discountapp.infrastructure.adapter.web.dto.UseCouponRequest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Timeout(120)
class CouponE2eIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    GeoLocationPort geoLocationPort;

    @Test
    void testReturn404WhenUsingNonexistentCoupon() throws Exception {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("PL"));

        // when / then
        mockMvc.perform(post("/api/coupons/DOESNOTEXIST/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("user1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COUPON_NOT_FOUND"));
    }

    @Test
    void testReturn409WhenCreatingDuplicateCoupon() throws Exception {
        // given
        var req = new CreateCouponRequest("DUPLICATE", 5, "PL");
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // when / then
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COUPON_ALREADY_EXISTS"));
    }

    @Test
    void testReturn403WhenCountryMismatch() throws Exception {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("DE"));
        var req = new CreateCouponRequest("PLONLY", 5, "PL");
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // when / then
        mockMvc.perform(post("/api/coupons/PLONLY/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("user1"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COUNTRY_NOT_ALLOWED"));
    }

    @Test
    void testReturn409WhenUserUsesCouponTwice() throws Exception {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("PL"));
        var req = new CreateCouponRequest("NOTWICE", 5, "PL");
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/coupons/NOTWICE/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("same-user"))))
                .andExpect(status().isOk());

        // when / then
        mockMvc.perform(post("/api/coupons/NOTWICE/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("same-user"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COUPON_ALREADY_USED"));
    }

    @Test
    void testReturn409WhenCouponExhausted() throws Exception {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("PL"));
        var req = new CreateCouponRequest("EXHAUST", 1, "PL");
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/coupons/EXHAUST/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("first-user"))))
                .andExpect(status().isOk());

        // when / then
        mockMvc.perform(post("/api/coupons/EXHAUST/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("second-user"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COUPON_EXHAUSTED"));
    }

    @Test
    void testReturn502WhenGeolocationFails() throws Exception {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willThrow(new GeoLocationException("Service down"));
        var req = new CreateCouponRequest("GEOFAIL", 5, "PL");
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // when / then
        mockMvc.perform(post("/api/coupons/GEOFAIL/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("user1"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("GEOLOCATION_UNAVAILABLE"));
    }

    @Test
    void testReturn400ForInvalidCreateRequest() throws Exception {
        // given
        var badReq = """
                {"code": "", "maxUses": 0, "country": ""}
                """;

        // when / then
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badReq))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void testTrackCurrentUsesAcrossMultipleUsers() throws Exception {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("PL"));
        var req = new CreateCouponRequest("MULTI", 10, "PL");
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // when
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/coupons/MULTI/usages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UseCouponRequest("user-" + i))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentUses").value(i));
        }

        // then
        mockMvc.perform(post("/api/coupons/MULTI/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("user-4"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentUses").value(4))
                .andExpect(jsonPath("$.maxUses").value(10))
                .andExpect(jsonPath("$.country").value("PL"));
    }

    @Test
    void testReturn400ForMissingUserIdOnUsage() throws Exception {
        // given
        var badUsage = """
                {"userId": ""}
                """;

        // when / then
        mockMvc.perform(post("/api/coupons/ANY/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badUsage))
                .andExpect(status().isBadRequest());
    }
}
