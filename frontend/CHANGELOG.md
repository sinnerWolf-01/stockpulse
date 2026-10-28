# StockPulse Frontend Change Log

## 2026-10-28 — Backend-aware product workflows

### Suggestion API integration

- Re-scanned the backend and verified `SuggestionController`, pricing/reorder DTOs, persisted statuses, strategies, and optimistic resolution behavior.
- Integrated pending pricing and reorder suggestion reads.
- Added accept/reject actions with `expectedProductVersion`.
- Refreshes products and pending suggestions after suggestion resolution and conflicts.
- Added manual pricing and reorder suggestion actions backed by the new product endpoints.
- Rechecked backend AI gateway/parser configuration and kept strategy selection internal to recommendation generation.
- Made suggestion reads tolerant of array or `content`-enveloped responses and URL-encoded their status query.
- Added strategy controls backed by `GET/PUT /admin/strategy` for independent pricing and reorder selection.

### Implemented

- Built the responsive StockPulse inventory dashboard.
- Added centralized plain-`fetch` API communication.
- Added `VITE_API_BASE=http://localhost:8080` configuration.
- Integrated `GET /products`.
- Integrated `POST /products` using the verified `CreateProductRequest` fields.
- Integrated `PATCH /products/{id}/stock` with the selected product version.
- Integrated `POST /products/{id}/orders` with explicit quantities.
- Added summary metrics, table display, search, category/status filters, and low-stock filter.
- Added stock-edit and sale-simulation modals.
- Added create-product modal with backend-aligned validation.
- Added three-second polling, cleanup, and overlap protection.
- Added a labelled recommendation placeholder with no fake data.

### Error handling and synchronization

- Added safe JSON/non-JSON response parsing.
- Added status-aware `400`, `404`, and `409` messages.
- Added support for backend `details` error content.
- Refreshes after successful mutations.
- Refreshes and informs the user after conflicts.
- Preserves backend status values.
- Uses strict `stockLevel < reorderThreshold` low-stock logic.

### Backend verification

Inspected:

- `backend/src/main/java/com/stockpulse/product/ProductController.java`
- `backend/src/main/java/com/stockpulse/product/CreateProductRequest.java`
- `backend/src/main/java/com/stockpulse/product/Category.java`
- `backend/src/main/java/com/stockpulse/product/OrderRequest.java`

Confirmed product endpoints:

- `GET /products`
- `POST /products`
- `PATCH /products/{id}/stock`
- `POST /products/{id}/orders`

At the time of this entry, no suggestion controller or suggestion HTTP endpoints were found in the inspected source. This was superseded by the later suggestion integration documented above.

### Checks

- `npm run lint`: completed with 0 warnings and 0 errors during the completed validation run.
- `npm run build`: completed successfully.
- `GET http://localhost:8080/products`: backend was not running during the latest check.
- Frontend dev server was started with `npm run dev -- --host 0.0.0.0`.

### Remaining work

- Run and verify all product workflows with Spring Boot running.
- Verify duplicate SKU, stale-version, validation, and insufficient-stock responses.
- Verify background-refresh failure behavior in the browser.
- Recommendation endpoints are now integrated; complete live browser verification with the backend running.