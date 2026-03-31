package pl.discountapp.application.dto;

public record UseCouponCommand(String couponCode, String userId, String ipAddress) {
}
