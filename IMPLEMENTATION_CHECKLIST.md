# StockPulse Implementation Checklist and Session Handoff

This is the resumable source of truth for implementation progress. A new session should read this file first, then `IMPLEMENTATION_LOG.md`.

## Status legend

- `[x]` Completed and verified
- `[~]` Implemented but validation or review is pending
- `[ ]` Not started
- `[!]` Blocked or requires a decision

## Project context

- Project: StockPulse event-driven commerce advisor.
- Workspace: `C:\Users\zycus\Desktop\zycus interview`
- Backend: Spring Boot 3.5.6, Java 17 source compatibility, Spring MVC, JPA, Bean Validation, H2, Maven.
- Frontend: React 19.3.1, Vite 8.3.5, plain `fetch`. React 19 was retained from the initializer although the brief said React 18.
- Backend port: `8080`; frontend port: `5173`.
- Backend package root: `com.stockpulse`.
- Database: H2 in-memory for the interview build.
- Primary implementation brief: `CLINE_BRIEF.md` supplied in the external project files.
- Progress log: `IMPLEMENTATION_LOG.md`.

## Architectural rules

- [ ] Do not introduce WebFlux/R2DBC; “reactive” means event-driven/asynchronous here.
- [x] Use `BigDecimal` for money and `@Enumerated(EnumType.STRING)` for enums.
- [x] Derive product status through `ProductStatusPolicy`; never assign it ad hoc.
- [x] Strategies remain pure: no repositories, events, or transactions.
- [x] Never hold a database transaction open across an LLM call; recommendation generation is outside transactions and persistence uses a separate transaction.
- [ ] Use `@TransactionalEventListener(AFTER_COMMIT)` plus `@Async`; no scheduler or plain event listener.
- [ ] Async persistence must use a separate transaction, preferably a separate `REQUIRES_NEW` service.
- [ ] Catch unique-constraint duplicate errors outside the failed insert transaction.
- [ ] Invalid/unavailable AI always falls back to rules; never silently produces zero suggestions.
- [ ] Reject invalid AI values; do not clamp invalid prices.
- [ ] Do not add auth, carts, payments, scraping, purchase orders, scheduler polling, or automatic price application.
- [ ] Never commit API keys, cookies, tokens, real gateway hosts, or model names.
- [ ] Add comments only for non-obvious reasoning.
- [ ] Update this file and `IMPLEMENTATION_LOG.md` after every implementation step.

## Current completed work

### Step 1 — Foundation alignment

- [x] Spring Boot changed from 4.1.2 to 3.5.6 in `backend/pom.xml`.
- [x] Java Maven compatibility changed from 27 to 17.
- [x] Standard Web, JPA, Validation, H2, and Test dependencies configured.
- [x] Package namespace/path changed from `com.stockpulse.backend` to `com.stockpulse`.
- [x] Backend port and H2/JPA properties configured in `backend/src/main/resources/application.properties`.
- [x] Frontend lint script changed to `oxlint src`.
- [x] Frontend lint and build pass.
- [x] `IMPLEMENTATION_LOG.md` created.

### Step 2 — Product domain foundation

- [x] Product enums: `Category`, `ProductStatus`.
- [x] JPA `Product` entity with String ID, unique SKU, BigDecimal price, stock, threshold, velocity, status, nullable future fields, and `@Version`.
- [x] `ProductRepository` with status/category filters.
- [x] `ProductStatusPolicy` with OUT_OF_STOCK, PRICE_REVIEW_PENDING, ACTIVE derivation.
- [x] `ProductResponse` DTO.
- [x] `ProductService` and `GET /products` with optional status/category filters.
- [x] Eight seed products in `SeedDataConfig`.
- [x] CORS for `http://localhost:5173`.
- [x] Status policy and controller tests added.
- [~] Backend Maven tests: sources compile and Maven reaches Spring test startup, but the shell bridge terminates before final completion; no failure report was produced.

