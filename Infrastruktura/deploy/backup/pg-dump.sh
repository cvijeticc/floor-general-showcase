#!/bin/bash
# Logicki backup baze -> S3. Drugi sloj je EBS snapshot (DLM, dnevno 02:20 UTC):
# snapshot hvata fizicki kvar, ovaj dump hvata ljudsku gresku (obrisana tabela,
# losa migracija) jer se moze vratiti selektivno.
set -euo pipefail

# Bucket nosi ID naloga u imenu (backup.tf); instanca ga cita iz svog identiteta.
BUCKET="floor-general-backups-$(aws sts get-caller-identity --query Account --output text)"
ENV_FILE="/data/secrets/floor-general.env"
STAMP="$(date -u +%Y-%m-%dT%H%M%SZ)"
KEY="postgres/floor_general-${STAMP}.sql.gz"

# shellcheck disable=SC1090
set -a; source "$ENV_FILE"; set +a

# pg_dump iz kontejnera, da se verzija klijenta uvek poklapa sa serverom.
# -Fc (custom format) umesto plain SQL: omogucava pg_restore -t jedne tabele.
docker exec -i floor-general-postgres-1 \
    pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc \
    | gzip -9 \
    | aws s3 cp - "s3://${BUCKET}/${KEY}" \
        --storage-class STANDARD_IA \
        --expected-size 1073741824

# Verifikacija: da objekat postoji i da je ZAISTA upotrebljiv. Provera same
# velicine ne valja -- prazna baza daje legitimnih ~400 B, pa bi prag lagao u
# oba smera. pg_restore -l cita zaglavlje arhive: prolazi samo ako je dump
# citljiv do kraja.
SIZE=$(aws s3api head-object --bucket "$BUCKET" --key "$KEY" --query ContentLength --output text)
if [ "$SIZE" -lt 200 ]; then
    echo "GRESKA: ${KEY} ima samo ${SIZE} B -- dump nije ni poceo" >&2
    exit 1
fi

if ! aws s3 cp "s3://${BUCKET}/${KEY}" - --quiet | gunzip | pg_restore -l >/dev/null 2>&1; then
    echo "GRESKA: ${KEY} (${SIZE} B) nije citljiv pg_restore-om -- backup je neupotrebljiv" >&2
    exit 1
fi

echo "backup ok i proveren: s3://${BUCKET}/${KEY} (${SIZE} B)"
