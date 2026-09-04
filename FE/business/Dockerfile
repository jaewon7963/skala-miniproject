# syntax=docker/dockerfile:1

# ── 빌드 ──────────────────────────────────────────────────────────────────────
# package.json 의 engines 가 node ^20.19 || >=22.12 라서 node:20 계열은 못 쓴다.
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite의 VITE_* 는 빌드 시점에 번들에 박힌다. 런타임 환경변수로는 바꿀 수 없어서 여기서 넣는다.
ARG VITE_USE_MOCK=false
ARG VITE_API_BASE_URL=/api
ENV VITE_USE_MOCK=$VITE_USE_MOCK \
    VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ── 실행 ──────────────────────────────────────────────────────────────────────
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
