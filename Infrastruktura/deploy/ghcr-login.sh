#!/bin/bash
# docker login na GHCR za privatne imidze. Token se cita iz SSM-a i ide u
# docker kroz --password-stdin: nikad u komandnoj liniji (video bi ga `ps`)
# ni u shell istoriji.
#
# Pokrece se JEDNOM (login se cuva u ~/.docker/config.json i prezivljava
# reboot), pa opet samo kad se token rotira.
set -euo pipefail

PARAM="/prod/floor-general/ghcr"
GH_USER="${1:-cvijeticc}"

aws ssm get-parameter --name "$PARAM" --with-decryption \
    --query Parameter.Value --output text \
  | sudo -u ubuntu docker login ghcr.io -u "$GH_USER" --password-stdin

echo "GHCR login ok za korisnika $GH_USER"
