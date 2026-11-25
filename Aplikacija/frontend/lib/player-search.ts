/**
 * Pretraga igrača („Pronađi igrača"): bez dijakritika i otporna na jednu slovnu grešku, jer
 * ljudi kucaju „milos risic", a u bazi piše „Miloš Ristić". Radi u pregledaču, nad ~100 igrača.
 */

export type Searchable = { nickname: string; name: string };

/** „Đuđević" i „Djudjevic" → „dudevic"; „Miloš" → „milos". */
export function fold(text: string): string {
  return text
    .toLowerCase()
    .replace(/đ/g, "d")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/dj/g, "d");
}

function tokens(text: string): string[] {
  return fold(text).split(/[^a-z0-9]+/).filter(Boolean);
}

/** Da li se dve reči razlikuju za najviše jedno slovo (zamena, višak ili manjak). */
function withinOneEdit(a: string, b: string): boolean {
  if (Math.abs(a.length - b.length) > 1) return false;
  let i = 0;
  let j = 0;
  let edits = 0;
  while (i < a.length && j < b.length) {
    if (a[i] === b[j]) {
      i++;
      j++;
      continue;
    }
    if (++edits > 1) return false;
    if (a.length > b.length) i++;
    else if (a.length < b.length) j++;
    else {
      i++;
      j++;
    }
  }
  return edits + (a.length - i) + (b.length - j) <= 1;
}

/**
 * 0 = ne poklapa se. Svaka reč upita mora da pogodi neku reč nadimka, imena ili prezimena:
 * cela reč (3) › početak reči (2) › jedno slovo razlike, za reči od 4+ slova (1).
 */
export function matchScore(query: string, player: Searchable): number {
  const wanted = tokens(query);
  if (wanted.length === 0) return 0;
  const words = [...tokens(player.nickname), ...tokens(player.name)];

  let score = 0;
  for (const q of wanted) {
    let best = 0;
    for (const w of words) {
      if (w === q) best = 3;
      else if (w.startsWith(q)) best = Math.max(best, 2);
      else if (q.length >= 4 && [q.length - 1, q.length, q.length + 1].some((n) => withinOneEdit(q, w.slice(0, n))))
        best = Math.max(best, 1);
    }
    if (best === 0) return 0;
    score += best;
  }
  return score;
}

/** Stavke koje se poklapaju sa upitom, najbolje prvo; kod istog skora po nadimku. */
export function searchPlayers<T>(
  query: string,
  items: T[],
  toSearchable: (item: T) => Searchable,
  limit = Infinity,
): T[] {
  return items
    .map((item) => ({ item, player: toSearchable(item), score: matchScore(query, toSearchable(item)) }))
    .filter((hit) => hit.score > 0)
    .sort((a, b) => b.score - a.score || a.player.nickname.localeCompare(b.player.nickname, "sr"))
    .slice(0, limit)
    .map((hit) => hit.item);
}
