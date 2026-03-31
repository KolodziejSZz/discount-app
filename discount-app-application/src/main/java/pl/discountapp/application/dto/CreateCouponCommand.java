package pl.discountapp.application.dto;

public record CreateCouponCommand(String code, int maxUses, String country) {
}
