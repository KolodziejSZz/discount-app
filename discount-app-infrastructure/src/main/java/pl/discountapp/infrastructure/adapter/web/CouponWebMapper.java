package pl.discountapp.infrastructure.adapter.web;

import org.mapstruct.Mapper;
import pl.discountapp.application.dto.CouponResult;
import pl.discountapp.application.dto.CreateCouponCommand;
import pl.discountapp.infrastructure.adapter.web.dto.CouponResponse;
import pl.discountapp.infrastructure.adapter.web.dto.CreateCouponRequest;

@Mapper(componentModel = "spring")
public interface CouponWebMapper {

    CreateCouponCommand toCommand(CreateCouponRequest request);

    CouponResponse toResponse(CouponResult result);
}
