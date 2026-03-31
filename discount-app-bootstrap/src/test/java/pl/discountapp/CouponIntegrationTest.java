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
import pl.discountapp.infrastructure.adapter.web.dto.CreateCouponRequest;
import pl.discountapp.infrastructure.adapter.web.dto.UseCouponRequest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Timeout(120)
class CouponIntegrationTest {

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
    void testCreateAndUseCoupon() throws Exception {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("PL"));
        var createReq = new CreateCouponRequest("INTTEST", 5, "PL");

        // when - create
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("INTTEST"))
                .andExpect(jsonPath("$.currentUses").value(0));

        // when - use
        mockMvc.perform(post("/api/coupons/INTTEST/usages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UseCouponRequest("user1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentUses").value(1));
    }

    @Test
    void testHandleConcurrentUsage() throws Exception {
        // given
        given(geoLocationPort.resolveCountry(anyString())).willReturn(Country.of("PL"));
        var createReq = new CreateCouponRequest("CONCURRENT", 3, "PL");
        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        var threads = 5;
        var latch = new CountDownLatch(1);
        var successCount = new AtomicInteger(0);
        var failCount = new AtomicInteger(0);

        // when
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            var userId = "concurrent-user-" + i;
            executor.submit(() -> {
                try {
                    latch.await();
                    var result = mockMvc.perform(post("/api/coupons/CONCURRENT/usages")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"userId\": \"" + userId + "\"}"))
                            .andReturn();
                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }
        latch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(successCount.get()).isEqualTo(3);
        assertThat(failCount.get()).isEqualTo(2);
    }
}
