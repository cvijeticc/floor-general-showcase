import { SiteFooter } from "@/components/site/site-footer";
import { SiteHeader } from "@/components/site/site-header";
import { SiteMotion } from "@/components/site/motion";

/** Javni sajt — rekreiran sa Framer originala floorgeneral.rs (odluka D-19). */
export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <SiteMotion>
      <div className="site-root min-h-dvh overflow-x-clip bg-white font-site text-black">
        <SiteHeader />
        <main>{children}</main>
        <SiteFooter />
      </div>
    </SiteMotion>
  );
}
