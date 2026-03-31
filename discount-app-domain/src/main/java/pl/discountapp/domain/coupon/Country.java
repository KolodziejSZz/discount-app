package pl.discountapp.domain.coupon;

import java.util.Objects;

public record Country(String value) {

    public Country {
        Objects.requireNonNull(value);
        if (!value.matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException("Country must be ISO 3166-1 alpha-2 code: " + value);
        }
    }

    public static Country of(String value) {
        return new Country(value.toUpperCase());
    }
}
