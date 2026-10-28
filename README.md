# StockPulse — Inventory & Commerce Advisor

StockPulse is a full-stack inventory dashboard that helps operators monitor stock health, simulate sales, and review pricing and reorder recommendations. It combines deterministic rule-based strategies with an optional OpenAI-compatible LLM gateway. Recommendations are advisory: pricing and stock changes are applied through explicit approval actions, not automatically when a signal is detected.

## Features

- Inventory dashboard with seeded demo products, category/status filtering, and search.
- Create products, edit stock with optimistic-concurrency checks, and simulate customer orders.
- Asynchronous, after-commit stock-signal evaluation for inventory-low and demand-spike conditions.
- Persisted pricing and reorder suggestions with pending-suggestion deduplication.
- Accept/reject workflows, conflict handling, and superseding of competing pending suggestions.
- Rule-based pricing/reorder strategies and optional AI strategies.
- Runtime strategy selection for pricing and reorder independently.
- Rule-based fallback when the selected AI strategy fails.

## Technology stack

- **Backend:** Java 17, Spring Boot 3.5.6, Spring MVC, Spring Data JPA, Bean Validation, Maven.
- **Database:** H2 in-memory database for the interview/demo build. Data is recreated when the application restarts.
- **Frontend:** React, Vite, JavaScript, plain `fetch`.
- **Default ports:** backend `8080`, frontend `5173`.

## Prerequisites

- JDK 17 or newer.
- Node.js compatible with Vite 8 (Node.js 20.19+ or 22.12+ recommended).
- npm.
- Maven installed, or use the included Maven Wrapper (`mvnw.cmd` on Windows / `./mvnw` on macOS/Linux).

## Run locally

Run the backend and frontend in separate terminals.

### 1. Start the backend

**Windows PowerShell / Command Prompt:**

```powershell
cd backend
copy .env.example .env.local
```

Leave the LLM fields blank to use the default rule-based strategies. If AI mode is needed, fill in the required values in `backend/.env.local` using credentials you are authorized to use. Never commit or share this file.

Start the application using the provided helper (which loads `.env.local`):

```powershell
.\start-backend.cmd
```

Or run directly with Maven (this does not automatically load `.env.local`; configure environment variables in your shell first):

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**macOS/Linux:**

```bash
cd backend
cp .env.example .env.local
chmod +x mvnw
./mvnw spring-boot:run
```

The backend listens on `http://localhost:8080` by default.

### 2. Start the frontend

Open a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` in your browser. The frontend API base URL is configured in `frontend/.env` through `VITE_API_BASE`; the default is `http://localhost:8080`.

### 3. Confirm the connection

Open `http://localhost:8080/products`. A successful response should return the seeded product list. The dashboard should show **Backend connected** when the frontend can reach the API.

## AI configuration

AI is optional; the application defaults to rule-based strategies. Configure these environment variables only if you have valid, authorized gateway credentials:

| Variable | Purpose |
|---|---|
| `LLM_BASE_URL` | Base URL for the OpenAI-compatible gateway. |
| `LLM_API_KEY` | Bearer token for the gateway. |
| `LLM_MODEL` | Model identifier accepted by the gateway. |
| `LLM_PRODUCT` | Required gateway `product` header value. |
| `LLM_COOKIE` | Optional cookie header, if required by the gateway. |
| `STOCKPULSE_PRICING_STRATEGY` | Initial pricing strategy: `rule` or `ai`; defaults to `rule`. |
| `STOCKPULSE_REORDER_STRATEGY` | Initial reorder strategy: `rule` or `ai`; defaults to `rule`. |
| `STOCKPULSE_AI_PRICING_MAX_CHANGE_PERCENT` | Maximum AI price change fraction; defaults to `0.20` (20%). |
| `STOCKPULSE_AI_PRICING_MINIMUM_PRICE` | Minimum permitted recommended price; defaults to `0.01`. |
| `STOCKPULSE_AI_REORDER_MAX_MULTIPLIER` | Maximum AI reorder quantity relative to the reorder threshold; defaults to `10`. |
| `SERVER_PORT` | Intended backend port for the helper script; defaults to `8080`. |

The gateway uses `/v1/chat/completions` and has 15-second connection/read timeouts. Keep credentials out of source control, screenshots, logs, and submission archives. Use `.env.example` as the shareable template; do not distribute `.env.local`.

The dashboard's **Recommendation strategies** panel can switch pricing and reorder strategies at runtime. If AI configuration is absent or the AI call fails, the backend is designed to fall back to rule-based recommendations.

## Core API endpoints

