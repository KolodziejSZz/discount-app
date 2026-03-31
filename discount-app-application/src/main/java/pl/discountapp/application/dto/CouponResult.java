package pl.discountapp.application.dto;

import java.time.Instant;

public record CouponResult(String code, Instant createdAt, int maxUses, int currentUses, String country) {
}
