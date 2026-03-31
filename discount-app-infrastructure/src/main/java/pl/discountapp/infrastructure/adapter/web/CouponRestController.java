package pl.discountapp.infrastructure.adapter.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.discountapp.application.dto.UseCouponCommand;
import pl.discountapp.application.port.input.CreateCouponUseCase;
import pl.discountapp.application.port.input.UseCouponUseCase;
import pl.discountapp.infrastructure.adapter.web.dto.CouponResponse;
import pl.discountapp.infrastructure.adapter.web.dto.CreateCouponRequest;
import pl.discountapp.infrastructure.adapter.web.dto.UseCouponRequest;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons")
public class CouponRestController {

    private final CreateCouponUseCase createCouponUseCase;
    private final UseCouponUseCase useCouponUseCase;
    private final CouponWebMapper webMapper;
    private final CouponMetrics metrics;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new coupon")
    @ApiResponse(responseCode = "201", description = "Coupon created")
    public CouponResponse create(@Valid @RequestBody CreateCouponRequest request) {
        var cmd = webMapper.toCommand(request);
        var result = createCouponUseCase.create(cmd);
        metrics.incrementCreated();
        return webMapper.toResponse(result);
    }

    @PostMapping("/{code}/usages")
    @Operation(summary = "Use a coupon")
    @ApiResponse(responseCode = "200", description = "Coupon used successfully")
    public CouponResponse use(
            @PathVariable String code,
            @Valid @RequestBody UseCouponRequest request,
            HttpServletRequest httpRequest
    ) {
        var clientIp = extractClientIp(httpRequest);
        var cmd = new UseCouponCommand(code, request.userId(), clientIp);
        var result = useCouponUseCase.use(cmd);
        metrics.incrementUsed();
        return webMapper.toResponse(result);
    }

    private String extractClientIp(HttpServletRequest request) {
        var xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
