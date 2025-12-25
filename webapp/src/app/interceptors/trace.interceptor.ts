import {HttpInterceptorFn} from '@angular/common/http';

/**
 * Adds X-Trace-Id header to all outgoing HTTP requests.
 * Generates a unique 32-character hex ID per request for distributed tracing.
 */
export const traceInterceptor: HttpInterceptorFn = (req, next) => {
  const traceId = generateTraceId();

  const request = req.clone({
    setHeaders: {
      'X-Trace-Id': traceId
    }
  });

  return next(request);
};

/**
 * Generates a 32-character hex trace ID using crypto.getRandomValues.
 */
function generateTraceId(): string {
  const array = new Uint8Array(16);
  crypto.getRandomValues(array);
  return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('');
}
