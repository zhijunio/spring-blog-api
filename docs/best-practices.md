# Spring Boot Best Practices

A collection of conventions and patterns for building well-structured Spring Boot applications.

## Table of Contents

1. [Technology Stack](#technology-stack)
2. [Package Structure](#package-structure)
3. [Visibility Modifiers](#visibility-modifiers)
4. [Configuration Properties](#configuration-properties)
5. [Configuration Classes](#configuration-classes)
6. [Profile-Specific YAML Files](#profile-specific-yaml-files)
7. [Caching](#caching)
8. [JPA Entities](#jpa-entities)
9. [DTOs (Data Transfer Objects)](#dtos-data-transfer-objects)
10. [Entity-to-DTO Conversion](#entity-to-dto-conversion)
11. [HTTP Request / Response Modeling](#http-request--response-modeling)
12. [Accessing the Authenticated User](#accessing-the-authenticated-user)
13. [Tests](#tests)
14. [Additional Practices](#additional-practices)

---

## Package Structure

Organize packages by **feature module**, not by layer. Each module contains its own `web` and `domain` sub-packages:

```
com.example.myapp
├── order/
│   ├── web/                  ← Controllers, request/response records
│   ├── domain/               ← Entities, repositories, services, mappers
│   │   └── model/            ← DTOs and command objects
│   └── OrderAPI.java         ← Public facade for cross-module access
├── customer/
│   ├── web/
│   ├── domain/
│   │   └── model/
│   └── CustomerAPI.java
├── notification/
├── job/
├── shared/
│   ├── persistence/          ← Shared JPA types (e.g., BaseEntity)
│   ├── security/             ← Shared security adapters
│   ├── exception/            ← Shared exception types
│   └── model/                ← Shared models (e.g., PagedResult)
└── config/                   ← Application-wide configuration
```

**Rules:**

- Group by feature, not by layer — no top-level `controller/`, `service/`, or `repository/` packages.
- Cross-module access goes through a public `*API` facade interface/class, not directly into another module's `domain`
  package.
- Shared utilities and base types live in the `shared` package.

### Controller and Service Boundary

Controllers are protocol adapters. A controller may validate input, resolve the authenticated identity, translate a web
request into a command, call a service, and build the HTTP response. It must not contain business rules or call a
repository directly.

Services own use-case orchestration and transaction boundaries. Repositories are implementation details behind services
and are never injected into controllers. Enforce this rule with ArchUnit.

---

## Visibility Modifiers

Default to **minimum necessary visibility**. Only expose what other modules or layers genuinely need.

| Component                | Class                       | Constructor     | Methods         |
|--------------------------|-----------------------------|-----------------|-----------------|
| Controller               | package-private             | package-private | package-private |
| Service                  | `public`                    | package-private | `public`        |
| Repository               | package-private (interface) | —               | —               |
| Entity                   | package-private             | protected       | `public`        |
| DTO / record             | `public` or package-private | —               | —               |
| Module API facade        | `public`                    | package-private | `public`        |
| Request/Response records | package-private             | —               | —               |
| Config/Exception handler | package-private             | —               | —               |

**Examples:**

```java
// Controller — package-private; not used outside its own web package
class OrderController {
    OrderController(OrderService orderService) { ...}

    PagedResult<OrderDto> findOrders(...) { ...}
}

// Service — public class with package-private constructor (Spring injects via DI)
@Service
public class OrderService {
    OrderService(OrderRepository repo, OrderMapper mapper) { ...}

    public PagedResult<OrderDto> findOrders(int pageNo) { ...}
}

// Module facade — public; this is the only entry point for other modules
@Component
public class OrderAPI {
    OrderAPI(OrderService orderService) { ...}

    public List<OrderDto> findOrdersCreatedBetween(LocalDate from, LocalDate to) { ...}
}

// Utility class — public with private constructor to prevent instantiation
public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static Long getCurrentUserIdOrThrow() { ...}
}
```

**Why:** Restricting visibility keeps implementation details internal and makes the module's public contract explicit.
Package-private controllers, entities, and repositories cannot be accidentally used from outside the module.

---

## Configuration Properties

Bind all custom application properties to a **`@ConfigurationProperties` record** rather than scattering `@Value`
annotations across classes. Place it at the root application package so it is visible to all modules.

```java
// ApplicationProperties.java
@ConfigurationProperties(prefix = "app")
@Validated
public record ApplicationProperties(
                @DefaultValue("10") int pageSize,
                @Valid JwtProperties jwt,
                @Valid CorsProperties cors) {

    public record JwtProperties(
            @NotBlank String issuer,
            @NotNull Long expiresInSeconds,
            @NotNull RSAPublicKey publicKey,
            @NotNull RSAPrivateKey privateKey) {
    }

    public record CorsProperties(
            @DefaultValue("/api/**") String pathPattern,
            @DefaultValue("*") String allowedOrigins,
            @DefaultValue("*") String allowedMethods) {
    }
}
```

Enable it in the main application class:

```java

@SpringBootApplication
@ConfigurationPropertiesScan
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

Inject `ApplicationProperties` as a constructor parameter wherever the properties are needed:

```java

@Service
public class OrderService {
    OrderService(OrderRepository repo, ApplicationProperties properties) { ...}

    public PagedResult<OrderDto> findOrders(int pageNo) {
        var pageable = PageRequest.of(pageNo - 1, properties.pageSize());
        ...
    }
}
```

**Rules:**

- Use `@Validated` + Jakarta Validation constraints (`@NotBlank`, `@NotNull`) on the record to fail fast at startup when
  required properties are missing.
- Use `@DefaultValue` for optional properties with sensible defaults.
- Use nested records to group related properties (jwt, cors, mail, etc.).
- Never use `@Value` for application-specific settings — use `@ConfigurationProperties` records. Reserve `@Value` only
  for simple Spring/infrastructure property references where a full record would be overkill.

Corresponding YAML configuration:

```yaml
app:
  page-size: 10
  jwt:
    issuer: MyApp
    expires-in-seconds: 604800
    public-key: classpath:certs/public.pem
    # Demo/test key only; use an external secret manager in production.
    private-key: classpath:certs/private.pem
  cors:
    path-pattern: /api/**
    allowed-origins: https://myapp.example.com
```

---

## Configuration Classes

Configuration classes live in the `config` package at the root of the application. They are **package-private** — they
configure Spring beans but are never referenced directly from other code.

**Rules:**

- Annotate with `@Configuration`. Add feature-enabling annotations (`@EnableAsync`, `@EnableJpaAuditing`, etc.) on their
  own dedicated, single-purpose config class rather than piling them onto one class.
- Keep config classes package-private. Bean methods can also be package-private — Spring discovers them through
  reflection.
- Use method parameters on `@Bean` methods for dependencies rather than field injection.
- Avoid putting unrelated beans in the same config class. Prefer one config class per concern.

```java
// config/AsyncConfig.java — single responsibility
@Configuration
@EnableAsync
class AsyncConfig {
}

// config/PersistenceConfig.java — single responsibility
@Configuration
@EnableJpaAuditing
class PersistenceConfig {
}

// config/WebSecurityConfig.java — security chain only
@Configuration
@EnableWebSecurity
class WebSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**");
        http.csrf(CsrfConfigurer::disable);
        http.sessionManagement(s -> s.sessionCreationPolicy(STATELESS));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .anyRequest().authenticated());
        http.oauth2ResourceServer(c -> c.jwt(Customizer.withDefaults()));
        http.exceptionHandling(c ->
                c.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}

// config/OpenApiConfig.java — dependencies injected via @Bean method parameters
@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI openApi(ApplicationProperties props) {
        var api = props.openApi();
        return new OpenAPI()
                .info(new Info().title(api.title()).version(api.version()));
    }
}
```

---

## Profile-Specific YAML Files

Use Spring's profile mechanism to override properties per environment without modifying the base configuration.

Keep the base `application.yml` runnable with safe demo defaults. Put only environment-specific differences in
`application-{profile}.yml`; do not copy the entire base configuration into every profile. Prefer ordinary YAML for
non-sensitive defaults and environment variables or a secret manager only for secrets and deployment-specific values.

## Caching

Use Spring Cache as the application-facing abstraction and Redis as the shared cache provider when the application may
run on more than one instance.

- Cache read-heavy, bounded data only; do not use cache as the source of truth.
- Put `@Cacheable` on service methods and invalidate related keys with `@CacheEvict` after successful writes.
- Configure a finite TTL, fixed cache names, key prefixes, and JSON serialization. Never use Java native serialization
  for shared cache data.
- Make cache invalidation transaction-aware so a rolled-back database transaction does not publish an invalidation.
- Keep cache failures observable and ensure a cache outage cannot change business correctness.
- Use Testcontainers Redis tests for cache hit, invalidation, serialization, and expiry behavior.

**File naming convention:**

```
src/main/resources/
├── application.yml                 ← base config, applies to all environments
├── application-local.yml            ← local dev overrides (verbose logging, all actuator endpoints)
└── application-prod.yml             ← production overrides (if not using env vars)

src/test/resources/
└── application-test.yml             ← test-specific overrides
```

**`application.yml`** — defaults and required structure:

```yaml
spring:
  application:
    name: my-app
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: postgres
    password: ${DB_PASSWORD:postgres} # Demo default; provide a secret in production.
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
management:
  endpoints:
    web:
      exposure:
        include: info,health
  endpoint:
    health:
      probes:
        enabled: true
```

**`application-local.yml`** — local developer overrides:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: '*'
logging:
  level:
    org.springframework.security: DEBUG
```

**`application-test.yml`** — test environment overrides:

```yaml
server:
  shutdown: immediate
logging:
  level:
    org.springframework.security: DEBUG
```

**Activating profiles:**

```bash
# IDE / local run
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Production
java -jar app.jar --spring.profiles.active=prod

# Tests — set on the base test class
@ActiveProfiles("test")
public abstract class AbstractIT { ... }
```

**Rules:**

- The base `application.yml` must work without any profile active. This demo defaults `DB_PASSWORD` to `postgres`;
  production must override it with a secret.
- Only sensitive values such as passwords use environment variables. Keep ordinary application defaults in YAML. Compose
  automatically reads the project `.env` file; do not add an `env_file` declaration just to enable interpolation.
- The `docker` profile contains container-only service hostnames (`blog-db`, `redis`, `kafka`, `mailpit`, and
  `otel-lgtm`); it does not turn those ordinary settings into environment variables.
- This demo keeps its JWT private key on the classpath for repeatable local/test execution; production must load it from
  an external secret manager.
- Never commit secrets to YAML, `.env`, source code, or container definitions.
- `local` profile is for developer convenience only — it is never active in CI or production.
- `test` profile is activated via `@ActiveProfiles("test")` on the `AbstractIT` base class; it should only contain
  overrides that make the test suite faster or more deterministic.
- Do not turn every setting into an environment variable. Use environment variables for secrets and deployment-specific
  overrides only.

---

## JPA Entities

Entities live in `{module}/domain/` and are **package-private**. They are never exposed outside their own module.

```java

@Entity
@Table(name = "orders")
class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_id_generator")
    @SequenceGenerator(name = "order_id_generator", sequenceName = "order_id_seq")
    private Long id;

    @Column(name = "reference", nullable = false, unique = true, length = 50)
    private String reference;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;  // FK stored as a plain Long — avoids cross-module @ManyToOne
}
```

**BaseEntity** — a shared mapped superclass for audit fields, used by all entities:

```java

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    protected Instant createdAt;

    @LastModifiedDate
    protected Instant updatedAt;

    @Version
    protected Long version;
}
```

**Rules:**

- Entities are only used in the `domain` layer (repository + service). Controllers never receive or return entities.
- Store cross-module foreign keys as plain `Long` fields rather than `@ManyToOne` associations to avoid unintended
  lazy-loading, N+1 queries, and module coupling.
- Enable optimistic locking via `@Version` on `BaseEntity`.

---

## DTOs (Data Transfer Objects)

DTOs are **immutable records** and live in `{module}/domain/model/`. They represent the data contract between the
service layer and its callers.

```java
// Domain DTO — returned by services and repositories
public record OrderDto(
                Long id,
                String reference,
                String status,
                Long customerId,
                String customerName,   // denormalized — avoids an extra query at the call site
                Instant createdAt,
                Instant updatedAt) {
}
```

**Rules:**

- Use records — immutable by default, no boilerplate.
- DTOs may carry denormalized fields (e.g., `customerName` alongside `customerId`) to avoid N+1 queries at the call
  site.
- DTOs must not reference JPA entities or any internal class from another module.
- Omit sensitive fields (e.g., passwords, secrets) from general-purpose DTOs. Provide a separate DTO variant if an
  internal use case requires them.

---

## Entity-to-DTO Conversion

Use one of two strategies depending on complexity. Conversion always happens in the **repository query** or **service
layer** — never in a controller.

### 1. JPQL Constructor Expression (preferred for joins)

When the DTO requires data from more than one table, construct it directly in the JPQL query:

```java
// OrderRepository
@Query("""
            select new com.example.myapp.orders.domain.model.OrderDto(
                o.id, o.reference, o.status, o.customerId, c.name, o.createdAt, o.updatedAt)
            from Order o join Customer c on o.customerId = c.id
            where o.reference = :reference
        """)
Optional<OrderDto> findByReference(@Param("reference") String reference);
```

Single query, no N+1 risk, joins resolved at the database level.

### 2. MapStruct Mapper (preferred for simple mappings)

For straightforward entity → DTO mappings without joins, use a MapStruct mapper declared as a package-private interface
in the `domain` layer:

```java

@Mapper(componentModel = "spring")
interface OrderMapper {
    OrderDto toDto(Order order);
}

// Example with field exclusion
@Mapper(componentModel = "spring")
interface CustomerMapper {
    @Mapping(target = "password", ignore = true)
    CustomerDto toDto(Customer entity);             // general use — password excluded

    CustomerDto toDtoWithPassword(Customer entity); // internal auth use only
}
```

Usage in a service:

```java
public List<OrderDto> findOrdersByCustomer(Long customerId) {
    return orderRepository.findByCustomerId(customerId)
            .stream()
            .map(orderMapper::toDto)
            .toList();
}
```

---

## HTTP Request / Response Modeling

### Request Payloads

Model incoming JSON request bodies as **package-private records** with Jakarta Validation annotations, placed in the
`web` package:

```java
// orders/web/CreateOrderRequest.java
record CreateOrderRequest(
                @NotBlank(message = "Reference is required") String reference,
                @NotNull(message = "Customer ID is required") Long customerId,
                @NotEmpty(message = "At least one item is required") List<OrderItemRequest> items) {
}
```

### Command Objects

Controllers translate request records into **command objects** before calling the service. Commands live in
`domain/model/` and contain only validated, clean domain data:

```java
// Controller
ResponseEntity<Void> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    Long userId = SecurityUtils.getCurrentUserIdOrThrow();
    var cmd = new CreateOrderCmd(request.reference(), request.customerId(), request.items(), userId);
    orderService.createOrder(cmd);
    URI location = URI.create("/api/orders/" + cmd.reference());
    return ResponseEntity.created(location).build();
}

// Command record — lives in domain/model/
public record CreateOrderCmd(String reference, Long customerId, List<OrderItemCmd> items, Long createdBy) {
}
```

**Why the two-step conversion?** Request records belong to the `web` package and carry HTTP-specific concerns
(validation annotations). Command objects belong to `domain` and carry only what the service needs — keeping the service
layer free of HTTP dependencies.

### Response Bodies

Services return domain DTOs. Controllers return them directly or wrapped in `ResponseEntity`:

```java
// Paginated list
@GetMapping("")
PagedResult<OrderDto> findOrders(@RequestParam(defaultValue = "1") int page) {
    return orderService.findOrders(page);
}

// Single resource
@GetMapping("/{reference}")
ResponseEntity<OrderDto> getOrder(@PathVariable String reference) {
    return orderService.findByReference(reference)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + reference));
}

// Creation — 201 Created with Location header, no body
@PostMapping("")
ResponseEntity<Void> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    ...
    return ResponseEntity.created(location).build();
}
```

### Error Responses

Handle all exceptions centrally via a `GlobalExceptionHandler` using Spring's RFC 7807 `ProblemDetail`:

```java

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handle(ResourceNotFoundException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(NOT_FOUND, e.getMessage());
        pd.setTitle("Resource Not Found");
        pd.setProperty("errors", List.of(e.getMessage()));
        return pd;
    }

    @ExceptionHandler(BadRequestException.class)
    ProblemDetail handle(BadRequestException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getMessage());
        pd.setTitle("Bad Request");
        pd.setProperty("errors", List.of(e.getMessage()));
        return pd;
    }
}
```

**Example error response:**

```json
{
  "type": "about:blank",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Order not found: ORD-999",
  "errors": [
    "Order not found: ORD-999"
  ]
}
```

---

## Accessing the Authenticated User

In JWT-secured APIs, extract the current user from the `SecurityContext` in a dedicated utility class rather than
repeating the extraction logic in every controller.

```java
// shared/security/SecurityUtils.java
public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static Long getCurrentUserIdOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("Access denied");
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            Long userId = jwt.getClaim("user_id");
            if (userId != null) return userId;
        }
        throw new AccessDeniedException("Access denied");
    }
}
```

Usage in a controller:

```java

@PostMapping("")
ResponseEntity<Void> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    Long userId = SecurityUtils.getCurrentUserIdOrThrow();
    var cmd = new CreateOrderCmd(request.reference(), userId);
    orderService.createOrder(cmd);
    ...
}
```

**For session-based auth**, prefer injecting the principal via a method parameter instead:

```java

@GetMapping("/me/orders")
List<OrderDto> myOrders(@AuthenticationPrincipal UserDetails user) {
    return orderService.findOrdersByUser(user.getUsername());
}
```

**Rules:**

- Extract auth-related boilerplate into a utility class or resolved argument — never duplicate it across controllers.
- Pass only the resolved user ID (or username) to the service layer. Services must not access `SecurityContextHolder`
  directly; keep security concerns in the `api` and `auth` packages.
- Services receive the user identity as part of a command object, not via a thread-local.

---

## Tests

### Test Stack

| Tool             | Purpose                                        |
|------------------|------------------------------------------------|
| JUnit 5          | Test runner                                    |
| AssertJ          | Fluent assertions                              |
| Spring Boot Test | `@SpringBootTest`, `MockMvc`, `RestTestClient` |
| Testcontainers   | Real database and service containers           |
| ArchUnit         | Architecture rule enforcement                  |
| Spring Modulith  | Module boundary validation                     |

### Base Integration Test Class

All integration tests extend a shared `AbstractIT` base class. This starts the full application context once and reuses
it across all tests:

```java

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Sql("/test-data.sql")    // reload fixture data before each test class
public abstract class AbstractIT {
    @Autowired
    protected RestTestClient restTestClient;
    @Autowired
    protected MockMvcTester mvc;
}
```

### Test Data Setup

Load test data from a SQL file via `@Sql`. The SQL file should:

1. Insert a known set of entities with **fixed IDs** so tests can reference predictable data.
2. Reset sequences to a high value so records created during tests do not collide with seed IDs.

```sql
-- Seed data with fixed IDs
insert into customer(id, name, email, created_at)
values (1, 'Alice', 'alice@example.com', now()),
       (2, 'Bob', 'bob@example.com', now());

insert into orders(id, reference, status, customer_id, created_at)
values (1, 'ORD-001', 'PLACED', 1, now()),
       (2, 'ORD-002', 'SHIPPED', 2, now());

-- Avoid ID collisions with records inserted during tests
alter sequence customer_id_seq restart with 101;
alter sequence order_id_seq restart with 101;
```

### Testcontainers Configuration

Declare containers once in a shared `@TestConfiguration` class. Use `@ServiceConnection` to wire them automatically — no
manual property overrides needed:

```java

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:18-alpine");
    }

    // Add other containers (Redis, Kafka, and LGTM) as needed
    @Bean
    @ServiceConnection
    GenericContainer<?> redis() {
        return new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);
    }

    @Bean
    @ServiceConnection
    LgtmStackContainer grafanaLgtm() {
        return new LgtmStackContainer("grafana/otel-lgtm:latest");
    }
}
```

With Spring Boot 4, a `@ServiceConnection`-annotated `LgtmStackContainer` supplies the OTLP connection details for
metrics, traces, and logs. This keeps integration tests isolated from a developer's host ports. Keep the LGTM stack in
tests that verify telemetry; disable or omit exporters in tests that do not need telemetry to reduce startup cost.

### Two Complementary Testing Styles

Write tests using both `RestTestClient` and `MockMvcTester`. Both share the same `AbstractIT` base.

**RestTestClient** — preferred for asserting response body content:

```java
class OrderControllerTests extends AbstractIT {

    @Test
    void shouldReturnOrdersWithPagination() {
        var result = restTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<PagedResult<OrderDto>>() {
                })
                .getResponseBody();

        assertThat(result.data()).hasSize(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void shouldCreateOrder() {
        restTestClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "reference": "ORD-NEW", "customerId": 1, "items": [] }
                        """)
                .exchange()
                .expectStatus().isCreated();
    }
}
```

**MockMvcTester** — preferred for asserting headers, redirects, and status codes:

```java
class OrderControllerMockMvcTests extends AbstractIT {

    @Test
    void shouldReturnLocationHeaderOnCreation() {
        mvc.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "reference": "ORD-NEW", "customerId": 1, "items": [] }
                        """)
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.CREATED)
                .redirectedUrl()
                .endsWith("/api/orders/ORD-NEW");
    }

    @Test
    void shouldReturn400WhenReferenceIsMissing() {
        mvc.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "customerId": 1 }
                        """)
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
```

### Architecture Tests

Use ArchUnit to codify package conventions and Spring Modulith to enforce module boundaries:

```java
// Enforce that controllers do not directly use repositories
class ArchitectureTests {
    @Test
    void controllersShouldNotDependOnRepositories() {
        noClasses()
                .that().haveNameMatching(".*Controller")
                .should().dependOnClassesThat().haveNameMatching(".*Repository")
                .check(importedClasses);
    }
}

// Validate module boundaries using Spring Modulith
class ModularityTests {
    @Test
    void modulesShouldBeCompliant() {
        ApplicationModules.of(MyApplication.class).verify();
    }
}
```

---

## Additional Practices

### Transactions and Persistence Boundaries

Put the transaction boundary around a complete use case in the service layer. Keep controllers, event listeners, and
repositories free of transaction orchestration.

- Use `@Transactional(readOnly = true)` for read-only use cases and `@Transactional` for state changes.
- Do not keep a database transaction open while calling slow remote services. Persist the decision first and handle
  external side effects through an after-commit event or an outbox.
- Use `flush()` only when the use case needs an early database constraint failure; it is not a substitute for a
  transaction boundary.
- Keep entities managed only inside the transaction. Return DTOs or immutable projections across the boundary.
- Use optimistic locking for concurrent updates and map an optimistic-lock conflict to a retryable or conflict response.
- Keep Flyway migrations immutable after they have been applied. Correct a deployed schema with a new migration, never
  by editing an old migration.
- The current demo migrations include the ShedLock table (`V6`), the PostgreSQL full-text search vector and GIN index
  (`V7`), and the event-delivery processing lease (`V8`).

### Pagination, Search, and Batch Processing

Bound every database read. A paginated endpoint and a batch job must have an explicit limit, stable ordering, and a
defined behavior when more data exists.

- Use Spring Data `Page<T>` inside repositories and services when the total count is required. Use `Slice<T>` when only
  `hasNext` is needed.
- Use a small API-specific `PagedResult<T>` only at a protocol boundary when the response must not expose Spring Data
  types. It should be a thin adapter around `Page<T>`, not a second pagination model with different semantics.
- Enforce a maximum page size. Use deterministic ordering such as `createdAt DESC, id DESC`; ordering only by a
  non-unique timestamp can produce duplicates or missing records between pages.
- Do not use unbounded `findAll()` in web requests, scheduled jobs, or event recovery. Use a projection plus page, keyset
  cursor, or database cursor depending on the workload.
- Prefer DTO projections for list screens and batch jobs so unused entity columns are not loaded.
- For small datasets, `LIKE` search may be adequate. For growing PostgreSQL tables, use full-text search with a
  `tsvector` column and a GIN index, and verify the query with `EXPLAIN (ANALYZE, BUFFERS)`.

### Scheduling and Distributed Execution

`@Scheduled` is local to one application instance. It does not provide a distributed lock, delivery guarantee, or
automatic retry.

- Inject a `Clock` for time-dependent logic and configure one explicit business time zone. Store timestamps as UTC
  (`Instant`) and convert to the application time zone only at API or scheduling boundaries.
- Make every scheduled job idempotent and safe to run again after a timeout or process restart.
- Page through recipients and source records; never build an unbounded in-memory batch.
- When more than one application instance may run, use ShedLock with a database-backed lock or move scheduling to a
  platform scheduler. Set a lock duration longer than the normal execution time and shorter than the operational retry
  window; this project uses a two-hour maximum lock for the weekly newsletter and a 15-minute event-processing lease.
- Expose job success, duration, skipped, and failure metrics. A failed job must remain visible instead of being logged
  and swallowed.

### Internationalization

Use Spring Boot's auto-configured `MessageSource` with `messages.properties` and locale-specific bundles such as
`messages_zh_CN.properties`. Validation annotations should reference message keys rather than embedding English text.
Resolve the current locale from `Accept-Language` and return a stable `ProblemDetail` shape for every error.

### OpenAPI Descriptions from Javadoc

Use `therapi-runtime-javadoc` at runtime and `therapi-runtime-javadoc-scribe` as a compiler annotation processor. Write
Javadoc on controller methods and DTO fields; avoid duplicating descriptions in Swagger annotations because explicit
annotation values override Javadoc values.

### Async Execution

Keep `@EnableAsync` in a dedicated configuration class and use Spring Boot's auto-configured `applicationTaskExecutor`.
Propagate only intentional context across threads, such as MDC correlation fields; pass business data in immutable event
payloads rather than relying on `ThreadLocal` or request state. Configure thread naming and graceful shutdown with
`spring.task.execution`; do not create a second executor unless a workload needs a different queue, capacity, or
isolation policy. Async work must have bounded execution time, observable failures, bounded retries, idempotency keys,
metrics, and a durable dead-letter path.

For direct parent-child executor tasks that genuinely need the authenticated principal, copy only the `Authentication`
into a new child `SecurityContext`, then restore and clear the worker context in `finally`. Do not use
`MODE_INHERITABLETHREADLOCAL` with pooled threads, and do not propagate `SecurityContext` through Kafka; cross-process
consumers must receive explicit business identifiers or use their own service identity.

An `@Async` method does not propagate its exception to the HTTP caller. Return a future when the caller must observe
completion, or handle failures in the async boundary with a durable status, metric, retry policy, and dead-letter path.
Do not retry indefinitely and do not retry non-idempotent work without a stable idempotency key.

### Kafka Messaging

Use Kafka when notification work must be decoupled from the request process. Publish only after the database transaction
commits, wait for the bounded producer acknowledgement, and use an immutable event with an idempotency key. Consumers
must have a bounded retry policy, persist processing and terminal states to a dead-letter store, and acknowledge the Kafka
record only after the consumer has completed its durable handling decision. Kafka is transport, not the source of truth.

Use the event ID as the message key when ordering and deduplication are based on that event. Put a unique database
constraint on the idempotency key and claim the delivery under a database lock before performing the side effect. A
consumer crash after an external side effect but before marking it processed can still redeliver the record, so use a
provider-level idempotency key when the provider supports one. Keep retry count, last error, processing, and terminal
status queryable; a log line alone is not a dead-letter mechanism.

Bound every retry with a maximum attempt count, exponential backoff, and a timeout. Classify permanent validation or
authorization failures separately from transient infrastructure failures. Do not hold a database transaction open while
waiting for Kafka or email delivery.

### Database Evolution and Concurrency

Use Flyway for every schema change and keep applied migrations immutable. Never automatically clean a database after a
migration failure. Treat database constraints as the final authority for uniqueness and integrity, because an `exists`
check followed by an insert is racy. Use optimistic locking for concurrent updates and test the conflict path.

Keep pagination bounded and deterministic with a stable ordering. Replace unbounded `findAll()` calls in batch jobs with
projections and cursor- or page-based reads. Validate important indexes with the actual query plan rather than adding
indexes speculatively.

For uniqueness, let database constraints be authoritative. An application-level existence check may improve the
common-case error message, but it cannot guarantee correctness under concurrency; catch constraint violations and map
them to a stable domain error.

### Security and Secrets

Keep passwords, tokens, and SMTP credentials in environment variables or a secrets manager. The classpath JWT private
key in this repository is demo/test material only; production must use an external secret manager and define key
rotation and expiration policies. Do not expose internal exception messages to clients. Restrict CORS to known origins
and expose only required Actuator endpoints. Log authentication failures without logging passwords, tokens, or
unnecessary personal data.

### Time, Serialization, and API Compatibility

Use `Instant` for persisted and cross-service timestamps. This project stores database timestamps in UTC and uses
`Asia/Shanghai` for application scheduling and presentation. Configure Jackson's time zone in one configuration class;
do not scatter competing time-zone properties across YAML, entities, and controllers.

Keep API date formats, pagination fields, error fields, and version negotiation stable. The API version header or URL
must be handled at the web boundary, while services operate on version-neutral commands and DTOs. Add a new field in a
backward-compatible way; remove or rename fields only through an explicit API version change.

### Observability and Operations

Use structured logs with correlation IDs, metrics for request latency and failures, database pool health, asynchronous
event outcomes, and scheduled-job outcomes. Separate liveness from readiness checks. Configure timeouts and bounded
retries for external services, and make scheduled jobs safe to rerun. Production containers should run as a non-root
user with explicit resource limits.

This project provides three local collection paths:

- `/actuator/prometheus` exposes Micrometer metrics for a Prometheus scraper.
- OpenTelemetry exports metrics, traces, and logs through OTLP. Set `OTEL_EXPORTER_OTLP_ENDPOINT` to the collector
  address; the application derives `/v1/metrics`, `/v1/traces`, and `/v1/logs`.
- `grafana/otel-lgtm` provides a local Grafana, Loki, Tempo, Prometheus, and OpenTelemetry Collector stack through
  `docker compose up -d`.

Do not collect the same metrics through both OTLP and Prometheus in production unless duplicate storage is intentional.
Keep `service.name` stable, propagate trace and span IDs into logs, and avoid user IDs, email addresses, request bodies,
or unbounded query values as metric labels. Actuator exposure should be restricted in production; the broad local
exposure is for development only. Alert on error rate, latency, queue lag, retry exhaustion, dead-letter growth, and
database pool exhaustion rather than only on process uptime.

### Delivery and Dependency Hygiene

CI should run `./mvnw clean verify`, architecture tests, dependency vulnerability scanning, and container image
scanning. Generate an SBOM, keep Maven and dependency versions reproducible, and use immutable image versions. A local
demo may use a moving image tag for convenience, but production and CI should pin an image tag or digest. Keep README,
OpenAPI, package diagrams, migration instructions, and operational runbooks synchronized with the code.

## Summary

| Concern                  | Decision                                                                    |
|--------------------------|-----------------------------------------------------------------------------|
| Package structure        | Feature-module packages; no top-level layer packages                        |
| Cross-module access      | Through `public *API` facade classes only                                   |
| Default visibility       | Package-private; only services and module APIs are `public`                 |
| Configuration properties | `@ConfigurationProperties` record with `@Validated`; injected as a bean     |
| Configuration classes    | Package-private; one class per concern; no `@Value` for app properties      |
| Profile YAML             | `application-{profile}.yml`; `local` for dev, `test` for tests              |
| JPA entities             | Package-private; used only inside the `domain` layer                        |
| DTOs                     | Immutable records; live in `{module}/domain/model/`                         |
| Conversion layer         | Repository (JPQL constructor) or service (MapStruct); never controller      |
| Request modeling         | Package-private records with Jakarta Validation in the `web` package        |
| Command objects          | Intermediate records; created in controller, consumed by service            |
| Response modeling        | Domain DTOs returned directly or wrapped in `ResponseEntity`/`PagedResult`  |
| Error handling           | `GlobalExceptionHandler` with RFC 7807 `ProblemDetail`                      |
| Controller boundary      | Controllers call services only; repositories remain behind services         |
| Transactions              | Service-level use-case boundaries; external side effects after commit       |
| Pagination and batches   | Bounded `Page`/`Slice`/cursor reads; no unbounded `findAll()`               |
| Database integrity       | Constraints are authoritative; application checks only improve feedback     |
| Scheduling               | Explicit `Clock` and zone; ShedLock or platform scheduler for replicas      |
| Authenticated user       | `SecurityUtils` utility class; pass user ID in command objects to service   |
| i18n                     | Spring `MessageSource` with locale-specific message bundles                 |
| OpenAPI descriptions     | Javadoc plus Therapi runtime and annotation processor                       |
| Async execution          | `@EnableAsync` plus Boot auto-configured task executor                      |
| Messaging reliability    | Bounded retries, idempotency keys, durable dead-letter state, metrics        |
| Observability            | Actuator/Prometheus plus OTLP metrics, traces, and logs                      |
| Time handling            | UTC persistence; `Asia/Shanghai` at application and presentation boundaries  |
| Test data                | `@Sql` with fixed IDs and sequence resets to avoid collision                |
| Containers               | Testcontainers with `@ServiceConnection` in a shared `TestcontainersConfig` |
| Test style               | `RestTestClient` + `MockMvcTester`; shared `AbstractIT` base class          |
