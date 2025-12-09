/**
 * Parse and handle API errors, returning user-friendly error messages
 */
export function handleApiError(err: any, context: 'login' | 'registration' = 'login'): string {
  // Parse backend error response
  // Check if err.error is a string and parse it
  let errorObj = err.error;
  if (typeof err.error === 'string') {
    try {
      errorObj = JSON.parse(err.error);
    } catch (e) {
      errorObj = null;
    }
  }

  // Handle structured error responses with error codes
  if (errorObj && errorObj.code) {
    const code = errorObj.code;
    const message = errorObj.message;

    switch (code) {
      case 'INVALID_CREDENTIALS':
        return 'Invalid email or password';
      case 'DUPLICATE_RESOURCE':
        return 'This email is already registered';
      case 'BREACHED_PASSWORD':
        return message || 'This password has been found in data breaches. Please choose a different password.';
      case 'VALIDATION_ERROR':
        return message;
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
      return context === 'registration'
        ? 'Invalid registration data. Please check your inputs'
        : 'Invalid login credentials';
    case 401:
    case 403:
      return 'Invalid email or password';
    case 409:
      return 'This email is already registered';
    case 500:
      return 'Server error. Please try again later';
    default:
      return context === 'registration'
        ? 'Registration failed. Please try again'
        : 'Login failed. Please try again';
  }
}