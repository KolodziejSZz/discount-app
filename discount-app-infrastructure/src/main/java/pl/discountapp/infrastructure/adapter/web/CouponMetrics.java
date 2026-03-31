package pl.discountapp.infrastructure.adapter.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CouponMetrics {

    private final Counter couponsCreated;
    private final Counter couponsUsed;
    private final MeterRegistry registry;

    public CouponMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.couponsCreated = Counter.builder("coupons_created_total").register(registry);
        this.couponsUsed = Counter.builder("coupons_used_total").register(registry);
    }

    public void incrementCreated() {
        couponsCreated.increment();
    }

    public void incrementUsed() {
        couponsUsed.increment();
    }

    public void incrementRejected(String reason) {
        Counter.builder("coupon_usage_rejected_total")
                .tag("reason", reason)
                .register(registry)
                .increment();
    }
}
