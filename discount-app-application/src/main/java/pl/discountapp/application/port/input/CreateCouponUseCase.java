package pl.discountapp.application.port.input;

import pl.discountapp.application.dto.CouponResult;
import pl.discountapp.application.dto.CreateCouponCommand;

public interface CreateCouponUseCase {
    CouponResult create(CreateCouponCommand cmd);
}
