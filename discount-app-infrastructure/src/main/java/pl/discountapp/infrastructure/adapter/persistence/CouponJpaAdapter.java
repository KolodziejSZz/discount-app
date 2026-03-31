package pl.discountapp.infrastructure.adapter.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.discountapp.domain.coupon.Coupon;
import pl.discountapp.domain.coupon.CouponCode;
import pl.discountapp.domain.coupon.CouponRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponJpaAdapter implements CouponRepository {

    private final CouponJpaRepository jpaRepository;
    private final CouponEntityMapper mapper;

    @Override
    public Optional<Coupon> findByCode(CouponCode code) {
        return jpaRepository.findByCodeIgnoreCase(code.value())
                .map(mapper::toDomain);
    }

    @Override
    public Coupon save(Coupon coupon) {
        var existing = jpaRepository.findById(coupon.getId().value());
        if (existing.isPresent()) {
            var entity = existing.get();
            entity.setCurrentUses(coupon.getCurrentUses());
            entity.setVersion(coupon.getVersion());
            var existingUserIds = entity.getUsages().stream()
                    .map(CouponUsageJpaEntity::getUserId)
                    .collect(java.util.stream.Collectors.toSet());
            coupon.getUsages().stream()
                    .filter(u -> !existingUserIds.contains(u.userId().value()))
                    .forEach(u -> {
                        var usage = CouponUsageJpaEntity.builder()
                                .coupon(entity)
                                .userId(u.userId().value())
                                .usedAt(u.usedAt())
                                .build();
                        entity.getUsages().add(usage);
                    });
            var saved = jpaRepository.save(entity);
            return mapper.toDomain(saved);
        }
        var entity = mapper.toEntity(coupon);
        entity.getUsages().forEach(u -> u.setCoupon(entity));
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByCode(CouponCode code) {
        return jpaRepository.existsByCodeIgnoreCase(code.value());
    }
}
