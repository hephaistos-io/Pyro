# Frontend Dockerfile - FlagForge Angular App
# Multi-stage build for development and production

# Stage 1: Development with hot reload
FROM node:22-alpine AS development

WORKDIR /app

# Copy package files first (for dependency caching)
COPY webapp/package*.json ./

# Install dependencies
RUN npm ci

# Copy configuration files (not src - that's mounted as volume)
COPY webapp/angular.json webapp/tsconfig*.json ./

# Expose dev server port
EXPOSE 4200

# Start dev server with polling for Docker volume changes
# --host 0.0.0.0 makes it accessible outside container
# --poll 2000 ensures file changes in volumes are detected
CMD ["npm", "run", "start", "--", "--host", "0.0.0.0", "--poll", "2000"]

# Stage 2: Build production assets
FROM node:22-alpine AS builder

WORKDIR /app

COPY webapp/package*.json ./
RUN npm ci

COPY webapp/ .
RUN npm run build -- --configuration production

# Stage 3: Serve with nginx (production)
FROM nginx:alpine AS production

# Copy built assets
COPY --from=builder /app/dist/pyro/browser /usr/share/nginx/html

# Copy nginx configuration
COPY deployment/local/nginx/nginx-frontend.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
