# Frontend image: static build served by nginx, which also proxies the API so the browser talks to
# a single origin (no CORS, refresh cookie stays first-party).
# Build:  docker build --build-arg VITE_STRIPE_PUBLISHABLE_KEY=pk_test_... -t vendora-frontend frontend

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Vite inlines VITE_* variables at build time.
ARG VITE_STRIPE_PUBLISHABLE_KEY=""
ENV VITE_STRIPE_PUBLISHABLE_KEY=$VITE_STRIPE_PUBLISHABLE_KEY
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
