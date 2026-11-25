# Sta instanca SME sa AWS API-jem, pored SSM Session Managera.
#
# Isti obrazac kao na ostalim nasim masinama: `.env` ne zivi u repou nego u SSM
# Parameter Store kao SecureString pod `/prod/<app>/env`. Instanca ih SAMO
# CITA; pisanje ide sa laptopa (`aws --profile floor-general ssm put-parameter`).
# SecureString koristi AWS-managed kljuc `aws/ssm`, pa kms:Decrypt nije potreban.
data "aws_iam_policy_document" "app" {
  statement {
    sid = "ReadProdSecrets"
    actions = [
      "ssm:GetParameter",
      "ssm:GetParameters",
      "ssm:GetParametersByPath",
    ]
    resources = ["arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/prod/*"]
  }

  statement {
    sid       = "ListSecretNames"
    actions   = ["ssm:DescribeParameters"]
    resources = ["*"]
  }

  # pg_dump off-site, u bucket u OVOM nalogu (ne u deljeni backup bucket -- to je
  # drugi nalog, cross-account bi trazio bucket policy tamo).
  statement {
    sid       = "BackupBucketList"
    actions   = ["s3:ListBucket", "s3:GetBucketLocation"]
    resources = [aws_s3_bucket.backups.arn]
  }

  statement {
    sid       = "BackupBucketObjects"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:AbortMultipartUpload", "s3:ListMultipartUploadParts"]
    resources = ["${aws_s3_bucket.backups.arn}/*"]
  }
}

resource "aws_iam_role_policy" "app" {
  name   = "${var.name}-app"
  role   = aws_iam_role.app.name
  policy = data.aws_iam_policy_document.app.json
}