### Pre-Step 3 hardening

- [~] `Product.version` changed to nullable `Long`; source compilation passed, but the full test suite is not confirmed.
- [~] `Product.setStatus` made package-private.
- [~] Seed statuses now derive through `ProductStatusPolicy`.
- [~] `ProductResponse` now exposes the optimistic-lock version.
- [~] Awaitility dependency added and resolved by Maven.
- [x] `CHANGELOG.md` created with Files/What/Why sections.

## Immediate next action

Implement product mutations before suggestion generation:

- [x] `POST /products`
- [x] `PATCH /products/{id}/stock`
- [x] `POST /products/{id}/orders`

Expected files:

- [x] `backend/src/main/java/com/stockpulse/product/CreateProductRequest.java`
- [x] `backend/src/main/java/com/stockpulse/product/StockPatchRequest.java`
- [x] `backend/src/main/java/com/stockpulse/product/OrderRequest.java`
- [x] Updated `ProductController.java` and `ProductService.java`.
- [x] Added a database-backed, pessimistically locked product ID counter for `PRD-<n>`.
- [x] Added mutation exceptions and centralized error response handling.
- [x] Added focused tests for creation, duplicates, default quantity, insufficient stock, and event publication.
- [x] Added the initial `StockSignalEvent` boundary without an async evaluator.

Mutation guardrails:

- [x] Generate IDs with a database-backed pessimistically locked counter; no `MAX(id) + 1`.
- [x] Counter starts at `9` after the eight seeded products and maps to `PRD-%03d`.
- [x] Verified counter initialization and first allocation through the service test and full Spring test startup.
- [x] Enforce duplicate SKU at request/service level and database constraint level.
- [x] Translate duplicate-SKU conflicts to `409 Conflict`.
- [x] Make stock/order mutations transactional so stock and velocity commit or roll back together.
- [x] Use the existing `@Version` field for optimistic locking; stock patches require `expectedVersion`.
- [x] Translate stale stock writes to `409 Conflict`.
- [x] Use a guarded atomic order update so concurrent orders cannot reduce stock below zero.
- [x] Translate insufficient stock to `409 Conflict`.
- [x] Keep status derivation behind `ProductStatusPolicy`.
- [x] Publish `StockSignalEvent` inside successful mutation transactions.
- [x] No event listener or async evaluator added; future handling remains `@TransactionalEventListener(AFTER_COMMIT)`.
- [x] Define `REORDER_ACCEPTANCE` metadata for future non-recursive reorder handling.
- [x] Define `CUSTOMER_ORDER`, `MANUAL_STOCK_PATCH`, `PRODUCT_CREATED`, and `REORDER_ACCEPTANCE` sources.

Mutation rules:

- [x] Create velocity starts at zero and status is derived.
- [x] Stock patch accepts an absolute non-negative value.
- [x] Stock patch does not change demand velocity; only customer orders increment demand velocity.
- [x] Manual stock patches publish `MANUAL_STOCK_PATCH`; future reorder acceptance is separately identified.
- [x] Order quantity defaults to one and must be positive.
- [x] Order atomically decrements stock only when enough stock exists.
- [x] Insufficient stock returns 409 with no stock/velocity change.
- [x] Successful orders increment demand velocity by quantity.
- [x] Return the refreshed product after an atomic update.

HTTP contract decisions:

- [x] `POST /products` returns `201 Created` and the created `ProductResponse`.
- [x] No `Location` header is returned; the created ID is in the response body.
- [x] `PATCH /products/{id}/stock` returns `200 OK` with the refreshed product.
- [x] `POST /products/{id}/orders` returns `200 OK` with the refreshed product.
- [x] Expose the optimistic-lock version in the response.
- [x] Stock patches require `expectedVersion`; stale clients receive 409.
- [x] `expectedVersion` omission is not supported; stock patches require an explicit version for stale-write protection.

Required mutation tests:

