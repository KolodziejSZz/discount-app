package pl.discountapp.infrastructure.adapter.web.dto;

import pl.discountapp.infrastructure.adapter.web.ErrorCode;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        ErrorCode errorCode,
        String message,
        String correlationId
) {
}
