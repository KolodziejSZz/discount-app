# discount-app

## What it does

REST API for managing discount coupons with geolocation-based country validation.

- Create coupons with a unique code, usage limit, and country restriction (ISO 3166-1 alpha-2)
- Redeem coupons — resolves the client IP to a country via ip-api.com, enforces one-use-per-user, validates country match
- Rate limited at 100 requests/min per IP (Bucket4j token bucket)
- Optimistic locking with automatic retry (up to 3 attempts, 50 ms backoff) for concurrent redemptions
- Geolocation results cached in Caffeine (10 000 entries, 1 h TTL)

**Architecture:** hexagonal (ports & adapters). `discount-app-domain` and `discount-app-application` are framework-free; Spring lives in `discount-app-infrastructure` and `discount-app-bootstrap` only. Flyway manages the PostgreSQL schema.

**Stack:** Java 21 · Spring Boot 3.5.13 · PostgreSQL 16 · Flyway 10 · Caffeine · Bucket4j · MapStruct · Micrometer / Prometheus

---

## Prerequisites

- Docker 24+ and Docker Compose v2
- Java 21 and Maven 3.9+ (for building or running tests locally)

---

## Build

```bash
# full build, runs all tests
mvn clean package

# skip tests for a faster image build
mvn clean package -DskipTests
```

Output JAR: `discount-app-bootstrap/target/discount-app-1.0.0-SNAPSHOT.jar`

---

## Run

```bash
docker-compose up --build
```

Brings up:

| Service    | Image               | Port |
|------------|---------------------|------|
| `postgres` | postgres:16-alpine  | 5432 |
| `app`      | built from Dockerfile | 8080 |

The app waits for the Postgres healthcheck, then Flyway runs `V1__create_coupon_tables.sql` which creates the `coupons` and `coupon_usages` tables.

Follow logs:

```bash
docker-compose logs -f app
```

---

## Test

```bash
# unit + integration tests (Testcontainers spins up PostgreSQL automatically)
mvn verify
```

Test layers:

| Layer | Key test classes | Notes |
|-------|-----------------|-------|
| Domain | `CouponTest`, `CouponCodeTest`, `CountryTest` | Pure JUnit 5 + AssertJ, no mocks |
| Application | `CouponServiceTest` | Mockito BDD, mocked `CouponRepository` and `GeoLocationPort` |
| Infrastructure | `CouponJpaAdapterIntegrationTest`, `CouponRestControllerTest` | MockMvc + Testcontainers PostgreSQL |
| Bootstrap | `CouponE2eIntegrationTest`, `ArchitectureTest` | Full-stack HTTP, ArchUnit layer verification |

JaCoCo report: `target/site/jacoco/index.html`

---

## API

Swagger UI: `http://localhost:8080/swagger-ui.html`

OpenAPI spec: `http://localhost:8080/v3/api-docs`

### Create coupon

```bash
curl -X POST http://localhost:8080/api/coupons \
  -H "Content-Type: application/json" \
  -d '{"code": "SAVE20", "maxUses": 100, "country": "PL"}'
```

Response `201 Created`:

```json
{
  "code": "SAVE20",
  "createdAt": "2026-03-31T12:00:00Z",
  "maxUses": 100,
  "currentUses": 0,
  "country": "PL"
}
```

Constraints: `code` 3–50 characters (normalized to uppercase), `maxUses` >= 1, `country` exactly 2 characters.

### Use coupon

```bash
curl -X POST http://localhost:8080/api/coupons/SAVE20/usages \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-123"}'
```

Response `200 OK`:

```json
{
  "code": "SAVE20",
  "createdAt": "2026-03-31T12:00:00Z",
  "maxUses": 100,
  "currentUses": 1,
  "country": "PL"
}
```

The client IP is resolved to a country. Private IPs (loopback, RFC 1918) are rejected with `COUNTRY_NOT_ALLOWED`.

### Error responses

All errors return `ApiError`:

```json
{
  "timestamp": "2026-03-31T12:00:00Z",
  "status": 409,
  "errorCode": "COUPON_EXHAUSTED",
  "message": "Coupon SAVE20 has reached its maximum usage limit",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

| HTTP | `errorCode` | When |
|------|------------|------|
| 400 | `VALIDATION_ERROR` | Invalid request body |
| 403 | `COUNTRY_NOT_ALLOWED` | Client IP country != coupon country |
| 404 | `COUPON_NOT_FOUND` | Unknown coupon code |
| 409 | `COUPON_ALREADY_EXISTS` | Duplicate code on create |
| 409 | `COUPON_EXHAUSTED` | Max uses reached |
| 409 | `COUPON_ALREADY_USED` | Same user already redeemed |
| 429 | `RATE_LIMIT_EXCEEDED` | Over 100 req/min from this IP |
| 502 | `GEOLOCATION_UNAVAILABLE` | ip-api.com unreachable |

### Health and metrics

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8080/actuator/metrics
```

