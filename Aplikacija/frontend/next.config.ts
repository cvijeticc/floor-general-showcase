import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Docker imidž (Aplikacija/frontend/Dockerfile) pokreće .next/standalone/server.js.
  output: "standalone",
};

export default nextConfig;
