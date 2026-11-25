"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

/**
 * Stari linkovi „/kontakt#prijava" (Instagram, poruke, Framer) vode na novu stranu /prijavi-se.
 * Sidro ne stiže do servera, pa preusmerenje mora u pregledaču.
 */
export function LegacySignupRedirect() {
  const router = useRouter();
  useEffect(() => {
    const check = () => {
      if (window.location.hash === "#prijava") router.replace("/prijavi-se");
    };
    check();
    window.addEventListener("hashchange", check);
    return () => window.removeEventListener("hashchange", check);
  }, [router]);
  return null;
}