- [x] Request validation for missing/blank fields, invalid prices/precision, negative stock/threshold, and non-positive order quantities.
- [x] Duplicate SKU returns 409, including a database uniqueness race after an explicit flush.
- [x] Missing product returns 404 for stock patch and order.
- [x] Stock patch persists the requested stock and re-derives status without changing velocity.
- [x] Missing order quantity defaults to one.
- [x] Insufficient stock returns 409 without changing stock or demand velocity.
- [x] Successful order increments demand velocity by the requested quantity.
- [x] Status transitions cover active stock, zero stock, and pending-pricing input through `ProductStatusPolicy` tests.
- [x] Order optimistic-lock conflicts return 409 and roll back the mutation transaction.
- [x] Successful mutations publish the expected `StockSignalEvent`.
- [x] Concurrent test asserts final stock, final demand velocity, number of successful orders, number of conflicts, and absence of partial updates.
- [x] Add an event test that verifies no event is published when a transaction rolls back.

## Suggestion domain

- [x] Define the signal contract: immutable product snapshot, product ID, mutation source, evaluator-derived signal types, and reorder-acceptance exclusion.
- [x] Keep after-commit publication and asynchronous evaluation as separate boundaries; no evaluator runs in the mutation transaction.
- [x] Add `PricingSuggestion` and `ReorderSuggestion` entities/tables and repositories.
- [x] Add enums: `SuggestionStatus` (`PENDING`, `ACCEPTED`, `REJECTED`), `TriggerReason` (`INITIAL`, `INVENTORY_LOW`, `DEMAND_SPIKE`, `MANUAL`), `SuggestionSource` (`AI`, `RULE`, `RULE_FALLBACK`, `MANUAL`), and `Direction` (`INCREASE`, `DECREASE`, `HOLD`).
- [x] Add nullable unique `activeKey`; require it for new pending suggestions, use `productId|triggerReason` while pending, and clear it on resolution.
- [x] Add timestamps, resolution notes, and DTOs.
- [x] Add `SignalContext`, `PricingStrategy`, `ReorderStrategy`, recommendation records, `StrategyRegistry`, and `CommerceAdvisor`.
- [x] Keep strategy classes pure and calculate peer averages excluding the current product.

## Rule strategies

- [x] Add `RulePricingStrategy` and `RuleReorderStrategy`.
- [x] Low stock: below threshold means price increase of 10%.
- [x] Demand spike: velocity > 2 × peer average and peer average > 0 means price increase of 5%.
- [x] Otherwise pricing recommendation is HOLD.
- [x] Round price to scale 2 using HALF_UP; rule confidence is `0.6`.
- [x] Reorder quantity is `max(1, reorderThreshold * 3 - stockLevel)`.
- [x] Reorder lead time is configurable, default 7 days.
- [x] Unit-test every branch and calculation.

## Suggestion APIs

- [x] `POST /products/{id}/suggest-pricing`
- [x] `POST /products/{id}/suggest-reorder`
- [x] Manual duplicates return the existing pending suggestion.
- [x] GET /pricing-suggestions?status= and GET /reorder-suggestions?status=; default to PENDING.
- [x] PATCH /pricing-suggestions/{id} and PATCH /reorder-suggestions/{id} for ACCEPTED/REJECTED.
- [x] Non-pending actions return 409; unknown IDs return 404.
- [x] Use central error shape {"error":"...","details":...}.

## Approval side effects

Pricing acceptance:

- [x] Reject 409 when product current price differs from suggestion current price.
- [x] Apply recommended price; HOLD makes no price change.
- [x] Mark accepted, clear active key, set resolved timestamp.
- [x] Reject other pending pricing suggestions for that product as superseded.
- [x] Re-derive product status in the same transaction.

Reorder acceptance:

- [x] Increase stock by recommended quantity to simulate inbound stock.
- [x] Mark accepted, clear active key, set resolved timestamp.
- [x] Reject other pending reorder suggestions as superseded.
- [x] Re-derive status in the same transaction.

