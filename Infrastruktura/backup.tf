# --- pg_dump u S3 ---------------------------------------------------------
# Namerno BEZ s3:DeleteObject u iam-app.tf: ako neko provali na instancu, ne
# sme da obrise backupe. Staro se cisti lifecycle pravilom ispod, ne rukom.
resource "aws_s3_bucket" "backups" {
  bucket = "${var.name}-backups-${data.aws_caller_identity.current.account_id}"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_versioning" "backups" {
  bucket = aws_s3_bucket.backups.id
  versioning_configuration { status = "Enabled" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "backups" {
  bucket = aws_s3_bucket.backups.id
  rule {
    apply_server_side_encryption_by_default { sse_algorithm = "AES256" }
  }
}

resource "aws_s3_bucket_public_access_block" "backups" {
  bucket                  = aws_s3_bucket.backups.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "backups" {
  bucket = aws_s3_bucket.backups.id

  rule {
    id     = "dumps-90d"
    status = "Enabled"
    filter { prefix = "postgres/" }

    expiration { days = 90 }
    noncurrent_version_expiration { noncurrent_days = 30 }
    abort_incomplete_multipart_upload { days_after_initiation = 7 }
  }
}

# --- dnevni EBS snapshot /data diska --------------------------------------
# Drugi sloj odbrane: pg_dump hvata logicku gresku (obrisana tabela), snapshot
# hvata fizicku (crknut disk, pokvaren fajlsistem).
resource "aws_iam_role" "dlm" {
  name = "${var.name}-dlm"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "dlm.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "dlm" {
  role       = aws_iam_role.dlm.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSDataLifecycleManagerServiceRole"
}

resource "aws_dlm_lifecycle_policy" "data" {
  description        = "Dnevni snapshot ${var.name} data diska"
  execution_role_arn = aws_iam_role.dlm.arn
  state              = "ENABLED"

  policy_details {
    resource_types = ["VOLUME"]

    # Hvata volumen po tagu Backup=daily (postavljen u compute.tf), ne po ID-u
    # -- zamena diska ne trazi izmenu ove politike.
    target_tags = { Backup = "daily" }

    schedule {
      name = "dnevni-${var.snapshot_retention_days}d"

      create_rule {
        interval      = 24
        interval_unit = "HOURS"
        # 02:20 UTC = 04:20 po lokalnom letnjem -- nocu, van saobracaja lige.
        times = ["02:20"]
      }

      retain_rule {
        count = var.snapshot_retention_days
      }

      copy_tags = true
    }
  }
}
