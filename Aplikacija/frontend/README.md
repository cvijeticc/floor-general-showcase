# Floor General — frontend

Next.js 15 (App Router) + React 19 + TypeScript + Tailwind 4 + shadcn/ui.
Jedna aplikacija pokriva oba dela: `/` javni sajt (Rok 2) i `/admin` administracija (Rok 1).
Pregled celog projekta je u `README.md` u korenu repoa.

## Pokretanje

```bash
# 1. dev baza i backend (iz Aplikacija/backend)
docker compose up -d
mvn spring-boot:run          # http://localhost:8080

# 2. frontend (iz Aplikacija/frontend)
cp .env.example .env.local   # BACKEND_URL=http://localhost:8080
npm install
npm run dev                  # http://localhost:3000
```

Prijava podrazumevanim nalogom: `admin` / `admin123` (pravi ga `AdminSeeder` na prvom pokretanju
backenda). Posle prve prijave lozinku promeniti kroz `PATCH /api/v1/users/me/password`.

## Kako radi prijava

Token **ne ide u `localStorage`** (UC-FE-02). Redosled je:

1. Forma šalje kredencijale na `/api/auth/login` — Next rutu, ne backend.
2. Ta ruta zove backend, uzme JWT i upiše ga u **httpOnly kolačić** `fg_token`.
   U browser se vraća samo korisničko ime; token nikad ne dođe do JavaScript-a.
3. Klijentski kod zove `/api/backend/...`, a proxy (`app/api/backend/[...path]/route.ts`)
   dodaje `Authorization: Bearer` iz kolačića. Zato nema ni CORS podešavanja na backendu.
4. `middleware.ts` čuva `/admin/*`: bez kolačića → preusmerenje na `/prijava?redirect=…`.
   Na `401` iz API poziva klijent radi isto (vidi `lib/api/client.ts`).

## API klijent se generiše, ne piše

Tipovi u `lib/api/schema.d.ts` dolaze iz OpenAPI specifikacije backenda:

```bash
# uz pokrenut backend
curl http://localhost:8080/v3/api-docs -o openapi.json
npm run api:generate
```

`openapi.json` i `schema.d.ts` se čuvaju u repozitorijumu da bi `npm run build` radio i bez
pokrenutog backenda. **Posle svake izmene ruta na backendu oba fajla se regenerišu.**

## Struktura

```
app/
├── (public)/        javni deo (Rok 2) — SSR, bez prijave
├── admin/           administracija (Rok 1) — client-side, zaštićeno middleware-om
├── prijava/         ekran za prijavu
└── api/             Next rute: auth/login, auth/logout, backend/[...path] (proxy)
components/ui/       shadcn/ui komponente
lib/api/             generisani tipovi + tipizovan klijent
```

## Provere

```bash
npm run lint
npx tsc --noEmit
npm run build
```
