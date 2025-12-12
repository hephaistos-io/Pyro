export const environment = {
  production: false,
  // Relative path works through nginx proxy (localhost:80 -> backend:8080)
  // For local dev without Docker, use: 'http://localhost:8080/api'
  apiUrl: '/api'
};
