package pl.discountapp.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import pl.discountapp.application.port.output.GeoLocationPort;
import pl.discountapp.application.service.CouponService;
import pl.discountapp.domain.coupon.CouponRepository;

@Configuration
@EnableTransactionManagement
public class CouponServiceConfig {

    @Bean
    public CouponService couponService(CouponRepository couponRepository, GeoLocationPort geoLocationPort) {
        return new CouponService(couponRepository, geoLocationPort);
    }
}
