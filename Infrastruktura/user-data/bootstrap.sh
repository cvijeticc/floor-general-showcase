#!/bin/bash
# Bootstrap za prvi boot. NIJE sredstvo upravljanja masinom -- izmene ovde ne
# stizu na vec pokrenutu instancu (user_data_replace_on_change = false).
#
# ARM64 (Graviton): pazi na arch= u docker repou i na aarch64 AWS CLI paket.
set -uxo pipefail

export DEBIAN_FRONTEND=noninteractive

# --- /data volumen -------------------------------------------------------
# Attachment se u Terraformu desava POSLE kreiranja instance, pa uredjaj na
# prvom bootu jos ne mora da postoji. Cekamo ga, ali ne obaramo boot ako
# izostane -- masina mora da se digne i bez /data.
DATA_DEV=""
for _ in $(seq 1 60); do
    for candidate in /dev/nvme1n1 /dev/xvdf /dev/sdf; do
        if [ -b "$candidate" ]; then
            DATA_DEV="$candidate"
            break 2
        fi
    done
    sleep 3
done

if [ -n "$DATA_DEV" ]; then
    # blkid prazan = sirov disk bez fajl sistema. Formatiramo SAMO tada, da
    # ponovni boot nikad ne obrise bazu lige.
    if ! blkid "$DATA_DEV" >/dev/null 2>&1; then
        mkfs.ext4 -L data "$DATA_DEV"
    fi

    mkdir -p /data
    DATA_UUID=$(blkid -s UUID -o value "$DATA_DEV")
    # nofail: pogresan fstab red inace ostavlja masinu u emergency modu, a
    # tada SSM jos ne radi pa nemas kako da udjes i popravis.
    if ! grep -q "$DATA_UUID" /etc/fstab; then
        echo "UUID=$DATA_UUID /data ext4 defaults,nofail 0 2" >> /etc/fstab
    fi
    mount -a
    chown ubuntu:ubuntu /data
fi

# --- swap ----------------------------------------------------------------
# 2GB na 8GB masini nije nuzda nego osigurac: sprecava da OOM killer ubije
# Postgres ili JVM u spicu umesto da sistem na par sekundi uspori.
if [ ! -f /swapfile ]; then
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    # Baza ne voli agresivan swap -- koristi ga tek kad zaista zagusti.
    sysctl -w vm.swappiness=10
    echo 'vm.swappiness=10' > /etc/sysctl.d/99-swappiness.conf
fi

# --- alati ---------------------------------------------------------------
apt-get update -y
apt-get install -y \
    ca-certificates curl gnupg git jq unzip htop tmux ripgrep \
    postgresql-client-16 nginx certbot python3-certbot-nginx

# Docker iz zvanicnog repoa (Ubuntu-ov docker.io zaostaje za compose plugin-om)
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=arm64 signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu noble stable" \
    > /etc/apt/sources.list.d/docker.list
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
usermod -aG docker ubuntu

# Docker imidzi na /data: root disk je 30GB i imidzi ga brzo napune, a ovako
# prezive i zamenu instance.
if [ -n "$DATA_DEV" ] && mountpoint -q /data; then
    systemctl stop docker
    mkdir -p /data/docker
    echo '{"data-root": "/data/docker"}' > /etc/docker/daemon.json
    systemctl start docker
fi

# AWS CLI v2 (aarch64!)
curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o /tmp/awscliv2.zip
unzip -q /tmp/awscliv2.zip -d /tmp
/tmp/aws/install
rm -rf /tmp/aws /tmp/awscliv2.zip

# --- ssm-env -------------------------------------------------------------
# Isti helper kao na ostalim nasim masinama: renderuje ceo .env iz SSM-a. Secreti ne
# zive u repou; instanca ih samo cita (iam-app.tf), pisanje ide sa laptopa.
cat > /usr/local/bin/ssm-env <<'HELPER'
#!/bin/bash
# ssm-env <app> [dest]  ->  cita /prod/<app>/env u dest (podrazumevano
# /data/secrets/<app>.env, 0600, vlasnik ubuntu)
set -euo pipefail
APP="${1:?ime aplikacije}"
DEST="${2:-/data/secrets/${APP}.env}"
mkdir -p "$(dirname "$DEST")"
aws ssm get-parameter --name "/prod/${APP}/env" --with-decryption \
    --query Parameter.Value --output text > "$DEST"
chmod 600 "$DEST"
chown ubuntu:ubuntu "$DEST"
echo "napisano: $DEST"
HELPER
chmod 755 /usr/local/bin/ssm-env

mkdir -p /data/secrets /data/srv /data/backups
chown -R ubuntu:ubuntu /data/secrets /data/srv /data/backups 2>/dev/null || true

echo "bootstrap gotov: $(date -Is)" > /var/log/floor-general-bootstrap-done