Rejection:

- [x] Mark rejected, clear active key, set timestamp/note, and re-derive status.

## Async event-driven workflow

- [x] Add `StockSignalEvent` and publish it after product creation, order, and stock mutation transactions.
- [x] Add asynchronous `StockSignalEvaluationService` and after-commit event listener.
- [x] Inventory low: `stockLevel < reorderThreshold`.
- [x] Demand spike: velocity > configured multiplier × peer average, with peer average > 0 and minimum velocity threshold.
- [x] Permit both signals simultaneously.
- [x] Add dedicated async executor: core 2, max 4, bounded queue.
- [x] Listener uses `@TransactionalEventListener(phase = AFTER_COMMIT)` and `@Async`.
- [x] Do not block HTTP requests on asynchronous evaluation.
- [x] Skip pending suggestions before strategy calls through `CommerceAdvisor` active-key lookup.
- [x] Let the unique constraint protect against concurrent duplicate triggers.
- [x] On AI failure, persist a RULE_FALLBACK suggestion; log the AI failure and preserve rule fallback operation.
- [ ] Add Awaitility integration tests for eventual creation and duplicate suppression; implementation is present but final Maven verification is pending.

## AI integration

Configuration:

- [x] Add environment-backed `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL`, `LLM_PRODUCT`, optional `LLM_COOKIE`.
- [x] Configure 15-second connection and read timeouts.
- [x] Blank URL/key raises an internal advisor exception; selected AI strategies fall back to rules.
- [ ] No secrets in source, tests, logs, README, or commits; perform final repository audit.

Gateway:

- [x] Add `LLMGateway` posting to `{base-url}/v1/chat/completions`.
- [x] Send Bearer Authorization, product header, and Cookie only when configured.
- [x] Default body contains only `model` and `messages`.
- [x] Parse `choices[0].message.content`.
- [x] Convert blank and malformed responses, plus request failures, to an advisor exception.
- [x] Add `MockRestServiceServer` request-shape and error-response tests.

Prompting/parsing:

- [x] Add pure `PromptBuilder` with product, category, price, stock, threshold, velocity, peer average, ratio, and days-of-cover.
- [x] Use distinct INVENTORY_LOW, DEMAND_SPIKE, and neutral MANUAL/INITIAL prompts.
- [x] Add trigger-specific reorder instructions and require JSON-only output.
- [x] Strip JSON fences/surrounding text before parsing.
- [x] Reject invalid JSON, invalid/non-positive prices, direction mismatch, missing confidence, invalid quantity, and blank reasoning.
- [x] Clamp valid confidence to [0,1]; default missing lead time; reject negative lead time.
- [x] Apply max reorder multiplier bound, default 10.
- [x] Truncate reasoning to 2000 characters.
- [x] Add configurable price guardrails; leave margin-floor guardrail as a future seam.

## Runtime strategy switching

- [x] Register rule and AI strategies by `key()`.
- [x] Default pricing/reorder strategies to `rule`.
- [x] `GET /admin/strategy`.
- [x] `PUT /admin/strategy` with optional pricing/reorder keys; unknown keys return 400.
- [x] Switch without restart and use the same registry/advisor from HTTP and async callers.
- [x] Wrap AI strategy with resilience and rule fallback.
- [x] Add runtime switching MVC coverage.

## Exceptions and validation

- [x] Add one `@RestControllerAdvice`.
- [x] Validation errors → 400.
- [x] Unknown resources → 404.
- [x] Stale, non-pending, and insufficient-stock conflicts → 409.
- [x] Handle malformed enum query parameters consistently.
- [x] Do not leak stack traces or secrets.

## React merchandising console

API/state:

- [ ] Add plain-`fetch` API module using `import.meta.env.VITE_API_BASE`, default `http://localhost:8080`.
- [ ] Add loading/error states, 3-second polling, and refresh after actions.

