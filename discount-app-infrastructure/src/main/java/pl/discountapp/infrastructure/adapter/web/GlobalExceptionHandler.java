package pl.discountapp.infrastructure.adapter.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.discountapp.domain.coupon.CouponAlreadyExistsException;
import pl.discountapp.domain.coupon.CouponAlreadyUsedException;
import pl.discountapp.domain.coupon.CouponExhaustedException;
import pl.discountapp.domain.coupon.CouponNotFoundException;
import pl.discountapp.domain.coupon.CountryNotAllowedException;
import pl.discountapp.domain.coupon.GeoLocationException;
import pl.discountapp.infrastructure.adapter.web.dto.ApiError;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final CouponMetrics metrics;

    @ExceptionHandler(CouponNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handle(CouponNotFoundException ex) {
        log.warn("Coupon not found: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ErrorCode.COUPON_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CouponExhaustedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handle(CouponExhaustedException ex) {
        log.warn("Coupon exhausted: {}", ex.getMessage());
        metrics.incrementRejected("exhausted");
        return buildError(HttpStatus.CONFLICT, ErrorCode.COUPON_EXHAUSTED, ex.getMessage());
    }

    @ExceptionHandler(CountryNotAllowedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handle(CountryNotAllowedException ex) {
        log.warn("Country not allowed: {}", ex.getMessage());
        metrics.incrementRejected("country_not_allowed");
        return buildError(HttpStatus.FORBIDDEN, ErrorCode.COUNTRY_NOT_ALLOWED, ex.getMessage());
    }

    @ExceptionHandler(CouponAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handle(CouponAlreadyUsedException ex) {
        log.warn("Coupon already used: {}", ex.getMessage());
        metrics.incrementRejected("already_used");
        return buildError(HttpStatus.CONFLICT, ErrorCode.COUPON_ALREADY_USED, ex.getMessage());
    }

    @ExceptionHandler(CouponAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handle(CouponAlreadyExistsException ex) {
        log.warn("Coupon already exists: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ErrorCode.COUPON_ALREADY_EXISTS, ex.getMessage());
    }

    @ExceptionHandler(GeoLocationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiError handle(GeoLocationException ex) {
        log.error("Geolocation error: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.BAD_GATEWAY, ErrorCode.GEOLOCATION_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handle(MethodArgumentNotValidException ex) {
        var msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.warn("Validation error: {}", msg);
        return buildError(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, msg);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handle(OptimisticLockingFailureException ex) {
        log.warn("Concurrent modification: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ErrorCode.CONCURRENT_MODIFICATION, "Concurrent modification, please retry");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleFallback(Exception ex) {
        log.error("Unexpected error", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Internal server error");
    }

    private ApiError buildError(HttpStatus status, ErrorCode errorCode, String message) {
        return new ApiError(Instant.now(), status.value(), errorCode, message, MDC.get("correlationId"));
    }
}