Custom business metrics: `coupon.created`, `coupon.used`, `coupon.rejected`, `geolocation.lookup.duration`.

---

## API Examples

### Health Check

```bash
curl -s http://localhost:8080/actuator/health
# Response: {"status":"UP","groups":["liveness","readiness"],"components":{"db":{"status":"UP",...},"geoLocation":{"status":"UP",...},...}}
```

### Liveness Probe

```bash
curl -s http://localhost:8080/actuator/health/liveness
# Response: {"status":"UP"}
```

### Readiness Probe

```bash
curl -s http://localhost:8080/actuator/health/readiness
# Response: {"status":"UP"}
```

### Metrics

```bash
curl -s http://localhost:8080/actuator/metrics
# Response: {"names":["coupons_created_total","coupons_used_total","http.server.requests",...]}
```

### Prometheus Metrics

```bash
curl -s http://localhost:8080/actuator/prometheus
# Response: Prometheus text format with counters, gauges, histograms
```

### OpenAPI Spec

```bash
curl -s http://localhost:8080/v3/api-docs
# Response: OpenAPI 3.1.0 JSON spec
```

### Create Coupon

```bash
curl -s -X POST http://localhost:8080/api/coupons \
  -H "Content-Type: application/json" \
  -d '{"code": "SUMMER2024", "maxUses": 10, "country": "PL"}'
# Response: {"code":"SUMMER2024","createdAt":"2026-03-31T14:13:36.911251872Z","maxUses":10,"currentUses":0,"country":"PL"}
# HTTP: 201
```

#### Error: Duplicate coupon (409)

```bash
curl -s -X POST http://localhost:8080/api/coupons \
  -H "Content-Type: application/json" \
  -d '{"code": "SUMMER2024", "maxUses": 10, "country": "PL"}'
# Response: {"timestamp":"...","status":409,"errorCode":"COUPON_ALREADY_EXISTS","message":"Coupon already exists: SUMMER2024","correlationId":"..."}
# HTTP: 409
```

#### Error: Validation failure (400)

```bash
curl -s -X POST http://localhost:8080/api/coupons \
  -H "Content-Type: application/json" \
  -d '{"code": "", "maxUses": 0, "country": "PL"}'
# Response: {"timestamp":"...","status":400,"errorCode":"VALIDATION_ERROR","message":"code: must not be blank; maxUses: must be greater than or equal to 1; code: size must be between 3 and 50","correlationId":"..."}
# HTTP: 400
```

### Use Coupon

The caller IP is resolved via geolocation and must match the coupon country. Use `X-Forwarded-For` to pass the real client IP when behind a proxy (required when running via Docker — the container internal IP is rejected as private).

```bash
curl -s -X POST http://localhost:8080/api/coupons/SUMMER2024/usages \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 85.128.0.1" \
  -d '{"userId": "user-123"}'
# Response: {"code":"SUMMER2024","createdAt":"2026-03-31T14:13:36.911252Z","maxUses":10,"currentUses":1,"country":"PL"}
# HTTP: 200
```

#### Error: Coupon not found (404)

```bash
curl -s -X POST http://localhost:8080/api/coupons/DOESNOTEXIST/usages \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 85.128.0.1" \
  -d '{"userId": "user-123"}'
# Response: {"timestamp":"...","status":404,"errorCode":"COUPON_NOT_FOUND","message":"Coupon not found: DOESNOTEXIST","correlationId":"..."}
# HTTP: 404
```

#### Error: Country not allowed (403)

```bash
curl -s -X POST http://localhost:8080/api/coupons/PL_ONLY/usages \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 85.214.0.1" \
  -d '{"userId": "user-de"}'
# Response: {"timestamp":"...","status":403,"errorCode":"COUNTRY_NOT_ALLOWED","message":"Country not allowed. Coupon is for PL, but user is from DE","correlationId":"..."}
# HTTP: 403
```

#### Error: User already used coupon (409)

```bash
curl -s -X POST http://localhost:8080/api/coupons/SUMMER2024/usages \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 85.128.0.1" \
  -d '{"userId": "user-123"}'
# Response: {"timestamp":"...","status":409,"errorCode":"COUPON_ALREADY_USED","message":"User user-123 already used coupon SUMMER2024","correlationId":"..."}
# HTTP: 409
```

#### Error: Geolocation unavailable — private IP (502)

```bash
curl -s -X POST http://localhost:8080/api/coupons/SUMMER2024/usages \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-123"}'
# Response: {"timestamp":"...","status":502,"errorCode":"GEOLOCATION_UNAVAILABLE","message":"Cannot resolve geolocation for private/localhost IP: ...","correlationId":"..."}
# HTTP: 502
```

---

## Stop

```bash
docker-compose down -v
```
