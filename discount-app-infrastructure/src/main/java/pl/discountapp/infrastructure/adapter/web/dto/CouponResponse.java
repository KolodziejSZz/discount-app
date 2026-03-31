package pl.discountapp.infrastructure.adapter.web.dto;

import java.time.Instant;

public record CouponResponse(
        String code,
        Instant createdAt,
        int maxUses,
        int currentUses,
        String country
) {
}
