package pl.discountapp.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.discountapp.application.dto.CouponResult;
import pl.discountapp.application.dto.CreateCouponCommand;
import pl.discountapp.application.dto.UseCouponCommand;
import pl.discountapp.application.port.input.CreateCouponUseCase;
import pl.discountapp.application.port.input.UseCouponUseCase;
import pl.discountapp.application.service.CouponService;

@Service
@Primary
@RequiredArgsConstructor
public class TransactionalCouponService implements CreateCouponUseCase, UseCouponUseCase {

    private final CouponService couponService;

    @Override
    @Transactional
    public CouponResult create(CreateCouponCommand cmd) {
        return couponService.create(cmd);
    }

    @Override
    @Transactional
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 50))
    public CouponResult use(UseCouponCommand cmd) {
        return couponService.use(cmd);
    }
}
