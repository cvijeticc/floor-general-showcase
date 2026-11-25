import { NextRequest, NextResponse } from "next/server";
import { TOKEN_COOKIE } from "@/lib/api/config";

/**
 * UC-FE-02 — /admin/* je zatvoren. Bez kolacica sa tokenom korisnik ide na prijavu, a posle
 * uspesne prijave se vraca tamo gde je krenuo (parametar `redirect`).
 *
 * Ovo je samo prva brana; pravu proveru radi backend na svakom pozivu (JWT + @PreAuthorize).
 */
export function middleware(request: NextRequest) {
  const token = request.cookies.get(TOKEN_COOKIE)?.value;

  if (!token) {
    const loginUrl = new URL("/prijava", request.url);
    loginUrl.searchParams.set("redirect", request.nextUrl.pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*"],
};
