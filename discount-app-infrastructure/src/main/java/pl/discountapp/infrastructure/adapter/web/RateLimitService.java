package pl.discountapp.infrastructure.adapter.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RateLimitService {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public RateLimitService(@Value("${rate-limit.requests-per-minute:100}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public boolean tryConsume(String clientIp) {
        return buckets.computeIfAbsent(clientIp, this::createBucket).tryConsume(1);
    }

    private Bucket createBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(requestsPerMinute).refillGreedy(requestsPerMinute, Duration.ofMinutes(1)).build())
                .build();
    }
}
