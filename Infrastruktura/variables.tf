variable "region" {
  type    = string
  default = "eu-central-1"
}

variable "aws_profile" {
  type    = string
  default = "floor-general"
}

variable "name" {
  type    = string
  default = "floor-general"
}

# 2 vCPU / 8GB, Graviton (ARM). AMI ISPOD MORA biti arm64 -- t4g ne boot-uje
# x86 imidz. Spring Boot (JDK 21), PostgreSQL 16 i Next.js rade na ARM-u bez
# izmene koda; jedino Docker imidzi moraju biti multi-arch ili linux/arm64.
#
# 8GB umesto 4GB: JVM + Postgres + Next.js SSR na 4GB traze -Xmx tuning i swap,
# a razlika je ~$28/mes koje pokrivaju Activate krediti (credit sharing je
# ukljucen za ovaj nalog). Ako CPU burst postane usko grlo u danu finala,
# m7g.large je ne-burstable zamena -- stop/start, bez migracije podataka.
variable "instance_type" {
  type    = string
  default = "t4g.large"
}

# PRODUKCIJA -> on-demand. Spot bi znacio da liga moze da ostane bez sajta
# usred kola kad AWS zatrazi kapacitet nazad. Ostavljeno kao prekidac samo da
# se moduo moze reciklirati za staging.
variable "use_spot" {
  type    = bool
  default = false
}

variable "root_volume_size" {
  type    = number
  default = 30
}

# Postgres podaci + docker volumeni. Odvojen disk da prezive i zamenu instance
# (promena tipa, nov AMI), ne samo stop/start.
variable "data_volume_size" {
  type    = number
  default = 50
}

# Prazno = port 22 ZATVOREN. Pristup ide preko SSM Session Managera -- developer
# ulazi svojim Identity Center identitetom, bez deljenja ikakvog kljuca.
variable "ssh_ingress_cidr" {
  type    = string
  default = ""
}

variable "key_pair_name" {
  type    = string
  default = ""
}

# Koliko dnevnih EBS snapshot-ova /data diska se cuva (DLM).
variable "snapshot_retention_days" {
  type    = number
  default = 7
}

# GitHub repo koji sme da deploy-uje (owner/repo). Trust policy je vezana za
# tacno ovaj repo -- token iz bilo kog drugog repoa ne moze da preuzme rolu.
variable "github_repo" {
  type    = string
  default = "<owner>/<repo>"
}

# Prefiks `sub` claim-a u OIDC tokenu (immutable format, sa ID-jevima vlasnika
# i repoa). Vidi komentar u github-oidc.tf.
variable "github_sub_prefix" {
  type    = string
  default = "repo:<owner>@<owner-id>/<repo>@<repo-id>"
}

# Grana sa koje se deploy-uje. Namerno JEDNA grana: token sa feature grane ne
# sme da dira produkciju.
variable "github_deploy_branch" {
  type    = string
  default = "master"
}
