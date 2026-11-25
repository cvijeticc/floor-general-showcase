import { NextRequest, NextResponse } from "next/server";
import { BACKEND_URL, TOKEN_COOKIE, TOKEN_COOKIE_SECURE, TOKEN_MAX_AGE_SECONDS } from "@/lib/api/config";

/**
 * UC-FE-02 — prijava. Token se sa backenda uzima ovde, na serveru, i odlaze u httpOnly kolacic.
 * U browser se vraca samo korisnicko ime; JWT nikad ne dodje do JavaScript-a.
 */
export async function POST(request: NextRequest) {
  const credentials = await request.text();

  const response = await fetch(`${BACKEND_URL}/api/v1/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: credentials,
    cache: "no-store",
  });

  if (!response.ok) {
    const body = await response.text();
    return new NextResponse(body.length > 0 ? body : null, {
      status: response.status,
      headers: { "Content-Type": response.headers.get("content-type") ?? "application/json" },
    });
  }

  const payload = (await response.json()) as { accessToken: string; username?: string };
  const result = NextResponse.json({ username: payload.username ?? null });

  result.cookies.set({
    name: TOKEN_COOKIE,
    value: payload.accessToken,
    httpOnly: true,
    sameSite: "lax",
    secure: TOKEN_COOKIE_SECURE,
    path: "/",
    maxAge: TOKEN_MAX_AGE_SECONDS,
  });

  return result;
}
