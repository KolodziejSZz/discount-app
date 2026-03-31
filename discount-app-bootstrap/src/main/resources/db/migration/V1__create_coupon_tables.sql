CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    max_uses INT NOT NULL,
    current_uses INT NOT NULL DEFAULT 0,
    country VARCHAR(2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_coupon_code UNIQUE (code)
);

CREATE UNIQUE INDEX idx_coupon_code_lower ON coupons (LOWER(code));

CREATE TABLE coupon_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id UUID NOT NULL REFERENCES coupons(id),
    user_id VARCHAR(255) NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_coupon_user UNIQUE (coupon_id, user_id)
);

CREATE INDEX idx_coupon_usages_coupon_id ON coupon_usages (coupon_id);
