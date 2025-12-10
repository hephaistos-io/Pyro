/**
 * Parse and handle API errors, returning user-friendly error messages
 */
export function handleApiError(err: any, context: 'login' | 'registration' | 'company' | 'profile' = 'login'): string {
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
      if (context === 'registration') return 'Invalid registration data. Please check your inputs';
      if (context === 'company') return 'Invalid company data. Please check your inputs';
      return 'Invalid login credentials';
    case 401:
    case 403:
      if (context === 'profile' || context === 'company') return 'Session expired. Please log in again';
      return 'Invalid email or password';
    case 409:
      if (context === 'company') return 'A company with this name already exists';
      return 'This email is already registered';
    case 500:
      return 'Server error. Please try again later';
    default:
      if (context === 'registration') return 'Registration failed. Please try again';
      if (context === 'company') return 'Failed to create company. Please try again';
      if (context === 'profile') return 'Failed to load profile. Please try again';
      return 'Login failed. Please try again';
  }
}