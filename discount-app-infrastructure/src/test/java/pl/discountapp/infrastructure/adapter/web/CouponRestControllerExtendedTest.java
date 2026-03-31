package pl.discountapp.infrastructure.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.discountapp.application.dto.CouponResult;
import pl.discountapp.application.dto.CreateCouponCommand;
import pl.discountapp.application.dto.UseCouponCommand;
import pl.discountapp.application.port.input.CreateCouponUseCase;
import pl.discountapp.application.port.input.UseCouponUseCase;
import pl.discountapp.domain.coupon.CouponAlreadyExistsException;
import pl.discountapp.domain.coupon.CouponAlreadyUsedException;
import pl.discountapp.domain.coupon.CountryNotAllowedException;
import pl.discountapp.infrastructure.adapter.web.dto.CouponResponse;
import pl.discountapp.infrastructure.adapter.web.dto.CreateCouponRequest;
import pl.discountapp.infrastructure.adapter.web.dto.UseCouponRequest;
import pl.discountapp.infrastructure.config.SecurityConfig;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponRestController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class CouponRestControllerExtendedTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CreateCouponUseCase createCouponUseCase;

    @MockitoBean
    UseCouponUseCase useCouponUseCase;

    @MockitoBean
    CouponWebMapper couponWebMapper;

    @MockitoBean
    RateLimitService rateLimitService;

    @MockitoBean
    CouponMetrics couponMetrics;

    @Test
    void testReturn429WhenRateLimitExceeded() throws Exception {
        // given
        given(rateLimitService.tryConsume(any())).willReturn(false);

        // when / then
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCouponRequest("TEST", 5, "PL"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testReturn409WhenCouponAlreadyExists() throws Exception {
        // given
        given(rateLimitService.tryConsume(any())).willReturn(true);
        given(couponWebMapper.toCommand(any())).willReturn(new CreateCouponCommand("DUP", 5, "PL"));
        given(createCouponUseCase.create(any())).willThrow(new CouponAlreadyExistsException("DUP"));

        // when / then
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCouponRequest("DUP", 5, "PL"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COUPON_ALREADY_EXISTS"));
    }

    @Test
    void testReturn409WhenCouponAlreadyUsedByUser() throws Exception {
        // given
        given(rateLimitService.tryConsume(any())).willReturn(true);
        given(useCouponUseCase.use(any())).willThrow(new CouponAlreadyUsedException("user1", "CODE"));

        // when / then
        mockMvc.perform(post("/api/coupons/CODE/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("user1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COUPON_ALREADY_USED"));
    }

    @Test
    void testReturn403WhenCountryNotAllowed() throws Exception {
        // given
        given(rateLimitService.tryConsume(any())).willReturn(true);
        given(useCouponUseCase.use(any())).willThrow(new CountryNotAllowedException("PL", "DE"));

        // when / then
        mockMvc.perform(post("/api/coupons/TEST/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("user1"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COUNTRY_NOT_ALLOWED"));
    }

    @Test
    void testReturnCouponResponseOnSuccessfulUsage() throws Exception {
        // given
        given(rateLimitService.tryConsume(any())).willReturn(true);
        var now = Instant.now();
        var result = new CouponResult("USED", now, 5, 1, "PL");
        given(useCouponUseCase.use(any())).willReturn(result);
        given(couponWebMapper.toResponse(any())).willReturn(new CouponResponse("USED", now, 5, 1, "PL"));

        // when / then
        mockMvc.perform(post("/api/coupons/USED/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("user1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USED"))
                .andExpect(jsonPath("$.currentUses").value(1))
                .andExpect(jsonPath("$.maxUses").value(5));
    }

    @ParameterizedTest
    @CsvSource({
            "'', 5, PL",
            "AB, 5, PL",
            "VALID, 0, PL",
            "VALID, 5, ''",
            "VALID, 5, XYZ"
    })
    void testReturn400ForInvalidCreateParams(String code, int maxUses, String country) throws Exception {
        // given
        given(rateLimitService.tryConsume(any())).willReturn(true);
        var json = String.format("{\"code\":\"%s\",\"maxUses\":%d,\"country\":\"%s\"}", code, maxUses, country);

        // when / then
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIncludeCorrelationIdInErrorResponse() throws Exception {
        // given
        given(rateLimitService.tryConsume(any())).willReturn(true);
        given(couponWebMapper.toCommand(any())).willReturn(new CreateCouponCommand("CORR", 5, "PL"));
        given(createCouponUseCase.create(any())).willThrow(new CouponAlreadyExistsException("CORR"));

        // when / then
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "test-corr-123")
                        .content(objectMapper.writeValueAsString(new CreateCouponRequest("CORR", 5, "PL"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }
}
