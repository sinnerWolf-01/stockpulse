# StockPulse Frontend

React/Vite dashboard for the StockPulse inventory and pricing advisor.

## Run locally

```bash
npm install
npm run dev
```

Open http://localhost:5173. The backend is expected at http://localhost:8080 and is configured through `VITE_API_BASE` in `.env`.

## Checks

```bash
npm run lint
npm run build
```

## Integrated backend endpoints

- `GET /products`
- `POST /products`
- `PATCH /products/{id}/stock`
- `POST /products/{id}/orders`
- `POST /products/{id}/suggest-pricing`
- `POST /products/{id}/suggest-reorder`
- `GET /admin/strategy`
- `PUT /admin/strategy`
- `GET /suggestions/pricing?status=PENDING`
- `GET /suggestions/reorder?status=PENDING`
- `POST /suggestions/pricing/{id}/accept|reject`
- `POST /suggestions/reorder/{id}/accept|reject`

Suggestion cards now use the backend's pending suggestion APIs. Resolution requests include the product's current `version` as `expectedProductVersion`; accepting applies the backend operation and rejecting records the resolution.

The strategy panel uses the backend's admin strategy endpoint to select `rule` or `ai` independently for pricing and reorder generation. AI failures are handled server-side with `RULE_FALLBACK`; the frontend displays the resulting source on each suggestion. The backend also supports `/suggestions/pricing-suggestions` and `/suggestions/reorder-suggestions` compatibility read routes plus PATCH resolution routes; the frontend uses the primary POST accept/reject routes.

## Project documentation

- [IMPLEMENTATION_CHECKLIST.md](./IMPLEMENTATION_CHECKLIST.md) — prioritized implementation and verification checklist.
- [CHANGELOG.md](./CHANGELOG.md) — frontend changes, backend verification, checks, and remaining work.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the Oxlint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and Oxlint's TypeScript related rules in your project.
