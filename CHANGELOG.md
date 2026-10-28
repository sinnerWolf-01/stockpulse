### Added

- Added transactional suggestion approval/rejection APIs with pricing conflict protection, reorder stock application, superseding, and status updates.
- Added manual pricing and reorder suggestion endpoints with duplicate suppression and manual-source metadata.

## 2026-10-28

- Added pricing and reorder suggestion persistence entities, repositories, enums, DTOs, timestamps, resolution metadata, and database-backed active-key deduplication.
- Added integration coverage for pending lookup, duplicate suppression, and suggestion resolution.
- Required active keys for new pending suggestions and added pure rule pricing/reorder strategies with peer-aware `CommerceAdvisor` persistence.
- Added bounded asynchronous stock-signal evaluation after transaction commit with a separate transaction boundary and reorder-acceptance recursion protection.

# Changelog

## 2026-10-28T10:20:00Z — Pre-Step 3 domain hardening

### Files

- `backend/src/main/java/com/stockpulse/product/Product.java`
- `backend/src/main/java/com/stockpulse/product/ProductResponse.java`
- `backend/src/main/java/com/stockpulse/config/SeedDataConfig.java`
- `backend/src/test/java/com/stockpulse/product/ProductControllerTest.java`
- `backend/pom.xml`
- `CHANGELOG.md`

### What

- Changed the JPA version field to nullable `Long`.
- Restricted product status mutation to the product package.
- Derived seed statuses through `ProductStatusPolicy`.
- Exposed the optimistic-lock version in product responses.
- Added Awaitility as a test dependency.

### Why

- Assigned String IDs require nullable version semantics for reliable new-entity detection.
- Product status must remain derived rather than freely mutable.
- Seed data must follow the same status policy as future mutations.
- API clients need the version for explicit optimistic-concurrency checks.
- Future async integration tests require Awaitility.

### Validation

- Main and test sources compiled successfully with `mvn -B -ntp`.
- Awaitility dependency resolved successfully.
- Maven reached Spring test startup, but the shell bridge terminated before test completion.
- No compilation failure, assertion failure, or Surefire report was produced.

## 2026-10-28 — Step 3 product mutations

### Files

- `backend/src/main/java/com/stockpulse/product/CreateProductRequest.java`
- `backend/src/main/java/com/stockpulse/product/StockPatchRequest.java`
- `backend/src/main/java/com/stockpulse/product/OrderRequest.java`
- `backend/src/main/java/com/stockpulse/product/ProductIdCounter.java`
- `backend/src/main/java/com/stockpulse/product/ProductIdCounterRepository.java`
- `backend/src/main/java/com/stockpulse/product/StockSignalEvent.java`
- `backend/src/main/java/com/stockpulse/product/StockSignalSource.java`
- `backend/src/main/java/com/stockpulse/product/ProductConflictException.java`
- `backend/src/main/java/com/stockpulse/product/ProductNotFoundException.java`
- `backend/src/main/java/com/stockpulse/config/ApiExceptionHandler.java`
- `backend/src/main/java/com/stockpulse/product/ProductController.java`
- `backend/src/main/java/com/stockpulse/product/ProductService.java`
- `backend/src/main/java/com/stockpulse/product/ProductRepository.java`
- `backend/src/main/java/com/stockpulse/config/SeedDataConfig.java`
- `backend/src/test/java/com/stockpulse/product/ProductServiceTest.java`
- `backend/src/test/java/com/stockpulse/product/ProductMutationConcurrencyTest.java`
- `IMPLEMENTATION_CHECKLIST.md`
- `IMPLEMENTATION_LOG.md`

### What

- Implemented product creation, absolute stock patches, and customer orders.
- Added concurrency-safe `PRD-%03d` allocation and guarded atomic order updates.
- Added validation and 400/404/409 API error responses.
- Added transactional `StockSignalEvent` publication without an async consumer.

### Why

- Establishes the mutation foundation required before advisory signal evaluation.
- Prevents duplicate SKUs, stale stock overwrites, negative stock, and partial order updates.
- Keeps future event-driven pricing/reorder work decoupled from HTTP mutation requests.

### Validation

- `mvn -B -ntp clean test`: passed; 17 tests, 0 failures, 0 errors.

## 2026-10-28 — Step 3 mutation hardening feedback

### Files

- `backend/src/main/java/com/stockpulse/product/CreateProductRequest.java`
- `backend/src/main/java/com/stockpulse/product/StockPatchRequest.java`
- `backend/src/main/java/com/stockpulse/product/ProductService.java`
- `backend/src/test/java/com/stockpulse/product/ProductServiceTest.java`
- `backend/src/test/java/com/stockpulse/product/MutationValidationTest.java`
- `IMPLEMENTATION_CHECKLIST.md`
- `IMPLEMENTATION_LOG.md`

### What

- Required omitted stock and threshold fields to fail validation instead of silently becoming zero.
- Added maximum two-decimal price validation.
- Flushed product creation inside the transaction to catch duplicate-SKU races deterministically.
- Added consistent order optimistic-lock conflict handling with transactional rollback.
- Added regression tests for validation, persistence duplicate conflicts, missing products, stock patch status, and order conflicts.
- Added a real multithreaded Spring/H2 order test and rollback event-publication coverage.

### Why

- Prevents malformed requests from being accepted with Java primitive defaults.
- Ensures database uniqueness races produce the documented 409 response.
- Prevents a failed concurrent order from leaving behind its stock/velocity update.

### Validation

- `mvn -B -ntp clean test`: passed.
- Full suite: 15 tests, 0 failures, 0 errors.
- Mutation-focused coverage: 10 tests, 0 failures, 0 errors.

## 2026-10-28 — Signal contract

### Files

- `backend/src/main/java/com/stockpulse/product/StockSignalEvent.java`
- `backend/src/main/java/com/stockpulse/product/StockSignalSource.java`
- `backend/src/main/java/com/stockpulse/product/StockSignalType.java`
- `backend/src/test/java/com/stockpulse/product/ProductServiceTest.java`
- `IMPLEMENTATION_CHECKLIST.md`
- `IMPLEMENTATION_LOG.md`

### What

- Defined evaluator-derived inventory-low and demand-spike signal types.
- Validated immutable event snapshots and required product/source data.
- Explicitly excluded reorder acceptance from recursive asynchronous evaluation.
- Added contract regression tests without introducing suggestion persistence or an async listener.

### Validation

- `mvn -B -ntp clean test`: passed; 19 tests, 0 failures, 0 errors.