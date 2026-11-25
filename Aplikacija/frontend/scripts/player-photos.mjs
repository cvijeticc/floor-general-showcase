// Slike igrača za javni sajt (odluka D-21) — pokreće se lokalno kad stignu slike od lige:
//
//   node scripts/player-photos.mjs <folder-sa-slikama> [--backend http://localhost:8082]
//
// Ulaz: folder VAN repoa (frontend/ je Docker build kontekst), fajlovi nazvani po igraču —
// „Miloš Ristić.jpg" ili „milos-ristic.png". Ime fajla bez dijakritika daje slug, isti kao u URL-u
// profila (lib/player-url.ts), pa je „Milos Ristic.jpg" isto što i „Miloš Ristić.jpg".
// Izlaz:
//   public/igraci/{slug}.jpg        portret 640×800 (JPEG, jer ga i Viber i WhatsApp prikazuju u pregledu linka)
//   public/igraci/mini/{slug}.webp  160×160 za tabele, mečeve i pretragu
//   lib/player-photos.json          spisak koji sajt čita (server i pregledač)
// Kadriranje: sharp „attention" traži lice i kontrast. Ako odseče glavu, u ulazni folder se stavi
// kadriranje.json, npr. { "milos-ristic": "top" } (top, center, bottom, attention, entropy).
// „glava" je za izrezane figure celog tela na providnoj pozadini (slike sa turniriukraju.com,
// 10.11.2026.): portret se seče od vrha, a mala slika je kvadrat glava–ramena oko glave koju
// skripta nađe po providnosti — inače bi u krugu od 20 px bio ceo trup.
// EXIF (i GPS sa telefona) se ne prenosi — sharp ga bez withMetadata() ne upisuje.
// sharp dolazi uz Next (node_modules), pa nije posebna zavisnost.

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";
import { slugify } from "../lib/player-url.ts";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const OUT = path.join(ROOT, "public", "igraci");
const OUT_MINI = path.join(OUT, "mini");
const MANIFEST = path.join(ROOT, "lib", "player-photos.json");
const INPUT_TYPES = new Set([".jpg", ".jpeg", ".png", ".webp", ".avif", ".tif", ".tiff"]);

const args = process.argv.slice(2);
const input = args.find((a) => !a.startsWith("--"));
const backendIndex = args.indexOf("--backend");
const backend = backendIndex >= 0 ? args[backendIndex + 1] : null;

if (!input || !fs.existsSync(input)) {
  console.error("Upotreba: node scripts/player-photos.mjs <folder-sa-slikama> [--backend http://localhost:8082]");
  process.exit(1);
}

const focusFile = path.join(input, "kadriranje.json");
const focus = fs.existsSync(focusFile) ? JSON.parse(fs.readFileSync(focusFile, "utf8")) : {};
const position = (slug) => {
  const wanted = focus[slug];
  if (!wanted || wanted === "attention") return sharp.strategy.attention;
  if (wanted === "entropy") return sharp.strategy.entropy;
  if (wanted === "glava") return "top";
  return wanted;
};

/**
 * Kvadrat glava–ramena na figuri sa providnom pozadinom: vrh glave je prvi red sa neprovidnim
 * pikselima, sredina glave je prosek tih piksela u gornjih 8% figure, a stranica je ~26% visine
 * (za poprsje — glava šira od petine visine — cela širina).
 */
