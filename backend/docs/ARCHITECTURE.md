# Shady Backend — Architecture

Java 17 · Spring Boot 3.x · Maven · PostgreSQL 16 · Flyway · Caffeine · Spring Security (JWT + rotating refresh cookie)

Modular monolith. Each module owns its entities, repositories, services, DTOs and controllers.
Modules call each other **only through services** (never another module's repository).
Controllers accept and return DTOs only. Entities never leave the service layer.

## 1. Repository layout

```
Shady_site/
├── src/ ...                      # existing React/Vite frontend (unchanged)
└── backend/
    ├── pom.xml
    ├── Dockerfile                # multi-stage, non-root
    ├── docker-compose.yml        # app + postgres
    ├── .env.example
    ├── .gitleaks.toml / .pre-commit-config.yaml
    ├── docs/ARCHITECTURE.md
    └── src/
        ├── main/java/com/shady/landing/
        │   ├── ShadyApplication.java
        │   │
        │   ├── common/                        # shared infrastructure (no business logic)
        │   │   ├── config/                    # AppProperties (@ConfigurationProperties), ClockConfig, JacksonConfig, OpenApiConfig (@Profile("!prod"))
        │   │   ├── error/                     # GlobalExceptionHandler, ApiError, NotFoundException, ConflictException, BadRequestException
        │   │   ├── web/                       # PageResponse<T>, PageRequests (max size clamp), CacheControlSupport, ClientIpResolver
        │   │   ├── cache/                     # CacheService (interface), CaffeineCacheService, CacheNames
        │   │   ├── ratelimit/                 # RateLimiter (interface), Bucket4jRateLimiter, RateLimitExceededException
        │   │   ├── sanitize/                  # HtmlSanitizer (OWASP java-html-sanitizer allowlist)
        │   │   ├── slug/                      # SlugGenerator (from English name, uniqueness suffix)
        │   │   └── mail/                      # EmailService (interface), LoggingEmailService (no-op default)
        │   │
        │   ├── security/                      # SecurityConfig (deny-by-default chain, CORS, headers), JwtConfig (Nimbus HS256
        │   │                                  # encoder/decoder + revocation validator), AccessTokenService, CsrfConfig, CurrentAdmin,
        │   │                                  # RestAuthenticationEntryPoint / RestAccessDeniedHandler (JSON 401/403)
        │   │
        │   ├── auth/                          # admin-only authentication
        │   │   ├── AdminUser, AdminUserRepository
        │   │   ├── RefreshToken, RefreshTokenRepository
        │   │   ├── AuthService                # login, refresh (rotate + reuse detection), logout, change password
        │   │   ├── LoginAttemptService        # failed-attempt counter + lockout (persisted in DB)
        │   │   ├── AdminBootstrap             # ApplicationRunner: creates admin from env on first startup
        │   │   ├── RefreshCookieFactory
        │   │   ├── web/AuthController         # /api/v1/auth/**
        │   │   └── dto/                       # LoginRequest, TokenResponse, ChangePasswordRequest
        │   │
        │   ├── media/
        │   │   ├── Media, MediaVariant, MediaRepository
        │   │   ├── MediaService               # validate → decode → strip EXIF (re-encode) → resize → WebP → store
        │   │   ├── ImageProcessor             # interface; WebpImageProcessor impl
        │   │   ├── ImageTypeDetector          # magic-byte sniffing (JPEG/PNG/WebP/HEIC rejected unless supported)
        │   │   ├── storage/StorageService     # interface: put/delete/publicUrl
        │   │   ├── storage/LocalDiskStorageService   (@Profile dev/test)
        │   │   ├── storage/S3StorageService          (@Profile prod; AWS SDK v2, works with Cloudflare R2)
        │   │   ├── web/AdminMediaController   # POST /api/v1/admin/media (multipart), DELETE
        │   │   ├── web/LocalMediaController   # dev only: serves /media/** from disk
        │   │   └── dto/MediaResponse          # id + variant URLs (CDN base URL + key)
        │   │
        │   ├── category/
        │   │   ├── Category, CategoryRepository, CategoryService, CategoryMapper
        │   │   ├── web/PublicCategoryController, web/AdminCategoryController
        │   │   └── dto/
        │   │
        │   ├── product/
        │   │   ├── Product, ProductImage, Availability (enum), ProductRepository, ProductService, ProductMapper
        │   │   ├── web/PublicProductController, web/AdminProductController
        │   │   └── dto/
        │   │
        │   ├── event/
        │   │   ├── Event, EventImage, EventStatus, LocationType, EventRepository, EventService, EventMapper
        │   │   ├── web/PublicEventController, web/AdminEventController
        │   │   └── dto/
        │   │
        │   ├── project/                       # portfolio projects (murals, café / nursery work)
        │   │   ├── Project, ProjectImage, ProjectRepository, ProjectService, ProjectMapper
        │   │   ├── web/PublicProjectController, web/AdminProjectController
        │   │   └── dto/
        │   │
        │   ├── content/                       # hero / about / gallery / footer sections
        │   │   ├── ContentSection, ContentSectionImage, SectionKey, ContentSectionRepository, ContentService
        │   │   ├── web/PublicContentController, web/AdminContentController
        │   │   └── dto/
        │   │
        │   ├── contact/                       # WhatsApp number, Instagram username, social links
        │   │   ├── ContactSettings, SocialLink, ContactSettingsRepository, ContactService
        │   │   ├── web/PublicContactController, web/AdminContactController
        │   │   └── dto/
        │   │
        │   ├── stats/                         # anonymous contact-button click counters
        │   │   ├── ContactClickRepository (native upsert), StatsService
        │   │   ├── web/PublicClickController (POST, rate-limited), web/AdminStatsController
        │   │   └── dto/
        │   │
        │   └── audit/
        │       ├── AuditLog, AuditAction, AuditLogRepository, AuditService
        │       └── web/AdminAuditController   # read-only list (paged)
        │
        ├── main/resources/
        │   ├── application.yml, application-dev.yml, application-test.yml, application-prod.yml
        │   └── db/migration/V1__init_schema.sql
        └── test/java/com/shady/landing/
            ├── <module>/..ServiceTest          # unit tests (Mockito)
            ├── support/IntegrationTest         # @SpringBootTest + Testcontainers PostgreSQL base class
            └── it/  AdminEndpointsSecurityIT, PublicVisibilityIT, MediaUploadIT, AuthFlowIT, ...
```

## 2. API surface

Every list endpoint is paginated: `?page=0&size=20` (size capped at 50) and returns
`{ items, page, size, totalElements, totalPages }`.

### Public (no auth, GET only unless stated). Responses carry `Cache-Control: public, max-age=60, s-maxage=300`.

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/public/products?category={slug}&featured=true` | published only; featured first, then newest |
| GET | `/api/v1/public/products/{slug}` | 404 if unpublished |
| GET | `/api/v1/public/categories` | ordered by `display_order` |
| GET | `/api/v1/public/events?when=upcoming\|past` | upcoming: start asc; past: start desc; never DRAFT |
| GET | `/api/v1/public/events/{slug}` | PUBLISHED or ENDED only |
| GET | `/api/v1/public/projects?featured=true` | published only; newest project date first |
| GET | `/api/v1/public/projects/{slug}` | 404 if unpublished |
| GET | `/api/v1/public/content` | all sections (hero, about, gallery, footer) |
| GET | `/api/v1/public/contact` | `{ whatsappNumber, instagramUsername, socialLinks[] }` |
| POST | `/api/v1/public/clicks` | `{ itemType, itemId, channel }` → 204; rate-limited, no personal data |

### Auth

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/auth/login` | returns access token (body) + sets refresh cookie; rate-limited; generic error |
| POST | `/api/v1/auth/refresh` | cookie + CSRF header; rotates refresh token |
| POST | `/api/v1/auth/logout` | cookie + CSRF header; revokes token family |
| POST | `/api/v1/auth/change-password` | ADMIN; revokes all refresh tokens |

### Admin (`ROLE_ADMIN`, bearer access token)

| Resource | Endpoints |
|---|---|
| products | `GET/POST /admin/products`, `GET/PUT/DELETE /admin/products/{id}`, `PUT /admin/products/{id}/images` (ordered media ids) |
| categories | `GET/POST /admin/categories`, `GET/PUT/DELETE /admin/categories/{id}` |
| events | `GET/POST /admin/events`, `GET/PUT/DELETE /admin/events/{id}`, `PUT /admin/events/{id}/images` |
| projects | `GET/POST /admin/projects`, `GET/PUT/DELETE /admin/projects/{id}`, `PUT /admin/projects/{id}/images` |
| content | `GET /admin/content`, `PUT /admin/content/{sectionKey}` |
| contact | `GET/PUT /admin/contact` |
| media | `POST /admin/media` (multipart, rate-limited), `DELETE /admin/media/{id}` (409 if in use) |
| stats | `GET /admin/stats/top-items?from=&to=&channel=` |
| audit | `GET /admin/audit-log` |

(All admin paths are prefixed `/api/v1`.) Any write evicts the relevant cache entries.
Admin and auth responses always carry `Cache-Control: no-store`; only public GETs are cacheable.

## 3. Key design decisions

- **IDs**: `BIGINT` identity for content tables; `UUID` for media (keys are also random file names) and refresh tokens.
- **Optimistic locking**: `version` column on editable tables → `409 Conflict` if two admin tabs overwrite each other.
- **Money**: `NUMERIC(10,2)` EGP. `NULL` price on a product = "ask for price".
- **Bilingual**: paired `_ar` / `_en` columns (simple, indexable, no JSON juggling). The public API returns both languages and the frontend picks one.
- **Slugs**: generated from the English name/title (`handmade-leather-bag`, `-2` suffix on collision), and the admin can override them. Unique index.
- **Images**: the original is decoded and re-encoded, which drops EXIF/GPS. We store 3 WebP variants (`THUMB` 400px, `MEDIUM` 960px, `LARGE` 1920px, never upscaled) and **do not keep the original**. The key is `media/{uuid}/{variant}.webp`, and URLs are built from `CDN_BASE_URL`.
- **Refresh tokens**: 256-bit random value. Only its SHA-256 is stored (a slow hash isn't needed for high-entropy tokens). Each rotation stays in the same `family_id`, so reusing an already-rotated token revokes the whole family.
- **Lockout**: the counter and `locked_until` live in the DB, so they survive restarts. Bucket4j adds per-IP rate limiting in front.
- **Clicks**: aggregated per day (`item, channel, day → count`) through an upsert. No IP, user agent or timestamp per click is stored.
- **Event status**: only `DRAFT` / `PUBLISHED` are stored. `ENDED` is computed: a published event whose `COALESCE(ends_at, starts_at) < now()` is returned with `status: ENDED` and listed under `when=past`.
- **Domains & cookies**: frontend `https://shady.com`, API `https://api.shady.com` (same site). The refresh cookie is `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth` on the API host only. The CSRF cookie `XSRF-TOKEN` (`Secure; SameSite=Strict; Domain=shady.com`, readable by JS) is echoed back by the frontend in the `X-XSRF-TOKEN` header on `/auth/refresh` and `/auth/logout` (double-submit). The frontend calls the API with `credentials: 'include'`.
- **Access tokens**: HS256 JWT (15 min) issued and validated by Spring Security's resource-server support. Tokens issued before the last password change are rejected.
- **Email**: `EmailService` interface, with a logging implementation for now (ready for Brevo/Resend). It sends an alert to the admin when the account gets locked out, after commit and asynchronously.
- **Deleting**: hard delete. Image files are removed once nothing references them. The published flag is how items are hidden.
- **Audit log**: append-only and written in the same transaction as the admin change. Login attempts record the IP (a legitimate security interest, and it's the admin's own traffic plus attackers).

## 4. Open items (tracked for later steps)

- **CDN + CORS**: Cloudflare doesn't vary its cache on `Origin`. A public response cached from a request without
  `Origin` could be served to the browser without `Access-Control-Allow-Origin`. This will be solved in the
  public-endpoints step, either with an explicit ACAO on public GETs or a Cloudflare cache-key rule.
- **Rate limiting** is per instance (in-memory Bucket4j). That's fine for a single instance; move it to Redis
  if the app is scaled out.
