import type { Metadata } from "next";
import { Geist, Geist_Mono, Inter, Manrope } from "next/font/google";
import { Toaster } from "@/components/ui/sonner";
import { SITE_URL } from "@/lib/site-url";
import { Providers } from "./providers";
import "./globals.css";

// latin-ext nosi č, ć, š, đ, ž -- bez njega srpska slova padaju na drugi font.
const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin", "latin-ext"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin", "latin-ext"],
});

// Fontovi javnog sajta -- isti kao na Framer originalu (floorgeneral.rs): Manrope za sve,
// Inter samo za imena u sekciji „O nama" i pravne linkove u podnožju.
const manrope = Manrope({
  variable: "--font-manrope",
  subsets: ["latin", "latin-ext"],
});

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin", "latin-ext"],
});

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "Floor General 1 na 1 Liga",
    template: "%s | Floor General 1 na 1 Liga",
  },
  description:
    "Prva 1 na 1 košarkaška liga u Srbiji. Nagradni fond 650.000 dinara, pobednik osvaja 500.000. Rezultati, tabele i raspored mečeva.",
  openGraph: {
    type: "website",
    locale: "sr_RS",
    siteName: "Floor General 1 na 1 Liga",
    images: ["/site/og.png"],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    // Promenljive fonta moraju biti na <html>: tamo se primenjuje font-sans (globals.css), a
    // promenljiva sa <body> se na <html> ne vidi -- pa je ceo sajt padao na Times.
    <html
      lang="sr"
      className={`${geistSans.variable} ${geistMono.variable} ${manrope.variable} ${inter.variable}`}
    >
      <body className="antialiased">
        <Providers>{children}</Providers>
        <Toaster position="top-center" richColors />
      </body>
    </html>
  );
}
