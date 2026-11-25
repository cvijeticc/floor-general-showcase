import { AdminOverview } from "./overview";

/**
 * Pocetni ekran administracije. Za sada pokazuje da veza sa backendom radi i koliko podataka
 * ima u sistemu; pravi ekrani (UC-FE-03…08) dolaze redom.
 */
export default function AdminHomePage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Administracija</h1>
        <p className="text-sm text-muted-foreground">
          Sezona se vodi odavde: sezone i faze, grupe, igrači, mečevi i unos rezultata.
        </p>
      </div>

      <AdminOverview />
    </div>
  );
}
