package pl.discountapp.application.port.output;

import pl.discountapp.domain.coupon.Country;

public interface GeoLocationPort {
    Country resolveCountry(String ipAddress);
}
