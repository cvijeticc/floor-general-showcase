import { NextResponse } from "next/server";
import { TOKEN_COOKIE } from "@/lib/api/config";

/** Odjava: brise kolacic sa tokenom. Backend je stateless, pa nema sta drugo da se ponisti. */
export async function POST() {
  const response = NextResponse.json({ ok: true });
  response.cookies.set({ name: TOKEN_COOKIE, value: "", path: "/", maxAge: 0 });
  return response;
}
