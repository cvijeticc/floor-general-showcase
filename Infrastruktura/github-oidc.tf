# GitHub Actions -> AWS bez ijednog secreta u repou.
#
# Umesto access kljuca (koji niko ne rotira i koji curi kroz logove), GitHub
# svakom job-u izdaje kratkotrajan OIDC token; AWS ga menja za privremene
# kredencijale samo ako se poklope repo I grana iz uslova ispod.
resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1",
    "1c58a3a8518e8759bf075b76b750d4f2df264fcd",
  ]
}

data "aws_iam_policy_document" "github_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # KLJUCNO: bez ovog uslova rolu bi mogao da preuzme BILO KOJI GitHub repo
    # na svetu. `sub` vezuje token za tacno ovaj repo i tacno ovu granu.
    #
    # Repo je napravljen posle 15.07.2026, pa GitHub OBAVEZNO salje immutable
    # format `repo:<owner>@<ownerID>/<repo>@<repoID>:ref:...` -- stari
    # `repo:<owner>/<repo>:ref:...` se nikad ne poklopi i AssumeRole pada.
    # Tacan prefiks: `gh api repos/<repo>/actions/oidc/customization/sub`.
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["${var.github_sub_prefix}:ref:refs/heads/${var.github_deploy_branch}"]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name                 = "${var.name}-github-deploy"
  description          = "Deploy sa GitHub Actions preko OIDC-a, bez dugozivecih kljuceva"
  assume_role_policy   = data.aws_iam_policy_document.github_assume.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "github_deploy" {
  # Deploy = jedna komanda na jednoj instanci. Namerno NEMA ec2:*, iam:* ni
  # pristupa bazi -- kompromitovan workflow ne moze da digne novu masinu ni da
  # procita secrete.
  statement {
    sid     = "SendDeployCommand"
    actions = ["ssm:SendCommand"]
    resources = [
      aws_instance.app.arn,
      "arn:aws:ssm:${var.region}::document/AWS-RunShellScript",
    ]
  }

  # Bez ovoga workflow posalje komandu ali ne vidi da li je pala.
  statement {
    sid = "ReadCommandResult"
    actions = [
      "ssm:GetCommandInvocation",
      "ssm:ListCommandInvocations",
      "ssm:ListCommands",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "${var.name}-github-deploy"
  role   = aws_iam_role.github_deploy.name
  policy = data.aws_iam_policy_document.github_deploy.json
}
