## Suggestion resolution slice

- Added transactional pricing and reorder suggestion approval/rejection services and HTTP endpoints.
- Added optimistic product-version and stale-price conflict handling with HTTP 409 responses.
- Added superseding of competing pending suggestions, product status re-derivation, and non-recursive reorder-acceptance event publication.
- Added focused service and controller coverage.

## Manual suggestion API slice

- Added product-scoped manual pricing and reorder suggestion endpoints.
- Reused `CommerceAdvisor` active-key deduplication and recommendation calculations.
- Marked manual suggestions with `SuggestionSource.MANUAL` and moved products with pending manual pricing suggestions to `PRICE_REVIEW_PENDING`.
- Added focused manual suggestion service coverage and updated product controller wiring.
- Fixed the manual suggestion test/build failure by adding the missing `SuggestionSource.MANUAL` enum value.

## Async signal threshold slice

- Updated asynchronous demand-spike evaluation to compare the event velocity against a configurable peer-average multiplier and minimum velocity threshold.
- Preserved independent inventory-low and demand-spike evaluation so both trigger types can be generated for one mutation.
- Added default signal configuration: multiplier `2.0` and minimum velocity `1`.
- Added focused coverage for demand-spike threshold and non-spike behavior.

## AI foundation slice

- Added environment-backed LLM configuration and a `LLMGateway` contract with an HTTP implementation for OpenAI-compatible chat completions.
- Added pure prompt construction containing product state, peer ratio, days of cover, trigger context, and JSON-only output instructions.
- Added strict AI recommendation parsing for pricing and reorder responses, including fenced JSON, direction validation, confidence bounds, default lead time, and reasoning truncation.
- Focused validation passed: `AiRecommendationParserTest` and `PromptBuilderTest` (4 tests, 0 failures).
- AI strategy registration, runtime switching, price guardrails, and rule-fallback persistence remain pending for the next slice.

# StockPulse Implementation Log

This file records the implementation steps completed for the StockPulse project. It is updated after each agreed step and is separate from the source-code change log.

For the complete resumable checklist and session handoff, see `IMPLEMENTATION_CHECKLIST.md` in the workspace root.

## Project baseline

- **Backend:** Spring Boot, Spring MVC, Spring Data JPA, Bean Validation, H2, Maven
- **Frontend:** React with Vite
- **Backend port:** `8080`
- **Frontend port:** Vite default (`5173`)
- **Target Java compatibility:** Java 17
- **Current local runtime:** Java 21 is installed; Maven is configured for Java 17 source compatibility and uses the installed runtime for validation
- **Database:** H2 in-memory for the interview build

## Step 1 — Foundation alignment

### Status

Completed on 2026-10-28.

### Objective

Align the generated backend with the agreed StockPulse technology baseline before adding domain features, and record the implementation process in this document.

### Changes made

- Changed the backend parent from Spring Boot 4.1.2 to Spring Boot 3.5.6.
- Changed the Maven Java compatibility property from 27 to 17.
- Replaced initializer-specific Spring Boot 4 starter names with standard Spring Boot 3 starters:
  - Web
  - Data JPA
  - Validation
  - H2
  - Test
- Renamed the backend application namespace from `com.stockpulse.backend` to `com.stockpulse` so future packages can follow the agreed design layout.
- Configured the backend to run on port `8080`.
- Added H2 in-memory datasource and JPA development settings.
- Recorded the current frontend baseline; it is still the untouched Vite starter and its production build succeeds.

### Validation

- Frontend `npm run build`: passed on the generated Vite application.
- Frontend lint: passed with 0 warnings and 0 errors after the script was corrected to explicitly target `src`.
- Frontend production build: passed.
- Backend Maven validation: Maven reached dependency resolution but the shell runner terminated before completion; no Maven failure stack trace or Surefire report was produced. This remains pending and must be rerun locally before the next implementation step.

### Next step

Create the product domain foundation: `Category`, `ProductStatus`, `Product`, `ProductRepository`, `ProductStatusPolicy`, seed data, and the initial `GET /products` endpoint.

## Step 2 — Product domain foundation

### Status

Implemented on 2026-10-28; Maven test completion is pending local verification because dependency resolution is being interrupted by the current shell environment.

### Objective

Create the first usable backend slice: persisted products, derived product status policy, seeded demo data, filtered product listing, and frontend CORS support.

### Changes made

- Added `Category` with `ELECTRONICS`, `APPAREL`, and `HOME` values.
- Added `ProductStatus` with `ACTIVE`, `PRICE_REVIEW_PENDING`, and `OUT_OF_STOCK` values.
- Added the JPA `Product` entity with:
  - String product IDs
  - Unique SKU
  - BigDecimal money fields with scale 2
  - Stock, reorder threshold, and demand velocity
  - String-persisted enums
  - Nullable future margin/supplier fields
  - JPA optimistic-locking version
