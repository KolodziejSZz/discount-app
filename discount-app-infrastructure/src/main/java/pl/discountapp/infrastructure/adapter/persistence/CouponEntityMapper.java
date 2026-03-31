package pl.discountapp.infrastructure.adapter.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import pl.discountapp.domain.coupon.Coupon;
import pl.discountapp.domain.coupon.CouponCode;
import pl.discountapp.domain.coupon.CouponId;
import pl.discountapp.domain.coupon.CouponUsage;
import pl.discountapp.domain.coupon.Country;
import pl.discountapp.domain.coupon.UserId;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CouponEntityMapper {

    @Mapping(target = "id", expression = "java(coupon.getId().value())")
    @Mapping(target = "code", expression = "java(coupon.getCode().value())")
    @Mapping(target = "country", expression = "java(coupon.getCountry().value())")
    @Mapping(target = "usages", expression = "java(mapUsagesToEntities(coupon, coupon.getUsages()))")
    CouponJpaEntity toEntity(Coupon coupon);

    default Coupon toDomain(CouponJpaEntity entity) {
        var usages = entity.getUsages().stream()
                .map(u -> new CouponUsage(UserId.of(u.getUserId()), u.getUsedAt()))
                .collect(Collectors.toSet());

        return Coupon.builder()
                .id(CouponId.of(entity.getId()))
                .code(CouponCode.of(entity.getCode()))
                .createdAt(entity.getCreatedAt())
                .maxUses(entity.getMaxUses())
                .currentUses(entity.getCurrentUses())
                .country(Country.of(entity.getCountry()))
                .usages(usages)
                .version(entity.getVersion())
                .build();
    }

    default Set<CouponUsageJpaEntity> mapUsagesToEntities(Coupon coupon, Set<CouponUsage> usages) {
        var entity = new CouponJpaEntity();
        entity.setId(coupon.getId().value());

        return usages.stream()
                .map(u -> CouponUsageJpaEntity.builder()
                        .coupon(entity)
                        .userId(u.userId().value())
                        .usedAt(u.usedAt())
                        .build())
                .collect(Collectors.toSet());
    }
}
