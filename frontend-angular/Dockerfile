# ── Stage 1: Build Angular static assets ─────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies first (cached layer; only re-runs on package-lock change).
COPY package.json package-lock.json ./
RUN npm ci

# Build the production bundle → dist/frontend-angular/browser (see angular.json outputPath).
COPY . .
RUN npm run build

# ── Stage 2: Serve with nginx ────────────────────────────────────────────────
FROM nginx:alpine

# Angular build output becomes the nginx document root.
# The `application` builder places the browser bundle under <outputPath>/browser/
# (outputPath is "dist/frontend-angular" in angular.json) → dist/frontend-angular/browser.
COPY --from=build /app/dist/frontend-angular/browser /usr/share/nginx/html

# nginx config: static serve + /api/ reverse proxy + SPA fallback.
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
