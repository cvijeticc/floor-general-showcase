// Raspored drveta playoff-a (lib/bracket.ts) — `npm test`.
import assert from "node:assert/strict";
import { test } from "node:test";
import {
  bracketHeight,
  cardTop,
  currentRoundIndex,
  feederLabel,
  playerStops,
  roundFormat,
  seriesCode,
  shortWhen,
} from "../lib/bracket.ts";

const series = (number, position, status = "READY", extra = {}) => ({
  id: number,
  number,
  position,
  thirdPlace: false,
  player1: null,
  player2: null,
  seed1: null,
  seed2: null,
  wins1: 0,
  wins2: 0,
  winnerId: null,
  walkover: false,
  status,
  scheduledStart: null,
  courtName: null,
  games: [],
  ...extra,
});

const round = (roundNo, list, winsRequired = 2) => ({
  roundNo,
  name: `Kolo ${roundNo}`,
  winsRequired,
  targetScore: 11,
  maxScore: 12,
  durationMinutes: 8,
  series: list,
});

/** Drvo od 8: 4 + 2 + finale i meč za 3. mesto. */
function bracketOfEight(firstStatus = "READY") {
  return {
    stageId: 1,
    stageName: "Playoff",
    bracketSize: 8,
    rounds: [
      round(1, [1, 2, 3, 4].map((n) => series(n, n, firstStatus))),
      round(2, [series(5, 1, "PENDING"), series(6, 2, "PENDING")]),
      round(3, [series(7, 1, "PENDING"), { ...series(8, 1, "PENDING"), thirdPlace: true }], 1),
    ],
  };
}

test("svaka serija sledećeg kola je na sredini između svoje dve", () => {
  assert.equal(cardTop(0, 3), 3);
  assert.equal(cardTop(1, 0), 0.5);
  assert.equal(cardTop(1, 1), 2.5);
  assert.equal(cardTop(2, 0), 1.5);
});

test("visina drveta prati kolo od kog se gleda", () => {
  const bracket = bracketOfEight();
  const fromFirst = bracketHeight(bracket.rounds, 0);
  const fromFinal = bracketHeight(bracket.rounds, 2);
  // Prvo kolo ima 4 serije; meč za 3. mesto ispod finala traži malo više mesta od toga.
  assert.ok(fromFirst >= 4);
  // Od finala se drvo skupi: finale i meč za 3. mesto ispod njega.
  assert.ok(fromFinal > 2 && fromFinal < fromFirst);
});

test("drvo se otvara na kolu koje se igra", () => {
  assert.equal(currentRoundIndex(bracketOfEight()), 0);
  const done = bracketOfEight("FINISHED");
  done.rounds[1].series = done.rounds[1].series.map((s) => ({ ...s, status: "READY" }));
  assert.equal(currentRoundIndex(done), 1);
});

test("prazno mesto kaže odakle igrač stiže, kao na kosturu lige", () => {
  const bracket = bracketOfEight();
  assert.equal(feederLabel(bracket.rounds, 1, bracket.rounds[1].series[0], 2), "Pobednik M02");
  assert.equal(feederLabel(bracket.rounds, 2, bracket.rounds[2].series[1], 1), "Poraženi M05");
});

test("oznake i format", () => {
  assert.equal(seriesCode({ number: 7 }), "M07");
  assert.equal(roundFormat({ winsRequired: 2, targetScore: 11, durationMinutes: 8 }), "Na 2 pobede · do 11 ili 8 min");
  assert.equal(roundFormat({ winsRequired: 1, targetScore: 16, durationMinutes: 15 }), "Jedna utakmica · do 16 ili 15 min");
  assert.equal(shortWhen("2026-11-14T18:00:00", "Teren A"), "Sub 14.11. · 18:00 · Teren A");
  assert.equal(shortWhen("2026-11-15T00:00:00", null), "Ned 15.11.");
});

test("pretraga vodi na poslednju seriju igrača, ne na prvo kolo", () => {
  const p = (id) => ({ id, name: `Igrač ${id}`, nickname: `N${id}` });
  const bracket = bracketOfEight("FINISHED");
  const r = bracket.rounds;
  // Prvo kolo: 1 pobeđuje 2, 3 pobeđuje 4…
  r[0].series = r[0].series.map((s, i) => ({ ...s, player1: p(2 * i + 1), player2: p(2 * i + 2), winnerId: 2 * i + 1 }));
  // Polufinale: 1 protiv 3 (1 prošao), 5 protiv 7 (još se igra).
  r[1].series = [
    { ...r[1].series[0], player1: p(1), player2: p(3), winnerId: 1, status: "FINISHED" },
    { ...r[1].series[1], player1: p(5), player2: p(7), status: "IN_PROGRESS" },
  ];
  r[2].series = [{ ...r[2].series[0], player1: p(1) }, { ...r[2].series[1], player1: p(3) }];

  const stops = new Map(playerStops(bracket).map((s) => [s.player.id, s]));
  assert.equal(stops.size, 8);
  // Ispao u prvom kolu: ostaje na prvom kolu.
  assert.equal(stops.get(2).roundIndex, 0);
  assert.equal(stops.get(2).state, "out");
  // Polufinale još traje: tu ga vodi.
  assert.equal(stops.get(5).roundIndex, 1);
  assert.equal(stops.get(5).state, "playing");
  // Finalista: finale, a ne prvo kolo ni polufinale.
  assert.equal(stops.get(1).roundIndex, 2);
  assert.equal(stops.get(1).series.thirdPlace, false);
  // Poraženi polufinalista: meč za 3. mesto.
  assert.equal(stops.get(3).roundIndex, 2);
  assert.equal(stops.get(3).series.thirdPlace, true);
});