- Added `ProductRepository` with status/category filter queries.
- Added `ProductStatusPolicy` as the single derivation method for product status:
  - zero stock → `OUT_OF_STOCK`
  - pending pricing review → `PRICE_REVIEW_PENDING`
  - otherwise → `ACTIVE`
- Added `ProductResponse` so JPA entities are not exposed directly by the API.
- Added `ProductService` and `GET /products` with optional `status` and `category` filters.
- Added eight required seed products through `SeedDataConfig`.
- Added CORS support for `http://localhost:5173`.
- Added unit tests for the status policy and a web-layer test for `GET /products`.
- Added comments for the intentional PRD-003 seed state and JPA-only constructor.

### Validation

- Frontend `npm run lint`: passed with 0 warnings and 0 errors.
- Frontend `npm run build`: passed.
- Source verification: passed; no stale backend package declarations remain.
- Maven test command: Maven began dependency/plugin resolution but the shell process was terminated before completion. No compilation failure, test failure, or Surefire report was produced. Run `mvn clean test` locally from the backend directory before proceeding.

## Pre-Step 3 — Domain hardening

### Status

Implemented on 2026-10-28; full Maven test completion remains pending because the shell bridge terminated during Spring test startup.

### Changes made

- Changed `Product.version` from primitive `long` to nullable `Long` while retaining `@Version` and the non-null database column.
- Changed `Product.getVersion()` to return `Long`.
- Made `Product.setStatus(...)` package-private so status mutation remains inside `com.stockpulse.product`.
- Added `version` to `ProductResponse` for the planned optimistic-concurrency contract.
- Changed `SeedDataConfig` to derive every seed status with `ProductStatusPolicy.derive(stock, false)`.
- Preserved the PRD-003 comment explaining its intentionally ACTIVE initial state.
- Added the test-scoped Awaitility dependency without a version, using Spring Boot dependency management.
- Created `CHANGELOG.md` with the required Files/What/Why format.

### Validation

- `mvn -B -ntp clean test` compiled 11 main sources and 3 test sources successfully.
- Maven resolved the new Awaitility dependency successfully.
- Maven reached `BackendApplicationTests` Spring context startup.
- The shell bridge terminated before Maven printed a final result.
- No compilation error, test assertion failure, or Surefire report was produced.
- This is recorded as validation pending, not as a passing test suite.

### Next step

Product mutations are now implemented in the following Step 3 section; the next step is the signal/evaluator design, not mutation work.

## Step 3 — Product mutations

### Status

Implemented on 2026-10-28. Full Maven validation passed.

### Changes made

- Added validated create, stock-patch, and order request records.
- Added `POST /products`, `PATCH /products/{id}/stock`, and `POST /products/{id}/orders`.
- Added a database-backed pessimistically locked counter. Seed data initializes it to `9`, so new IDs begin at `PRD-009`.
- Added duplicate-SKU checks at service and database levels, with conflicts translated to HTTP 409.
- Added transactional stock patches and guarded atomic order updates.
- Required `expectedVersion` for stock patches and translated stale writes to HTTP 409.
- Added 400 validation, 404 missing-product, and 409 mutation error handling.
- Added `StockSignalEvent` and source metadata for customer orders, manual patches, product creation, and future reorder acceptance.
- Published events inside successful mutation transactions; no listener or evaluator was added.
- Added focused service and validation coverage for ID allocation, duplicate SKU persistence conflicts, default quantity, insufficient stock, stock patch status, missing products, and order optimistic-lock conflicts.
- Added a real Spring/H2 multithreaded order integration test covering final stock, demand velocity, successful orders, conflicts, and the absence of negative inventory or partial updates.
- Added regression coverage confirming failed order mutations do not publish stock signal events.
- Changed required numeric request fields to nullable wrappers with `@NotNull`, and added two-decimal price precision validation.
- Changed product creation to `saveAndFlush()` so database uniqueness races are translated to HTTP 409 inside the service transaction.
- Translated optimistic-lock failures during order completion to HTTP 409; the transaction rolls back the guarded stock and velocity update.

### Validation

- `mvn -B -ntp clean test`: passed.
- Tests run: 17; failures: 0; errors: 0; skipped: 0.
- Mutation-focused tests: 12 (`ProductServiceTest` 9 + `MutationValidationTest` 2 + `ProductMutationConcurrencyTest` 1); failures: 0; errors: 0.
- Main source compilation: passed.

