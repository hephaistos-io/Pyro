/**
 * Type guard for HTTP error responses
 */
interface HttpErrorResponse {
  error?: unknown;
  status?: number;
}

interface ApiErrorResponse {
  code?: string;
  message?: string;
}

function isHttpErrorResponse(err: unknown): err is HttpErrorResponse {
  return typeof err === 'object' && err !== null;
}

function isApiErrorResponse(obj: unknown): obj is ApiErrorResponse {
  return typeof obj === 'object' && obj !== null && ('code' in obj || 'message' in obj);
}

function getDefaultErrorMessage(context: 'login' | 'registration' | 'company' | 'profile' | 'application'): string {
  switch (context) {
    case 'registration':
      return 'Registration failed. Please try again';
    case 'company':
      return 'Failed to create company. Please try again';
    case 'profile':
      return 'Failed to load profile. Please try again';
    case 'application':
      return 'Failed to create application. Please try again';
    default:
      return 'Login failed. Please try again';
  }
}

/**
 * Parse and handle API errors, returning user-friendly error messages
 */
export function handleApiError(err: unknown, context: 'login' | 'registration' | 'company' | 'profile' | 'application' = 'login'): string {
  if (!isHttpErrorResponse(err)) {
    return getDefaultErrorMessage(context);
  }

  // Parse backend error response
  // Check if err.error is a string and parse it
  let errorObj: unknown = err.error;
  if (typeof err.error === 'string') {
    try {
      errorObj = JSON.parse(err.error);
    } catch {
      errorObj = null;
    }
  }

  // Handle structured error responses with error codes
  if (isApiErrorResponse(errorObj) && errorObj.code) {
    const code = errorObj.code;
    const message = errorObj.message;

    switch (code) {
      case 'INVALID_CREDENTIALS':
        return 'Invalid email or password';
      case 'DUPLICATE_RESOURCE':
        if (context === 'application') return 'An application with this name already exists';
        return 'This email is already registered';
      case 'BREACHED_PASSWORD':
        return message || 'This password has been found in data breaches. Please choose a different password.';
      case 'VALIDATION_ERROR':
        return message || 'Validation error';
      case 'INTERNAL_ERROR':
        return 'Server error. Please try again later';
      default:
        return message || 'An error occurred';
    }
  }

  // Fallback to HTTP status codes for unexpected responses
  const status = Number(err.status);

  switch (status) {
    case 0:
      return 'Cannot connect to server. Please check your connection';
    case 400:
      if (context === 'registration') return 'Invalid registration data. Please check your inputs';
      if (context === 'company') return 'Invalid company data. Please check your inputs';
      if (context === 'application') return 'Invalid application data. Please check your inputs';
      return 'Invalid login credentials';
    case 401:
    case 403:
      if (context === 'profile' || context === 'company' || context === 'application') return 'Session expired. Please log in again';
      return 'Invalid email or password';
    case 409:
      if (context === 'company') return 'A company with this name already exists';
      if (context === 'application') return 'An application with this name already exists';
      return 'This email is already registered';
    case 500:
      return 'Server error. Please try again later';
    default:
      if (context === 'registration') return 'Registration failed. Please try again';
      if (context === 'company') return 'Failed to create company. Please try again';
      if (context === 'profile') return 'Failed to load profile. Please try again';
      if (context === 'application') return 'Failed to create application. Please try again';
      return 'Login failed. Please try again';
  }
}
