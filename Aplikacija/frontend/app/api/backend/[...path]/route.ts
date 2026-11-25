import { revalidateTag } from "next/cache";
import { NextRequest, NextResponse } from "next/server";
import { BACKEND_URL, TOKEN_COOKIE } from "@/lib/api/config";
import { PUBLIC_CACHE_TAG } from "@/lib/site-api";

/**
 * Proxy ka Spring backendu. Postoji da token ostane u httpOnly kolacicu: browser salje
 * zahtev bez zaglavlja, a ovde se dodaje `Authorization: Bearer`. Bonus: nema CORS-a,
 * jer je sve isto poreklo.
 */
async function forward(request: NextRequest, path: string[]) {
  const token = request.cookies.get(TOKEN_COOKIE)?.value;
  const search = request.nextUrl.search;
  const target = `${BACKEND_URL}/${path.join("/")}${search}`;

  const headers = new Headers();
  headers.set("Accept", "application/json");
  const contentType = request.headers.get("content-type");
  if (contentType) {
    headers.set("Content-Type", contentType);
  }
  // Javne rute (/api/v1/public/...) idu bez tokena: istekao token admina bi inace oborio
  // prijavu u ligu sa sajta, jer resource server odbija los Bearer i na permitAll rutama.
  const isPublic = path[0] === "api" && path[1] === "v1" && path[2] === "public";
  if (token && !isPublic) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const hasBody = request.method !== "GET" && request.method !== "DELETE";
  const response = await fetch(target, {
    method: request.method,
    headers,
    body: hasBody ? await request.text() : undefined,
    cache: "no-store",
  });

  // Uspešna izmena iz admin panela (rezultat, serija, raspored, „Poništi"...) odmah briše keš
  // javnog sajta, da posetioci ne gledaju staro drvo i tabele do 60 s (lib/site-api.ts).
  // Javne rute se preskaču: prijava u ligu sa sajta ne menja ništa što sajt prikazuje.
  if (request.method !== "GET" && !isPublic && response.ok) {
    revalidateTag(PUBLIC_CACHE_TAG);
  }

  // arrayBuffer, ne text(): Excel izvoz je binaran i text() bi ga pokvario.
  const body = await response.arrayBuffer();
  const responseHeaders = new Headers({
    "Content-Type": response.headers.get("content-type") ?? "application/json",
  });
  const disposition = response.headers.get("content-disposition");
  if (disposition) {
    responseHeaders.set("Content-Disposition", disposition);
  }
  return new NextResponse(body.byteLength > 0 ? body : null, {
    status: response.status,
    headers: responseHeaders,
  });
}

type RouteContext = { params: Promise<{ path: string[] }> };

export async function GET(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  return forward(request, path);
}

export async function POST(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  return forward(request, path);
}

export async function PUT(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  return forward(request, path);
}

export async function PATCH(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  return forward(request, path);
}

export async function DELETE(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  return forward(request, path);
}