## Signal contract — 2026-10-28

### Changes made

- Added `StockSignalType` for evaluator-derived `INVENTORY_LOW` and `DEMAND_SPIKE` categories.
- Hardened `StockSignalEvent` as a validated immutable mutation snapshot containing product ID, stock, reorder threshold, demand velocity, and source.
- Added `StockSignalSource.triggersEvaluation()` so `REORDER_ACCEPTANCE` is explicitly excluded from recursive evaluation.
- Kept mutation publication, after-commit handling, and asynchronous evaluation as separate boundaries; no listener or evaluator was added yet.

### Validation

- Focused signal and mutation tests passed: 12 tests, 0 failures, 0 errors.
- Full `mvn -B -ntp clean test` passed: 19 tests, 0 failures, 0 errors, 0 skipped.

## Suggestion domain persistence — 2026-10-28

### Changes made

- Added `PricingSuggestion` and `ReorderSuggestion` JPA entities with separate tables, typed recommendation fields, timestamps, resolution metadata, and immutable product ID references.
- Added `SuggestionStatus`, `TriggerReason`, `SuggestionSource`, and `Direction` enums.
- Added nullable unique active keys and repository pending/active-key lookups to suppress duplicate pending suggestions. Resolving a suggestion clears its active key.
- Added framework-neutral pricing and reorder DTO records.

### Validation

- Focused persistence tests passed: 5 tests, 0 failures, 0 errors.
- Verified H2 uniqueness enforcement for duplicate active keys and accepted/rejected resolution lifecycle.

## Strategy layer and persistence hardening — 2026-10-28

### Changes made

- Required nonblank active keys for newly constructed pending pricing and reorder suggestions; resolution still clears the key.
- Added missing-active-key regression tests for both suggestion types.
- Added immutable `SignalContext`, strategy interfaces, typed recommendation records, pure rule pricing/reorder strategies, and a default `StrategyRegistry`.
- Added `CommerceAdvisor` as the persistence boundary. It calculates category peer averages excluding the current product, returns existing pending suggestions by active key, and handles database uniqueness races at the service boundary.
- Kept HOLD recommendations at the current price so later approval can make no price change.

### Validation

- Added unit coverage for low-stock pricing, demand-spike pricing, HOLD behavior, HALF_UP rounding, peer exclusion, reorder quantity, lead time, confidence, and missing active keys.
- Full `mvn -B -ntp clean test` passed: 31 tests, 0 failures, 0 errors, 0 skipped.

## Asynchronous signal evaluator — 2026-10-28

### Changes made

- Added a bounded `suggestionTaskExecutor` with core size 2, maximum size 4, and queue capacity 100.
- Added an `AFTER_COMMIT` and `@Async` stock-signal listener so mutation requests do not wait for suggestion evaluation.
- Added a separate `REQUIRES_NEW` evaluation service that can create independent inventory-low and demand-spike pricing/reorder suggestions for the same event.
- Preserved the `REORDER_ACCEPTANCE` recursion guard at both listener and evaluator boundaries.

### Validation

- Focused `StockSignalEvaluationServiceTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
- Full `mvn -B -ntp clean test`: passed; 34 tests, 0 failures, 0 errors, 0 skipped.
- Duplicate-key warnings during concurrent evaluation were contained by the separate `REQUIRES_NEW` writer transaction and did not produce Hibernate session failures or uncaught async exceptions.

## AI strategy integration and validation coverage — 2026-10-28

### Changes made

- Added AI pricing and reorder strategies backed by `PromptBuilder`, `LLMGateway`, and `AiRecommendationParser`.
- Added configurable pricing guardrails and a maximum AI reorder multiplier.
- Added runtime strategy selection through `/admin/strategy`, retaining `rule` as the default.
- Added rule fallback persistence with `SuggestionSource.RULE_FALLBACK` for AI failures.
- Configured 15-second LLM connection/read timeouts and matched the supplied lowercase `product` header.
- Added gateway request/error tests, strategy endpoint tests, and AI fallback coverage.
- Moved recommendation strategy execution, including AI network calls, outside transaction boundaries; suggestion writes remain isolated in `REQUIRES_NEW` persistence transactions.
- Made combined runtime strategy updates validate both keys before applying either change.
- Added root-level suggestion route aliases while retaining `/suggestions/...` compatibility routes.
- Required the configured `product` gateway header for AI requests.

### Validation

- Test compilation passed with `mvn -B -ntp -DskipTests test-compile`.
- `mvn -B -ntp clean test` was started, but the terminal wrapper stopped capturing before Maven emitted a final result or fresh Surefire summaries; this remains pending verification.