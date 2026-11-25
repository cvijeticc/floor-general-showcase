"use client";

import createClient, { type Middleware } from "openapi-fetch";
import type { paths } from "@/lib/api/schema";
import { API_PROXY_PREFIX } from "@/lib/api/config";

/**
 * Tipizovan klijent nad rutama backenda. Tipovi (`schema.d.ts`) se GENERISU iz OpenAPI
 * specifikacije (`npm run api:generate`) — ne pisu se rucno (CLAUDE.md §5).
 */
export const api = createClient<paths>({ baseUrl: API_PROXY_PREFIX });

/** Kad token istekne ili je nevazeci, korisnik ide na prijavu — bez tihog pada ekrana. */
const redirectOnUnauthorized: Middleware = {
  async onResponse({ response }) {
    if (response.status === 401 && typeof window !== "undefined") {
      const current = window.location.pathname + window.location.search;
      window.location.href = `/prijava?redirect=${encodeURIComponent(current)}`;
    }
    return response;
  },
};

api.use(redirectOnUnauthorized);

/**
 * Svaki poziv baca gresku sa backenda kakva jeste — errorMessage() je posle prikazuje.
 * Zajednicko za sve ekrane, da se ne prepisuje po hook fajlovima.
 */
export async function unwrap<T>(promise: Promise<{ data?: T; error?: unknown }>): Promise<T> {
  const { data, error } = await promise;
  if (error) {
    throw error;
  }
  return data as T;
}

/** Oblik greske koji backend uvek vraca (ApiError iz GlobalExceptionHandler-a). */
export type ApiError = {
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  fieldErrors?: Record<string, string>;
};

/**
 * Poruka za korisnika. Backend vec salje tekst na srpskom, pa se on prikazuje kakav jeste —
 * nikad „Error 409" (pravilo Etape FE).
 */
export function errorMessage(error: unknown, fallback = "Došlo je do greške"): string {
  const apiError = error as ApiError | undefined;
  if (!apiError) {
    return fallback;
  }
  const fieldError = apiError.fieldErrors ? Object.values(apiError.fieldErrors)[0] : undefined;
  return fieldError ?? apiError.message ?? fallback;
}