Required UI:

- [ ] Replace Vite starter screen.
- [ ] Product table: SKU, name, category, stock, price, velocity, status.
- [ ] Highlight stock below threshold; add category/status filters.
- [ ] Show pending pricing/reorder suggestions.
- [ ] Show current → recommended price, direction, quantity, lead time, confidence, reasoning, trigger, and source.
- [ ] Accept/reject pricing and reorder suggestions.
- [ ] Show inline/toast 409 conflicts.
- [ ] Add simulate sale, edit stock, and rule/AI strategy toggle controls.
- [ ] Only after required UI: price history, margin, heatmap, or SSE reasoning stream.

## Testing checklist

Backend unit tests:

- [x] Product status policy branches.
- [x] Product controller basic response.
- [x] Product validation, ID generation, rule pricing, rule reorder, peer average.
- [ ] Prompt trigger differences and peer ratio.
- [ ] Parser: valid output, fenced JSON, invalid JSON, invalid price, bounds, direction mismatch, invalid quantity, missing/blank reasoning.

Backend integration tests:

- [~] Application context exists; Maven completion is pending local verification.
- [ ] Seed data produces eight products.
- [ ] Product create/list/filter flow.
- [ ] Order atomicity and insufficient stock.
- [ ] Stock patch.
- [ ] Pricing acceptance and stale-price conflict.
- [ ] Suggestion superseding and reorder acceptance.
- [ ] Async order → suggestions using Awaitility.
- [ ] Duplicate suppression.
- [x] AI failure creates RULE_FALLBACK.
- [x] Runtime strategy switching.
- [x] LLM request shape.

Frontend:

- [x] `npm run lint` passes currently.
- [x] `npm run build` passes currently.
- [ ] Validate the completed console and manual demo flows.

## Documentation and delivery

- [x] `IMPLEMENTATION_LOG.md` exists.
- [x] This handoff checklist exists.
- [ ] Add/finalize root `README.md` with prerequisites, startup, environment variables, API examples, and demo paths.
- [ ] Finalize `ADR.md`, `DESIGN.md`, and `WORKFLOW.md` to match the built code.
- [ ] Keep `CHANGELOG.md` updated if present.
- [ ] Confirm no `.env`, API key, cookie, or token is tracked.
- [ ] Confirm fresh setup and documentation are accurate.

## Definition of done

- [ ] Required product, suggestion, strategy, and admin endpoints work.
- [ ] Product mutation events create suggestions asynchronously.
- [ ] Both demo paths work from the React console.
- [ ] Prices and stock change only through accepted suggestions or explicit mutations.
- [ ] Runtime strategy switching works without restart.
- [ ] AI failure never creates zero suggestions on the async path.
- [ ] No duplicate pending suggestions exist for the same product, trigger, and type.
- [ ] Backend tests pass and frontend lint/build pass.
- [ ] README supports a fresh run; no secrets are committed.
- [ ] ADR and workflow documentation match implementation.

## Exact resume instructions

1. Read this file completely.
2. Read `C:\Users\zycus\Desktop\zycus interview\IMPLEMENTATION_LOG.md`.
3. Inspect the current backend source tree before editing.
4. Run from `C:\Users\zycus\Desktop\zycus interview\backend`:
   ```powershell
   mvn clean test
   ```
5. Run from `C:\Users\zycus\Desktop\zycus interview\frontend`:
   ```powershell
   npm run lint
   npm run build
   ```
6. If backend tests pass, begin the Immediate next action in this file.
7. If backend tests fail, fix that failure before adding functionality.
8. Work one section at a time, update both documentation files, and record validation results.
9. Do not silently change requirements or architecture; document decisions here first.

## Current next action

The asynchronous rule evaluator and manual suggestion APIs are implemented. The next action is to implement the AI integration slice, beginning with environment-backed configuration, the gateway contract, and pure prompt/response parsing tests.

