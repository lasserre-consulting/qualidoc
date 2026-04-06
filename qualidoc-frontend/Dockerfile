# ── Stage 1 : build Angular ───────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# ── Stage 2 : Nginx ───────────────────────────────────────────────────────────
FROM nginx:alpine AS runtime

COPY --from=build /app/dist/qualidoc-frontend/browser /usr/share/nginx/html
COPY --from=build /app/src/assets/favicon.ico /usr/share/nginx/html/favicon.ico
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