### Products

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/products` | List products; supports optional `status` and `category` filters. |
| `POST` | `/products` | Create a product. |
| `PATCH` | `/products/{id}/stock` | Set stock to an absolute value; include `expectedVersion`. |
| `POST` | `/products/{id}/orders` | Simulate a customer order; quantity defaults to one where supported. |
| `POST` | `/products/{id}/suggest-pricing` | Manually request a pricing suggestion. |
| `POST` | `/products/{id}/suggest-reorder` | Manually request a reorder suggestion. |

### Suggestions

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/suggestions/pricing?status=PENDING` | List pricing suggestions. |
| `GET` | `/suggestions/reorder?status=PENDING` | List reorder suggestions. |
| `POST` | `/suggestions/pricing/{id}/accept` | Accept a pricing suggestion. |
| `POST` | `/suggestions/pricing/{id}/reject` | Reject a pricing suggestion. |
| `POST` | `/suggestions/reorder/{id}/accept` | Accept a reorder suggestion. |
| `POST` | `/suggestions/reorder/{id}/reject` | Reject a reorder suggestion. |
| `PATCH` | `/pricing-suggestions/{id}` | Compatibility resolution route. |
| `PATCH` | `/reorder-suggestions/{id}` | Compatibility resolution route. |

The backend also supports `/suggestions/pricing-suggestions` and `/suggestions/reorder-suggestions` compatibility read routes.

### Strategy selection

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/admin/strategy` | Read current pricing and reorder strategies. |
| `PUT` | `/admin/strategy` | Change strategy selection at runtime. |

## Business rules

- Product status is derived: zero stock gives `OUT_OF_STOCK`; otherwise a pending pricing suggestion gives `PRICE_REVIEW_PENDING`; otherwise `ACTIVE`.
- Rule pricing: stock below the reorder threshold recommends a 10% price increase; otherwise, a demand spike greater than twice the category peer average can recommend a 5% increase; otherwise the recommendation is `HOLD`.
- Rule reorder quantity: `max(1, reorderThreshold * 3 - stockLevel)`.
- Default reorder lead time is 7 days.
- Category peer averages exclude the current product.
- Inventory-low and demand-spike signals are evaluated asynchronously after a successful mutation transaction commits.
- Accepting a pricing suggestion applies its recommended price, except `HOLD`, which leaves the price unchanged. Accepting a reorder suggestion increases stock by the recommended quantity.
- Rejections do not apply the suggested change. Conflicts such as stale versions or insufficient stock are returned as HTTP errors.

## Test and build commands

Run from the project root, using separate terminals as needed.

**Backend tests (Windows):**

```powershell
cd backend
.\mvnw.cmd clean test
```

**Backend tests (macOS/Linux):**

```bash
cd backend
./mvnw clean test
```

**Frontend lint and production build:**

```bash
cd frontend
npm run lint
npm run build
```

Run these commands against the current source before submission. Older logs in the workspace are historical evidence and should not be treated as validation of later changes. For an end-to-end smoke test, create or sell a product, wait for asynchronous suggestions, accept one suggestion, reject another, and confirm that the dashboard refreshes with the expected price, stock, and statuses.

## Demo scenario

1. Start both applications and confirm the dashboard says **Backend connected**.
2. Find a product whose stock is below its reorder threshold, or create one with stock `5` and threshold `10`.
3. Click **Sell** on that product and submit a quantity of `1`.
4. Refresh after a few seconds and check for automatically generated pending suggestions.
5. Accept the pricing suggestion and verify that the price changes only after acceptance.
6. Reject the reorder suggestion and verify that rejection does not increase stock.
7. Switch strategies in the dashboard and verify the selected strategy is returned by `GET /admin/strategy`.

## Troubleshooting

- **“Unable to reach the backend”**: verify the backend is running on port `8080` and `frontend/.env` points to the correct URL; restart Vite after changing environment variables.
- **No automatic recommendations**: check backend logs for asynchronous evaluation errors and confirm the product actually meets a signal condition. Do not use manual suggestion buttons to claim the asynchronous flow passed.
- **AI strategy fails**: verify all required gateway variables, especially `LLM_PRODUCT`, and confirm that the configured gateway/model is reachable. Rule fallback should remain available.
- **Port already in use**: stop the process occupying the port or configure a different backend port and update `VITE_API_BASE` accordingly.
- **Data disappears after restart**: this demo uses an in-memory H2 database, so data is not persisted between application runs.

## Project documentation

- `IMPLEMENTATION_CHECKLIST.md` — project requirements and verification checklist.
- `IMPLEMENTATION_LOG.md` — implementation history and validation notes.
- `CHANGELOG.md` — recorded changes.

## Current verification status

This README documents the intended setup and behavior based on the source tree. It does not assert that the latest full backend test suite or live end-to-end flow has passed. Run the commands above on the exact version being submitted and record the results before claiming final verification.
