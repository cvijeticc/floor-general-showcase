output "account_id" {
  value = data.aws_caller_identity.current.account_id
}

output "instance_id" {
  value = aws_instance.app.id
}

output "public_ip" {
  description = "Elastic IP -- ovde pokazuje DNS zapis lige."
  value       = aws_eip.app.public_ip
}

output "ssm_connect" {
  description = "Shell na masini, bez kljuceva i bez otvorenog porta 22."
  value       = "aws --profile ${var.aws_profile} ssm start-session --target ${aws_instance.app.id}"
}

output "backups_bucket" {
  value = aws_s3_bucket.backups.id
}

output "pricing_mode" {
  value = var.use_spot ? "spot (persistent + stop na prekid)" : "on-demand"
}

output "github_deploy_role_arn" {
  description = "Ide u GitHub Actions workflow kao role-to-assume (nije tajna)."
  value       = aws_iam_role.github_deploy.arn
}
