package pl.discountapp.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CouponJpaRepository extends JpaRepository<CouponJpaEntity, UUID> {

    Optional<CouponJpaEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
