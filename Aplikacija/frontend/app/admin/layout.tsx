import type { Metadata } from "next";
import { AdminNav } from "./admin-nav";

export const metadata: Metadata = {
  title: {
    default: "Administracija",
    template: "%s | Administracija",
  },
};

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-dvh bg-muted/30">
      <AdminNav />
      <main className="mx-auto max-w-6xl px-4 py-6">{children}</main>
    </div>
  );
}
