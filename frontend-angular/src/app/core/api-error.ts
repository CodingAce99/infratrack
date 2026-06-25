/**
 * ApiError wraps a non-2xx HTTP response so Angular services can throw a typed error
 * carrying the HTTP status (e.g. 409 duplicate IP) and a human-readable message.
 */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    // Restore prototype chain for ES5 targets so `instanceof ApiError` works across boundaries.
    Object.setPrototypeOf(this, ApiError.prototype);
  }
}