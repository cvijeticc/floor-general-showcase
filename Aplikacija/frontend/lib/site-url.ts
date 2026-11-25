/**
 * Javna adresa sajta — za metadataBase, sitemap, robots.txt i apsolutne URL-ove u schema.org
 * oznakama. Na produkciji se zadaje kroz SITE_URL (Infrastruktura/deploy/docker-compose.yml);
 * podrazumevano je pravi domen lige, na koji se sajt seli.
 */
export const SITE_URL = (process.env.SITE_URL ?? "https://www.floorgeneral.rs").replace(/\/+$/, "");
