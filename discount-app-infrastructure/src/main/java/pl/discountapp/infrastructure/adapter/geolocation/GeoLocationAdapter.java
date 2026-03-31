package pl.discountapp.infrastructure.adapter.geolocation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.discountapp.application.port.output.GeoLocationPort;
import pl.discountapp.domain.coupon.Country;
import pl.discountapp.domain.coupon.GeoLocationException;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;
import java.util.Set;

@Slf4j
@Component
public class GeoLocationAdapter implements GeoLocationPort {

    private static final Set<String> PRIVATE_IPS = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1", "localhost");
    private final RestClient restClient;
    private final Timer lookupTimer;

    public GeoLocationAdapter(
            @Value("${geolocation.base-url:http://ip-api.com}") String baseUrl,
            @Value("${geolocation.timeout:3s}") Duration timeout,
            MeterRegistry meterRegistry
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.lookupTimer = Timer.builder("geolocation_lookup_duration")
                .register(meterRegistry);
    }

    @Override
    @Cacheable("geolocation")
    public Country resolveCountry(String ipAddress) {
        if (PRIVATE_IPS.contains(ipAddress) || ipAddress.startsWith("10.") || ipAddress.startsWith("172.") || ipAddress.startsWith("192.168.")) {
            throw new GeoLocationException("Cannot resolve geolocation for private/localhost IP: " + ipAddress);
        }
        return lookupTimer.record(() -> doResolve(ipAddress));
    }

    private Country doResolve(String ipAddress) {
        try {
            var resp = restClient.get()
                    .uri("/json/{ip}?fields=status,countryCode", ipAddress)
                    .retrieve()
                    .body(GeoLocationResponse.class);

            if (resp == null || !"success".equals(resp.status())) {
                throw new GeoLocationException("Failed to resolve geolocation for IP: " + ipAddress);
            }
            return Country.of(resp.countryCode());
        } catch (GeoLocationException e) {
            throw e;
        } catch (Exception e) {
            throw new GeoLocationException("Geolocation service unavailable", e);
        }
    }
}
