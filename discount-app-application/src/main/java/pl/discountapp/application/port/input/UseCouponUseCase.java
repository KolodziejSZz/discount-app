package pl.discountapp.application.port.input;

import pl.discountapp.application.dto.CouponResult;
import pl.discountapp.application.dto.UseCouponCommand;

public interface UseCouponUseCase {
    CouponResult use(UseCouponCommand cmd);
}
