/**
 * Adresa backenda i ime kolacica sa tokenom.
 *
 * Browser NIKAD ne zove backend direktno: ide na /api/backend/... u Next-u, koji dodaje
 * Authorization zaglavlje iz httpOnly kolacica. Zato token ne postoji u JavaScript-u
 * (ni u localStorage — UC-FE-02), a backend ne mora da otvara CORS.
 */
export const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

/** Prefiks preko kojeg klijentski kod zove backend. */
export const API_PROXY_PREFIX = "/api/backend";

export const TOKEN_COOKIE = "fg_token";

/**
 * `Secure` kolacic pregledac prima samo preko HTTPS-a. Dok produkcija nema domen i sertifikat,
 * compose postavlja AUTH_COOKIE_SECURE=false — inace prijava preko http://<IP> ne radi.
 * Bez te promenljive vazi staro pravilo: secure u produkciji, ne i lokalno.
 */
export const TOKEN_COOKIE_SECURE =
  process.env.AUTH_COOKIE_SECURE !== undefined
    ? process.env.AUTH_COOKIE_SECURE === "true"
    : process.env.NODE_ENV === "production";

/** JWT sa backenda traje 1h (app.jwt.expiration) — kolacic ne sme da ga nadzivi. */
export const TOKEN_MAX_AGE_SECONDS = 60 * 60;
