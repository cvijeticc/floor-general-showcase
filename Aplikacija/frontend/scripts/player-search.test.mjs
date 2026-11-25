// Pretraga igrača (lib/player-search.ts) — `npm test`. Node 24 sam skida TypeScript tipove,
// pa test ne traži nijedan dodatni paket. Imena su izmišljena.
import assert from "node:assert/strict";
import { test } from "node:test";
import { fold, matchScore, searchPlayers } from "../lib/player-search.ts";

const PLAYERS = [
  { nickname: "Sky Lale", name: "Miloš Ristić" },
  { nickname: "ŽIKA", name: "Živorad Ristić" },
  { nickname: "Laki", name: "Lazar Ristić" },
  { nickname: "Đoka", name: "Đorđe Đuđević" },
  { nickname: "Pera", name: "Petar Petrović" },
];

const find = (query) => searchPlayers(query, PLAYERS, (p) => p).map((p) => p.nickname);

test("bez dijakritika i bez obzira na velika slova", () => {
  assert.deepEqual(find("milos ristic"), ["Sky Lale"]);
  assert.deepEqual(find("MILOŠ"), ["Sky Lale"]);
  assert.deepEqual(find("zika"), ["ŽIKA"]);
});

test("jedna slovna greška u reči od 4+ slova", () => {
  // Bez kvačica i sa slovom manje: „Milos Risic", a igrač je Miloš Ristić.
  assert.deepEqual(find("Milos Risic"), ["Sky Lale"]);
  assert.deepEqual(find("rizt"), ["Laki", "Sky Lale", "ŽIKA"]);
});

test("isto prezime — nudi sve, a ime ili nadimak bira", () => {
  assert.deepEqual(find("ristic"), ["Laki", "Sky Lale", "ŽIKA"]);
  assert.deepEqual(find("laki ristic"), ["Laki"]);
});

test("đ se kuca kao d ili dj", () => {
  assert.equal(fold("Đuđević"), "dudevic");
  assert.deepEqual(find("djudjevic"), ["Đoka"]);
  assert.deepEqual(find("dorde"), ["Đoka"]);
  assert.deepEqual(find("Đorđe"), ["Đoka"]);
});

test("početak reči je dovoljan, cela reč je bolja", () => {
  assert.ok(matchScore("lale", PLAYERS[0]) > matchScore("lal", PLAYERS[0]));
  assert.deepEqual(find("sky"), ["Sky Lale"]);
});

test("bez poklapanja i za prazan upit", () => {
  assert.deepEqual(find("xyz"), []);
  assert.deepEqual(find("   "), []);
  // Kratke reči ne idu kroz „jedno slovo razlike" — „pra" ne sme da nađe Peru.
  assert.deepEqual(find("pra"), []);
});

test("admin kuca bez kvačica: „koco“ nalazi „Kočo“, „c“ i „s“ nalaze č, ć i š", () => {
  const admins = [
    { nickname: "Kočo", name: "Jovan Lazarević" },
    { nickname: "Cane", name: "Andrija Cvetković" },
    { nickname: "Šone", name: "Nikola Šarić" },
    { nickname: "Ćira", name: "Marko Čolić" },
  ];
  const pick = (query) => searchPlayers(query, admins, (p) => p).map((p) => p.nickname);
  assert.deepEqual(pick("koco"), ["Kočo"]);
  assert.deepEqual(pick("koc"), ["Kočo"]);
  assert.deepEqual(pick("cvetkovic"), ["Cane"]);
  assert.deepEqual(pick("saric"), ["Šone"]);
  assert.deepEqual(pick("s"), ["Šone"]);
  assert.deepEqual(pick("colic"), ["Ćira"]);
  assert.deepEqual(pick("c"), ["Cane", "Ćira"]);
});