async function headBox(pipeline) {
  const { data, info } = await pipeline.clone().ensureAlpha().extractChannel("alpha").raw().toBuffer({ resolveWithObject: true });
  const { width, height } = info;
  const opaque = (x, y) => data[y * width + x] > 128;
  let top = 0;
  while (top < height && ![...Array(width).keys()].some((x) => opaque(x, top))) top++;
  const band = Math.max(1, Math.round(height * 0.08));
  let sum = 0;
  let count = 0;
  let minX = width;
  let maxX = 0;
  for (let y = top; y < Math.min(height, top + band); y++) {
    for (let x = 0; x < width; x++) {
      if (opaque(x, y)) {
        sum += x;
        count++;
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
      }
    }
  }
  const centerX = count ? sum / count : width / 2;
  // Poprsje: glava je široka u odnosu na visinu slike (kod cele figure je ~7 puta manja od visine).
  // Ne gleda se odnos stranica slike — figura raširenih ruku je široka, a ipak je cela figura.
  const bust = count > 0 && (maxX - minX) / height > 0.2;
  const side = Math.max(1, Math.min(width, height, bust ? Math.min(width, height) : Math.round(height * 0.26)));
  const left = Math.round(Math.min(Math.max(0, centerX - side / 2), width - side));
  const y = Math.max(0, Math.min(height - side, top - Math.round(height * 0.015)));
  return { left, top: y, width: side, height: side };
}

fs.mkdirSync(OUT_MINI, { recursive: true });
const manifest = fs.existsSync(MANIFEST) ? JSON.parse(fs.readFileSync(MANIFEST, "utf8")) : {};

const files = fs.readdirSync(input).filter((f) => INPUT_TYPES.has(path.extname(f).toLowerCase()));
const done = [];
for (const file of files) {
  const slug = slugify(path.basename(file, path.extname(file)));
  if (!slug) {
    console.warn(`Preskočeno (ime fajla ne daje slug): ${file}`);
    continue;
  }
  // Slike stižu i kao snimci ekrana telefona (WhatsApp) — trim skida jednobojne trake sa ivica
  // pre kadriranja, inače crna traka sa vrha ekrana završi u portretu.
  const base = sharp(path.join(input, file)).autoOrient().trim({ threshold: 12 });

  await base
    .clone()
    .resize(640, 800, { fit: "cover", position: position(slug) })
    .jpeg({ quality: 82, mozjpeg: true })
    .toFile(path.join(OUT, `${slug}.jpg`));

  // Posle trim-a sharp ne ume da nastavi extract nad istim nizom, pa se trim-ovana slika prvo materijalizuje.
  const thumbSource = focus[slug] === "glava" ? sharp(await base.clone().png().toBuffer()) : null;
  await (thumbSource ? thumbSource.clone().extract(await headBox(thumbSource)) : base.clone())
    .resize(160, 160, { fit: "cover", position: position(slug) })
    .webp({ quality: 80 })
    .toFile(path.join(OUT_MINI, `${slug}.webp`));

  manifest[slug] = { photo: `/igraci/${slug}.jpg`, thumb: `/igraci/mini/${slug}.webp` };
  done.push(slug);
  console.log(`✓ ${file} → ${slug}`);
}

const sorted = Object.fromEntries(Object.entries(manifest).sort(([a], [b]) => a.localeCompare(b)));
fs.writeFileSync(MANIFEST, `${JSON.stringify(sorted, null, 2)}\n`);
console.log(`\nObrađeno: ${done.length}. U spisku ukupno: ${Object.keys(sorted).length}.`);

// Provera prema pravom spisku igrača: slika bez igrača (pogrešno ime) i igrači bez slike.
if (backend) {
  const response = await fetch(`${backend.replace(/\/+$/, "")}/api/v1/public/players`);
  const players = await response.json();
  const bySlug = new Map(players.map((p) => [slugify(p.player.name), p.player]));
  const unknown = Object.keys(sorted).filter((slug) => !bySlug.has(slug));
  const missing = [...bySlug.entries()].filter(([slug]) => !sorted[slug]).map(([, p]) => `${p.name} (${p.nickname})`);
  if (unknown.length) console.warn(`\nSlike bez igrača (proveri ime fajla): ${unknown.join(", ")}`);
  console.log(`Igrači bez slike: ${missing.length}${missing.length ? ` — ${missing.join(", ")}` : ""}`);
}
