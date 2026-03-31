package pl.discountapp.infrastructure.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.discountapp.application.dto.CouponResult;
import pl.discountapp.application.port.input.CreateCouponUseCase;
import pl.discountapp.application.port.input.UseCouponUseCase;
import pl.discountapp.domain.coupon.CouponExhaustedException;
import pl.discountapp.domain.coupon.CouponNotFoundException;
import pl.discountapp.domain.coupon.GeoLocationException;
import pl.discountapp.infrastructure.adapter.web.dto.CreateCouponRequest;
import pl.discountapp.infrastructure.adapter.web.dto.UseCouponRequest;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponRestController.class)
@Import({GlobalExceptionHandler.class, pl.discountapp.infrastructure.config.SecurityConfig.class})
class CouponRestControllerTest {

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
    void testReturn201WhenCouponCreated() throws Exception {
        // given
        var request = new CreateCouponRequest("SPRING", 10, "PL");
        var result = new CouponResult("SPRING", Instant.now(), 10, 0, "PL");
        given(rateLimitService.tryConsume(any())).willReturn(true);
        given(couponWebMapper.toCommand(any())).willReturn(new pl.discountapp.application.dto.CreateCouponCommand("SPRING", 10, "PL"));
        given(createCouponUseCase.create(any())).willReturn(result);
        given(couponWebMapper.toResponse(any())).willReturn(new pl.discountapp.infrastructure.adapter.web.dto.CouponResponse("SPRING", result.createdAt(), 10, 0, "PL"));

        // when / then
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SPRING"));
    }

    @Test
    void testReturn400WhenInvalidRequest() throws Exception {
        // given
        given(rateLimitService.tryConsume(any())).willReturn(true);
        var badRequest = """
                {"code": "", "maxUses": 0, "country": ""}
                """;

        // when / then
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testReturn404WhenCouponNotFound() throws Exception {
        // given
        given(rateLimitService.tryConsume(any())).willReturn(true);
        given(useCouponUseCase.use(any())).willThrow(new CouponNotFoundException("NOPE"));

        // when / then
        mockMvc.perform(post("/api/coupons/NOPE/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("user1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COUPON_NOT_FOUND"));
    }

    @Test
    void testReturn409WhenCouponExhausted() throws Exception {
        // given
        given(rateLimitService.tryConsume(any())).willReturn(true);
        given(useCouponUseCase.use(any())).willThrow(new CouponExhaustedException("TEST"));

        // when / then
        mockMvc.perform(post("/api/coupons/TEST/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("user1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COUPON_EXHAUSTED"));
    }

    @Test
    void testReturn502WhenGeolocationUnavailable() throws Exception {
        // given
        given(rateLimitService.tryConsume(any())).willReturn(true);
        given(useCouponUseCase.use(any())).willThrow(new GeoLocationException("Service down"));

        // when / then
        mockMvc.perform(post("/api/coupons/TEST/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("user1"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("GEOLOCATION_UNAVAILABLE"));
    }
}
