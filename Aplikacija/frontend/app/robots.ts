import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/site-url";

// SITE_URL se zadaje pri pokretanju kontejnera, ne pri build-u — zato na zahtev.
export const dynamic = "force-dynamic";

/** UC-PUB-09 — javni sajt se indeksira, admin, prijava i API ne. */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: { userAgent: "*", allow: "/", disallow: ["/admin", "/prijava", "/api/"] },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
