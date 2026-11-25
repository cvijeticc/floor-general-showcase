import type { Metadata } from "next";
import { GroupsGrid } from "./groups-grid";

export const metadata: Metadata = {
  title: "Grupe",
};

/**
 * Grupe jedne sezone kao velika polja „Grupa 1"…„Grupa N". Broj polja dolazi iz baze,
 * nije hardkodiran (CLAUDE.md §4). `?sezona=` bira sezonu; bez njega se uzima najnovija.
 */
export default async function GroupsPage({
  searchParams,
}: {
  searchParams: Promise<{ sezona?: string }>;
}) {
  const { sezona } = await searchParams;
  const seasonId = sezona != null && sezona !== "" ? Number(sezona) : undefined;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Grupe</h1>
        <p className="text-sm text-muted-foreground">
          Klikni na grupu da vidiš njene igrače.
        </p>
      </div>

      <GroupsGrid initialSeasonId={Number.isFinite(seasonId) ? seasonId : undefined} />
    </div>
  );
}
