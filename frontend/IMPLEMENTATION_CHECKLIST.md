# StockPulse Frontend Implementation Checklist

This checklist tracks the original StockPulse brief against the currently verified backend.

## Priority 1 — Live inventory workflows

- [x] React, Vite, JavaScript, and plain `fetch`.
- [x] `VITE_API_BASE` with a `http://localhost:8080` fallback.
- [x] Centralized API module at `src/api/stockPulseApi.js`.
- [x] Safe JSON and non-JSON error handling.
- [x] Live products from `GET /products`.
- [x] Live total, low-stock, and out-of-stock metrics.
- [x] Backend status values remain authoritative.
- [x] Product table with name, SKU, category, price, stock, threshold, demand, status, and actions.
- [x] Search by name or SKU.
- [x] Category, status, and low-stock filters.
- [x] Loading, empty, filtered-empty, error, and disabled states.
- [x] Stock editing through `PATCH /products/{id}/stock`.
- [x] Current product `version` sent as `expectedVersion`.
- [x] Non-negative integer stock validation.
- [x] Refresh after success and after `409` conflicts.
- [x] Sale simulation through `POST /products/{id}/orders`.
- [x] Explicit positive sale quantity.
- [x] Refresh after successful sale and clear order errors.

## Priority 2 — Product management and synchronization

- [x] Verified `CreateProductRequest` before implementing the form.
- [x] Product creation through `POST /products`.
- [x] Required SKU and name validation.
- [x] Valid `ELECTRONICS`, `APPAREL`, and `HOME` category selection.
- [x] Price minimum `0.01`, maximum two decimal places.
- [x] Non-negative integer stock and threshold validation.
- [x] Backend validation details and duplicate-SKU errors displayed.
- [x] Three-second polling with unmount cleanup.
- [x] Protection against overlapping background requests.
- [ ] Verify all mutations with the backend running.
- [ ] Confirm background-refresh failure while preserving last good data.
- [ ] Test responsive layouts at desktop and mobile widths.
- [ ] Extract more components if the dashboard grows.

## Priority 3 — Suggestions and AI workflows

- [x] Recommendation area uses backend pricing and reorder APIs.
- [x] No fabricated recommendations or suggestion counts.
- [x] Verified `SuggestionController`, DTOs, statuses, and resolution request.
- [x] Integrate pricing suggestions with pending status and resolution actions.
- [x] Integrate reorder suggestions with pending status and resolution actions.
- [x] Add real pending/accepted/rejected resolution actions.
- [x] Add manual pricing and reorder suggestion actions.
- [x] Add pricing and reorder strategy controls through confirmed `/admin/strategy` endpoints.
- [x] Keep recommendation generation advisory; acceptance uses the backend's explicit apply workflow.
- [x] Display backend AI/rule/rule-fallback sources while keeping fallback execution server-side.
- [x] Normalize suggestion list responses whether returned as arrays or `content` envelopes.

## Quality and release checks

- [x] `npm run lint` completed with 0 warnings and 0 errors.
- [x] `npm run build` completed successfully.
- [ ] Start backend and verify browser API flows.
- [ ] Verify duplicate SKU, stale version, insufficient stock, and validation errors.
- [ ] Verify refresh after every successful mutation.
- [ ] Verify unavailable-backend retry behavior.
- [x] No mock/demo product data exists.
- [ ] Run final live-data hackathon demo walkthrough.

## Confirmed endpoints

- `GET /products`
- `POST /products`
- `PATCH /products/{id}/stock`
- `POST /products/{id}/orders`
- `GET /suggestions/pricing?status=PENDING`
- `GET /suggestions/reorder?status=PENDING`
- `POST /suggestions/pricing/{id}/accept`
- `POST /suggestions/pricing/{id}/reject`
- `POST /suggestions/reorder/{id}/accept`
- `POST /suggestions/reorder/{id}/reject`

The backend also exposes compatibility aliases under `/suggestions/*-suggestions` and PATCH resolution routes; the frontend uses the primary `/suggestions/pricing` and `/suggestions/reorder` routes.

Manual generation remains available through `POST /products/{id}/suggest-pricing` and `POST /products/{id}/suggest-reorder`; these return DTOs with `triggerReason: MANUAL` and `source: MANUAL`.