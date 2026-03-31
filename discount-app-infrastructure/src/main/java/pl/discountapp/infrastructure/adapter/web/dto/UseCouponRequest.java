package pl.discountapp.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UseCouponRequest(
        @NotBlank String userId
) {
}
