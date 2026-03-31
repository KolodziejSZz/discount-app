package pl.discountapp.domain.coupon;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class Coupon {

    private final CouponId id;
    private final CouponCode code;
    private final Instant createdAt;
    private final int maxUses;
    private int currentUses;
    private final Country country;
    @Builder.Default
    private final Set<CouponUsage> usages = new HashSet<>();
    private long version;

    public static Coupon create(CouponCode code, int maxUses, Country country) {
        if (maxUses < 1) {
            throw new IllegalArgumentException("maxUses must be >= 1");
        }
        return Coupon.builder()
                .id(CouponId.generate())
                .code(code)
                .createdAt(Instant.now())
                .maxUses(maxUses)
                .currentUses(0)
                .country(country)
                .build();
    }

    public void use(UserId userId, Country userCountry) {
        if (!country.equals(userCountry)) {
            throw new CountryNotAllowedException(country.value(), userCountry.value());
        }
        if (currentUses >= maxUses) {
            throw new CouponExhaustedException(code.value());
        }
        var alreadyUsed = usages.stream()
                .anyMatch(u -> u.userId().equals(userId));
        if (alreadyUsed) {
            throw new CouponAlreadyUsedException(userId.value(), code.value());
        }
        usages.add(CouponUsage.of(userId));
        currentUses++;
    }
}
