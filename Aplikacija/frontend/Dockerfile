# syntax=docker/dockerfile:1

# Zavisnosti i build se vrte na platformi runnera (x86) -- `next build` daje
# platformski nezavisan JS, pa nema potrebe za QEMU emulacijom.
FROM --platform=$BUILDPLATFORM node:22-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# Runtime je ciljna platforma (produkcija je linux/arm64). `output: "standalone"`
# u next.config.ts daje server.js sa samo potrebnim delom node_modules.
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3000 HOSTNAME=0.0.0.0
COPY --from=build --chown=node:node /src/.next/standalone ./
COPY --from=build --chown=node:node /src/.next/static ./.next/static
COPY --from=build --chown=node:node /src/public ./public
USER node
EXPOSE 3000
CMD ["node", "server.js"]
