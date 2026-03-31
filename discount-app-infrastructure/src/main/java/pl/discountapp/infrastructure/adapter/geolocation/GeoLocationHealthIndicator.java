package pl.discountapp.infrastructure.adapter.geolocation;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GeoLocationHealthIndicator implements HealthIndicator {

    private final RestClient restClient;

    public GeoLocationHealthIndicator() {
        this.restClient = RestClient.builder()
                .baseUrl("http://ip-api.com")
                .build();
    }

    @Override
    public Health health() {
        try {
            var resp = restClient.get()
                    .uri("/json/8.8.8.8?fields=status")
                    .retrieve()
                    .body(String.class);
            if (resp != null && resp.contains("success")) {
                return Health.up().withDetail("service", "ip-api.com").build();
            }
            return Health.down().withDetail("service", "ip-api.com").build();
        } catch (Exception e) {
            return Health.down().withDetail("service", "ip-api.com").withException(e).build();
        }
    }
}
