"use client";

import { DownloadIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { API_PROXY_PREFIX } from "@/lib/api/config";

/**
 * „Preuzmi Excel" — obična veza, pa pregledač sam preuzima fajl (ime dolazi iz
 * Content-Disposition: floor-general-<datum>.xlsx). Token ide iz kolačića kroz proxy.
 */
export function ExportButton() {
  return (
    <Button asChild variant="outline">
      <a href={`${API_PROXY_PREFIX}/api/v1/export/xlsx`} download>
        <DownloadIcon />
        Preuzmi Excel
      </a>
    </Button>
  );
}
