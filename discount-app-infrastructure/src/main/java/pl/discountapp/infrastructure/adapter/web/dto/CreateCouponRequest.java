package pl.discountapp.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(
        @NotBlank @Size(min = 3, max = 50) String code,
        @Min(1) int maxUses,
        @NotBlank @Size(min = 2, max = 2) String country
) {
}
