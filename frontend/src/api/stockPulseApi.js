const API_BASE = (import.meta.env.VITE_API_BASE || 'http://localhost:8080').replace(/\/$/, '')

const suggestionList = (status, type) => request(`/suggestions/${type}?status=${encodeURIComponent(status)}`)

class ApiError extends Error {
  constructor(message, status) { super(message); this.name = 'ApiError'; this.status = status }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(options.headers || {}) } })
  } catch { throw new ApiError('Unable to reach the backend. Check that the API is running.', 0) }
  const text = await response.text()
  let body
  try { body = text ? JSON.parse(text) : null } catch { body = null }
  if (!response.ok) {
    const fallback = response.status === 409 ? 'This data changed elsewhere. The inventory was refreshed.' : response.status === 404 ? 'The selected product could not be found.' : response.status === 400 ? 'Please check the submitted values.' : 'The backend could not complete this request.'
    const details = Array.isArray(body?.details) ? ` ${body.details.join(' ')}` : body?.details ? ` ${body.details}` : ''
    throw new ApiError(`${body?.message || body?.error || fallback}${details}`, response.status)
  }
  return body
}

export const stockPulseApi = {
  listProducts: () => request('/products'),
  createProduct: (product) => request('/products', { method: 'POST', body: JSON.stringify(product) }),
  updateStock: (id, stockLevel, expectedVersion) => request(`/products/${id}/stock`, { method: 'PATCH', body: JSON.stringify({ stockLevel, expectedVersion }) }),
  simulateOrder: (id, quantity) => request(`/products/${id}/orders`, { method: 'POST', body: JSON.stringify({ quantity }) }),
  suggestPricing: (id) => request(`/products/${id}/suggest-pricing`, { method: 'POST' }),
  suggestReorder: (id) => request(`/products/${id}/suggest-reorder`, { method: 'POST' }),
  getStrategy: () => request('/admin/strategy'),
  updateStrategy: (strategy) => request('/admin/strategy', { method: 'PUT', body: JSON.stringify(strategy) }),
  listPricingSuggestions: (status = 'PENDING') => suggestionList(status, 'pricing'),
  listReorderSuggestions: (status = 'PENDING') => suggestionList(status, 'reorder'),
  resolvePricingSuggestion: (id, action, expectedProductVersion, note = '') => request(`/suggestions/pricing/${id}/${action}`, { method: 'POST', body: JSON.stringify({ expectedProductVersion, status: action === 'accept' ? 'ACCEPTED' : 'REJECTED', note }) }),
  resolveReorderSuggestion: (id, action, expectedProductVersion, note = '') => request(`/suggestions/reorder/${id}/${action}`, { method: 'POST', body: JSON.stringify({ expectedProductVersion, status: action === 'accept' ? 'ACCEPTED' : 'REJECTED', note }) }),
}

export { ApiError }